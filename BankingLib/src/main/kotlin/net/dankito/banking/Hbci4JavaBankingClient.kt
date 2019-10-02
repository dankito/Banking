package net.dankito.banking

import net.dankito.banking.model.*
import net.dankito.banking.tan.TanData
import net.dankito.banking.util.AccountingEntryMapper
import org.kapott.hbci.GV.HBCIJob
import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.GV_Result.GVRSaldoReq
import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.HBCIUtilsInternal
import org.kapott.hbci.manager.HBCIVersion
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.status.HBCIExecStatus
import org.kapott.hbci.structures.Konto
import org.kapott.hbci.structures.Value
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


open class Hbci4JavaBankingClient @JvmOverloads constructor(
        val credentials: AccountCredentials,
        val dataDirectory: File = File("data"),
        val enterTanCallback: ((TanData) -> String?)? = null
) : IBankingClient {

    companion object {
        // the date format is hard coded in HBCIUtils.string2DateISO()
        val HbciLibDateFormat = SimpleDateFormat("yyyy-MM-dd")

        const val NinetyDaysInMilliseconds = 90 * 24 * 60 * 60 * 1000L

        private val log = LoggerFactory.getLogger(Hbci4JavaBankingClient::class.java)
    }


    protected val accountingEntryMapper = AccountingEntryMapper()


    override fun findBankByIban(iban: String): BankInfo? {
        if (iban.length > 4) {
            val ibanWithoutCountryAndChecksum = iban.substring(4)

            HBCIUtilsInternal.banks.values.filter { bank ->
                ibanWithoutCountryAndChecksum.startsWith(bank.blz)
            }.firstOrNull()?.let {
                return BankInfo(it, listOf())
            }
        }

        return null
    }

    protected open fun connect(): ConnectionValues {
        return connect(credentials, HBCIVersion.HBCI_300)
    }

    protected open fun connect(credentials: AccountCredentials, version: HBCIVersion): ConnectionValues {
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        val props = Properties()
        HBCIUtils.init(props, HbciCallback(credentials, enterTanCallback))

        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos geloescht werden. Sie wird beim naechsten mal automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat (siehe unten).
        // Wir speichern die Datei der Einfachheit halber im aktuellen Verzeichnis.
        dataDirectory.mkdirs()
        val passportFile = File(dataDirectory,"passport.dat")

        // Wir setzen die Kernel-Parameter zur Laufzeit. Wir koennten sie alternativ
        // auch oben in "props" setzen.
        HBCIUtils.setParam("client.passport.default", "PinTan") // Legt als Verfahren PIN/TAN fest.
        HBCIUtils.setParam("client.passport.PinTan.filename", passportFile.absolutePath)
        HBCIUtils.setParam("client.passport.PinTan.init", "1")

        var handle: HBCIHandler? = null
        var passport: HBCIPassport? = null

        try {
            // Erzeugen des Passport-Objektes.
            passport = AbstractHBCIPassport.getInstance()

            // Konfigurieren des Passport-Objektes.
            // Das kann alternativ auch alles ueber den Callback unten geschehen

            // Das Land.
            passport.country = "DE"

            // Server-Adresse angeben. Koennen wir entweder manuell eintragen oder direkt von HBCI4Java ermitteln lassen
            val info = HBCIUtils.getBankInfo(credentials.bankleitzahl)
            passport.host = info.pinTanAddress

            // TCP-Port des Servers. Bei PIN/TAN immer 443, da das ja ueber HTTPS laeuft.
            passport.port = 443

            // Art der Nachrichten-Codierung. Bei Chipkarte/Schluesseldatei wird
            // "None" verwendet. Bei PIN/TAN kommt "Base64" zum Einsatz.
            passport.filterType = "Base64"

            // Verbindung zum Server aufbauen
            handle = HBCIHandler(version.getId(), passport)


        }
        catch(e: Exception) {
            log.error("Could not connect to bank ${credentials.bankleitzahl}", e)
            closeConnection(handle, passport)

            return ConnectionValues(false, error = e)
        }

        return ConnectionValues(true, handle, passport)
    }

    protected open fun closeConnection(connection: ConnectionValues) {
        closeConnection(connection.handle, connection.passport)
    }

    protected open fun closeConnection(handle: HBCIHandler?, passport: HBCIPassport?) {
        // Sicherstellen, dass sowohl Passport als auch Handle nach Beendigung geschlossen werden.
        try {
            handle?.close()

            passport?.close()

            HBCIUtils.doneThread() // i hate static variables, here's one of the reasons why: Old callbacks and therefore credentials get stored in static variables and therefor always the first entered credentials have been used
        } catch(e: Exception) { log.error("Could not close connection", e) }
    }


    override fun getAccountsAsync(callback: (GetAccountsResult) -> Unit) {
        thread {
            callback(getAccounts())
        }
    }

    protected open fun getAccounts(): GetAccountsResult {
        val connection = connect()
        closeConnection(connection)

        if(connection.successful) {
            connection.passport?.let { passport ->
                val accounts = passport.accounts
                if (accounts == null || accounts.size == 0) {
                    log.error("Keine Konten ermittelbar")
                    return GetAccountsResult(false, error = Exception("Keine Konten ermittelbar"))
                }

                log.info("Anzahl Konten: " + accounts.size)
                val bankInfo = HBCIUtils.getBankInfo(credentials.bankleitzahl)

                return GetAccountsResult(true, BankInfo(bankInfo, mapAccounts(accounts, credentials)))
            }
        }

        return GetAccountsResult(false, error = connection.error)
    }

    private fun mapAccounts(accounts: Array<out Konto>, credentials: AccountCredentials): List<Account> {
        val mappedAccounts = ArrayList<Account>()

        accounts.forEach { account ->
            mappedAccounts.add(Account(account, credentials))
        }

        return mappedAccounts
    }


    override fun getAccountingEntriesAsync(account: Account, startDate: Date?, callback: (AccountingEntries) -> Unit) {
        thread {
            callback(getAccountingEntries(account, startDate))
        }
    }

    /**
     * According to PSD2 for the accounting entries of the last 90 days the two-factor authorization does not have to
     * be applied. It depends on the bank if they request a second factor or not.
     *
     * So we simply try to retrieve at accounting entries of the last 90 days and see if a second factor is required
     * or not.
     */
    override fun getAccountingEntriesOfLast90DaysAsync(account: Account, callback: (AccountingEntries) -> Unit) {
        thread {
            val ninetyDaysAgo = Date(Date().time - NinetyDaysInMilliseconds)
            callback(getAccountingEntries(account, ninetyDaysAgo))
        }
    }

    protected open fun getAccountingEntries(account: Account, startDate: Date?): AccountingEntries {
        val connection = connect()

        connection.handle?.let { handle ->
            try {
                val (saldoJob, umsatzJob, status) = executeJobsForGetAccountingEntries(handle, account, startDate)

                // Pruefen, ob die Kommunikation mit der Bank grundsaetzlich geklappt hat
                if(!status.isOK) {
                    log.error("Could not connect to bank ${credentials.bankleitzahl} ${status.toString()}: ${status.errorString}")
                    return AccountingEntries(false, error = Exception("Could not connect to bank ${credentials.bankleitzahl}: ${status.toString()}"))
                }

                // Auswertung des Saldo-Abrufs.
                val saldoResult = saldoJob.jobResult as GVRSaldoReq
                if(!saldoResult.isOK) {
                    log.error("Could not get saldo of bank ${credentials.bankleitzahl}: ${saldoResult.toString()}", saldoResult.getJobStatus().exceptions)
                    return AccountingEntries(false, error = Exception("Could not get saldo of bank ${credentials.bankleitzahl}: ${saldoResult.toString()}"))
                }

                val saldo = saldoResult.entries[0].ready.value
                log.info("Saldo: " + saldo.toString())


                // Das Ergebnis des Jobs koennen wir auf "GVRKUms" casten. Jobs des Typs "KUmsAll"
                // liefern immer diesen Typ.
                val result = umsatzJob.jobResult as GVRKUms

                // Pruefen, ob der Abruf der Umsaetze geklappt hat
                if(!result.isOK) {
                    log.error("Could not get accounting details of bank ${credentials.bankleitzahl}: ${result.toString()}", result.getJobStatus().exceptions)
                    return AccountingEntries(false, error = Exception("Could not get accounting details of bank ${credentials.bankleitzahl}: ${result.toString()}"))
                }


                return AccountingEntries(true, saldo, accountingEntryMapper.mapAccountingEntries(result))
            }
            catch(e: Exception) {
                log.error("Could not get accounting details for bank ${credentials.bankleitzahl}", e)
                return AccountingEntries(false, error = e)
            }
            finally {
                closeConnection(connection)
            }
        }

        closeConnection(connection)
        return AccountingEntries(false, error = connection.error)
    }

    protected open fun executeJobsForGetAccountingEntries(handle: HBCIHandler, account: Account, startDate: Date?): Triple<HBCIJob, HBCIJob, HBCIExecStatus> {
        // 1. Auftrag fuer das Abrufen des Saldos erzeugen
        val saldoJob = handle.newJob("SaldoReq")
        saldoJob.setParam("my", account.info) // festlegen, welches Konto abgefragt werden soll.
        saldoJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen
        val umsatzJob = handle.newJob("KUmsAll")
        umsatzJob.setParam("my", account.info) // festlegen, welches Konto abgefragt werden soll.
        // evtl. Datum setzen, ab welchem die Auszüge geholt werden sollen
        startDate?.let {
            umsatzJob.setParam("startdate", HbciLibDateFormat.format(it))
        }
        umsatzJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // Hier koennen jetzt noch weitere Auftraege fuer diesen Bankzugang hinzugefuegt
        // werden. Z.Bsp. Ueberweisungen.

        // Alle Auftraege aus der Liste ausfuehren.
        val status = handle.execute()

        return Triple(saldoJob, umsatzJob, status)
    }


    override fun transferCashAsync(cashTransfer: CashTransfer, callback: (CashTransferResult) -> Unit) {
        thread {
            callback(transferCash(cashTransfer))
        }
    }

    override fun transferCash(transfer: CashTransfer): CashTransferResult {
        val connection = connect()

        connection.handle?.let { handle ->
            try {
                createTransferCashJob(handle, transfer)

                val status = handle.execute()

                return CashTransferResult(status.isOK, status.toString())
            } catch(e: Exception) {
                log.error("Could not transfer cash $transfer" , e)
                return CashTransferResult(false, e.localizedMessage, e)
            }
            finally {
                closeConnection(connection)
            }
        }

        return CashTransferResult(false, "Could not connect", connection.error)
    }

    private fun createTransferCashJob(handle: HBCIHandler, transfer: CashTransfer) {
        val transferCashJob = handle.newJob("UebSEPA")

        val source = mapToKonto(transfer.source)
        val destination = mapToKonto(transfer.destination)
        val amount = Value(transfer.amount, "EUR")

        transferCashJob.setParam("src", source)
        transferCashJob.setParam("dst", destination)
        transferCashJob.setParam("btg", amount)
        transferCashJob.setParam("usage", transfer.usage)

        transferCashJob.addToQueue()
    }

    private fun mapToKonto(party: SepaParty): Konto {
        val konto = Konto(party.country, party.blz, party.kontoNummer)

        konto.name = party.name
        konto.iban = party.iban
        konto.bic = party.bic

        return konto
    }

}