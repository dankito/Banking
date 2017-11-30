package net.dankito.banking

import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.ConnectionValues
import net.dankito.banking.model.GetAccountsResult
import org.kapott.hbci.GV.HBCIJob
import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.GV_Result.GVRSaldoReq
import org.kapott.hbci.callback.AbstractHBCICallback
import org.kapott.hbci.callback.HBCICallback
import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.HBCIVersion
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.status.HBCIExecStatus
import org.kapott.hbci.structures.Konto
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import kotlin.concurrent.thread


open class Hbci4JavaBankingClient(val bankleitzahl: String, val customerId: String, private val pin: String) : IBankingClient {

    companion object {
        private val log = LoggerFactory.getLogger(Hbci4JavaBankingClient::class.java)
    }



    protected open fun connect(): ConnectionValues {
        return connect(bankleitzahl, customerId, pin, HBCIVersion.HBCI_300)
    }

    protected open fun connect(bankleitzahl: String, customerId: String, pin: String, version: HBCIVersion): ConnectionValues {
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        val props = Properties()
        HBCIUtils.init(props, MyHBCICallback(bankleitzahl, customerId, pin))

        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos geloescht werden. Sie wird beim naechsten mal automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat (siehe unten).
        // Wir speichern die Datei der Einfachheit halber im aktuellen Verzeichnis.
        val passportFile = File("testpassport.dat")

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
            val info = HBCIUtils.getBankInfo(bankleitzahl)
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
            log.error("Could not connect to bank $bankleitzahl", e)
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
        handle?.close()

        passport?.close()
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
                val bankInfo = HBCIUtils.getBankInfo(bankleitzahl)

                return GetAccountsResult(true, accounts.toList(), bankInfo) // TODO: map to Banking specific Account object
            }
        }

        return GetAccountsResult(false, error = connection.error)
    }


    override fun getAccountingEntriesAsync(account: Konto, callback: (AccountingEntries) -> Unit) {
        thread {
            callback(getAccountingEntries(account))
        }
    }

    protected open fun getAccountingEntries(account: Konto): AccountingEntries {
        val connection = connect()

        connection.handle?.let { handle ->
            try {
                val (saldoJob, umsatzJob, status) = executeJobsForGetAccountingEntries(handle, account)
                closeConnection(connection)

                // Pruefen, ob die Kommunikation mit der Bank grundsaetzlich geklappt hat
                if(!status.isOK) {
                    log.error("Could not connect to bank $bankleitzahl ${status.toString()}: ${status.errorString}")
                    return AccountingEntries(false, error = Exception("Could not connect to bank $bankleitzahl: ${status.toString()}"))
                }

                // Auswertung des Saldo-Abrufs.
                val saldoResult = saldoJob.jobResult as GVRSaldoReq
                if(!saldoResult.isOK) {
                    log.error("Could not get saldo of bank $bankleitzahl: ${saldoResult.toString()}", saldoResult.getJobStatus().exceptions)
                    return AccountingEntries(false, error = Exception("Could not get saldo of bank $bankleitzahl: ${saldoResult.toString()}"))
                }

                val saldo = saldoResult.entries[0].ready.value
                log.info("Saldo: " + saldo.toString())


                // Das Ergebnis des Jobs koennen wir auf "GVRKUms" casten. Jobs des Typs "KUmsAll"
                // liefern immer diesen Typ.
                val result = umsatzJob.jobResult as GVRKUms

                // Pruefen, ob der Abruf der Umsaetze geklappt hat
                if(!result.isOK) {
                    log.error("Could not get accounting details of bank $bankleitzahl: ${result.toString()}", result.getJobStatus().exceptions)
                    return AccountingEntries(false, error = Exception("Could not get accounting details of bank $bankleitzahl: ${result.toString()}"))
                }


                return AccountingEntries(true, saldo, result.flatData)
            }
            catch(e: Exception) {
                log.error("Could not get accounting details for bank $bankleitzahl", e)
                return AccountingEntries(false, error = e)
            }
        }

        closeConnection(connection)
        return AccountingEntries(false, error = connection.error)
    }

    protected open fun executeJobsForGetAccountingEntries(handle: HBCIHandler, account: Konto): Triple<HBCIJob, HBCIJob, HBCIExecStatus> {
        // 1. Auftrag fuer das Abrufen des Saldos erzeugen
        val saldoJob = handle.newJob("SaldoReq")
        saldoJob.setParam("my", account) // festlegen, welches Konto abgefragt werden soll.
        saldoJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen
        val umsatzJob = handle.newJob("KUmsAll")
        umsatzJob.setParam("my", account) // festlegen, welches Konto abgefragt werden soll.
        umsatzJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // Hier koennen jetzt noch weitere Auftraege fuer diesen Bankzugang hinzugefuegt
        // werden. Z.Bsp. Ueberweisungen.

        // Alle Auftraege aus der Liste ausfuehren.
        val status = handle.execute()

        return Triple(saldoJob, umsatzJob, status)
    }

    private fun mapAccountingEntries(result: GVRKUms): List<GVRKUms.UmsLine> {
        result.flatData.forEach { buchung ->
            val sb = StringBuilder()
            sb.append(buchung.valuta)

            val v = buchung.value
            if (v != null) {
                sb.append(": ")
                sb.append(v)
            }

            val zweck = buchung.usage
            if (zweck != null && zweck.size > 0) {
                sb.append(" - ")
                // Die erste Zeile des Verwendungszwecks ausgeben
                sb.append(zweck[0])
            }

            // Ausgeben der Umsatz-Zeile
            log.info(sb.toString())
        }

        return result.flatData // TODO: map to AccountingEntry
    }


    /**
     * Ueber diesen Callback kommuniziert HBCI4Java mit dem Benutzer und fragt die benoetigten
     * Informationen wie Benutzerkennung, PIN usw. ab.
     */
    private class MyHBCICallback(private val bankleitzahl: String, private val customerId: String, private val pin: String) : AbstractHBCICallback() {
        /**
         * @see org.kapott.hbci.callback.HBCICallback.log
         */
        override fun log(msg: String, level: Int, date: Date, trace: StackTraceElement) {
            // Ausgabe von Log-Meldungen bei Bedarf
            log.info("Callback log: $msg")
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback.callback
         */
        override fun callback(passport: HBCIPassport, reason: Int, msg: String, datatype: Int, retData: StringBuffer) {
            // Diese Funktion ist wichtig. Ueber die fragt HBCI4Java die benoetigten Daten von uns ab.
            when (reason) {
            // Mit dem Passwort verschluesselt HBCI4Java die Passport-Datei.
            // Wir nehmen hier der Einfachheit halber direkt die PIN. In der Praxis
            // sollte hier aber ein staerkeres Passwort genutzt werden.
            // Die Ergebnis-Daten muessen in dem StringBuffer "retData" platziert werden.
                HBCICallback.NEED_PASSPHRASE_LOAD, HBCICallback.NEED_PASSPHRASE_SAVE -> retData.replace(0, retData.length, pin)

            // PIN wird benoetigt
                HBCICallback.NEED_PT_PIN -> retData.replace(0, retData.length, pin)

            // ADDED: Auswaehlen welches PinTan Verfahren verwendet werden soll
                HBCICallback.NEED_PT_SECMECH -> retData.replace(0, retData.length, "911") // TODO: i set it to a fixed value here, ask user

            // BLZ wird benoetigt
                HBCICallback.NEED_BLZ -> retData.replace(0, retData.length, bankleitzahl)

            // Die Benutzerkennung
                HBCICallback.NEED_USERID -> retData.replace(0, retData.length, customerId)

            // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
            // Bei manchen Banken kann man die auch leer lassen
                HBCICallback.NEED_CUSTOMERID -> retData.replace(0, retData.length, customerId)

            // Manche Fehlermeldungen werden hier ausgegeben
                HBCICallback.HAVE_ERROR -> log.error(msg)

                else -> {
                }
            }// Wir brauchen nicht alle der Callbacks
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback.status
         */
        override fun status(passport: HBCIPassport, statusTag: Int, o: Array<Any>?) {
            // So aehnlich wie log(String,int,Date,StackTraceElement) jedoch fuer Status-Meldungen.
            log.info("New status for passport $passport: $statusTag $o")
        }

    }

}