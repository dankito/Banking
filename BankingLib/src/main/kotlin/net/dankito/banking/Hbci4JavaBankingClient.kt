package net.dankito.banking

import net.dankito.banking.model.*
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


open class Hbci4JavaBankingClient(val credentials: AccountCredentials) : IBankingClient {

    companion object {
        private val DateStartString = "DATUM "
        private val DateEndString = " UHR"

        private val DateFormat = SimpleDateFormat("dd.MM.yyyy,HH.mm")

        private val log = LoggerFactory.getLogger(Hbci4JavaBankingClient::class.java)
    }



    protected open fun connect(): ConnectionValues {
        return connect(credentials, HBCIVersion.HBCI_300)
    }

    protected open fun connect(credentials: AccountCredentials, version: HBCIVersion): ConnectionValues {
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        val props = Properties()
        HBCIUtils.init(props, MyHBCICallback(credentials))

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


    override fun getAccountingEntriesAsync(account: Account, callback: (AccountingEntries) -> Unit) {
        thread {
            callback(getAccountingEntries(account))
        }
    }

    protected open fun getAccountingEntries(account: Account): AccountingEntries {
        val connection = connect()

        connection.handle?.let { handle ->
            try {
                val (saldoJob, umsatzJob, status) = executeJobsForGetAccountingEntries(handle, account)
                closeConnection(connection)

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


                return AccountingEntries(true, saldo, mapAccountingEntries(result))
            }
            catch(e: Exception) {
                log.error("Could not get accounting details for bank ${credentials.bankleitzahl}", e)
                return AccountingEntries(false, error = e)
            }
        }

        closeConnection(connection)
        return AccountingEntries(false, error = connection.error)
    }

    protected open fun executeJobsForGetAccountingEntries(handle: HBCIHandler, account: Account): Triple<HBCIJob, HBCIJob, HBCIExecStatus> {
        // 1. Auftrag fuer das Abrufen des Saldos erzeugen
        val saldoJob = handle.newJob("SaldoReq")
        saldoJob.setParam("my", account.info) // festlegen, welches Konto abgefragt werden soll.
        saldoJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen
        val umsatzJob = handle.newJob("KUmsAll")
        umsatzJob.setParam("my", account.info) // festlegen, welches Konto abgefragt werden soll.
        umsatzJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // Hier koennen jetzt noch weitere Auftraege fuer diesen Bankzugang hinzugefuegt
        // werden. Z.Bsp. Ueberweisungen.

        // Alle Auftraege aus der Liste ausfuehren.
        val status = handle.execute()

        return Triple(saldoJob, umsatzJob, status)
    }

    private fun mapAccountingEntries(result: GVRKUms): List<AccountingEntry> {
        val entries = ArrayList<AccountingEntry>()

        result.flatData.forEach { buchung ->
            entries.add(mapAccountingEntry(buchung))
        }

        return entries.sortedByDescending { it.bookingDate }
    }

    private fun mapAccountingEntry(buchung: GVRKUms.UmsLine): AccountingEntry {
        val entry = AccountingEntry(buchung.value, buchung.bdate, buchung.text, buchung.other, buchung.usage.joinToString(""))

        mapUsage(buchung, entry)

        return entry
    }

    /**
     * From https://sites.google.com/a/crem-solutions.de/doku/version-2012-neu/buchhaltung/03-zahlungsverkehr/05-e-banking/technische-beschreibung-der-mt940-sta-datei:
     *
     * Weitere 4 Verwendungszwecke können zu den Feldschlüsseln 60 bis 63 eingestellt werden.
     * Jeder Bezeichner [z.B. EREF+] muss am Anfang eines Subfeldes [z. B. ?21] stehen.
     * Bei Längenüberschreitung wird im nachfolgenden Subfeld ohne Wiederholung des Bezeichners fortgesetzt. Bei Wechsel des Bezeichners ist ein neues Subfeld zu beginnen.
     * Belegung in der nachfolgenden Reihenfolge, wenn vorhanden:
     * EREF+[ Ende-zu-Ende Referenz ] (DD-AT10; CT-AT41 - Angabe verpflichtend; NOTPROVIDED wird nicht eingestellt.)
     * KREF+[Kundenreferenz]
     * MREF+[Mandatsreferenz] (DD-AT01 - Angabe verpflichtend)
     * CRED+[Creditor Identifier] (DD-AT02 - Angabe verpflichtend bei SEPA-Lastschriften, nicht jedoch bei SEPA-Rücklastschriften)
     * DEBT+[Originators Identification Code](CT-AT10- Angabe verpflichtend,)
     * Entweder CRED oder DEBT
     *
     * optional zusätzlich zur Einstellung in Feld 61, Subfeld 9:
     *
     * COAM+ [Compensation Amount / Summe aus Auslagenersatz und Bearbeitungsprovision bei einer nationalen Rücklastschrift sowie optionalem Zinsausgleich.]
     * OAMT+[Original Amount] Betrag der ursprünglichen Lastschrift
     *
     * SVWZ+[SEPA-Verwendungszweck] (DD-AT22; CT-AT05 -Angabe verpflichtend, nicht jedoch bei R-Transaktionen)
     * ABWA+[Abweichender Überweisender] (CT-AT08) / Abweichender Zahlungsempfänger (DD-AT38) ] (optional)
     * ABWE+[Abweichender Zahlungsemp-fänger (CT-AT28) / Abweichender Zahlungspflichtiger ((DD-AT15)] (optional)
     */
    private fun mapUsage(buchung: GVRKUms.UmsLine, entry: AccountingEntry) {
        var lastUsageLineType = UsageLineType.ContinuationFromLastLine
        var typeValue = ""

        buchung.usage.forEach { line ->
            val (type, adjustedString) = getUsageLineType(line, entry)

            if(type == UsageLineType.ContinuationFromLastLine) {
                typeValue += (if(adjustedString[0].isUpperCase()) " " else "") + adjustedString
            }
            else if(lastUsageLineType != type) {
                if(lastUsageLineType != UsageLineType.ContinuationFromLastLine) {
                    setUsageLineValue(entry, lastUsageLineType, typeValue)
                }

                typeValue = adjustedString
                lastUsageLineType = type
            }

            tryToParseBookingDateFromUsageLine(entry, adjustedString, typeValue)
        }

        if(lastUsageLineType != UsageLineType.ContinuationFromLastLine) {
            setUsageLineValue(entry, lastUsageLineType, typeValue)
        }
    }

    private fun setUsageLineValue(entry: AccountingEntry, lastUsageLineType: UsageLineType, typeValue: String) {
        when(lastUsageLineType) {
            UsageLineType.EREF -> entry.endToEndReference = typeValue
            UsageLineType.DEBT -> entry.originatorsIdentificationCode = typeValue
            UsageLineType.SVWZ -> entry.sepaVerwendungszweck = typeValue
            UsageLineType.ABWA -> entry.abweichenderAuftraggeber = typeValue
            UsageLineType.NoSpecialType -> entry.usageWithNoSpecialType = typeValue
        }
    }

    private fun getUsageLineType(line: String, entry: AccountingEntry): Pair<UsageLineType, String> {
        if(line.startsWith("EREF+")) {
            return Pair(UsageLineType.EREF, line.substring(5))
        }
        else if(line.startsWith("KREF+")) {
            return Pair(UsageLineType.KREF, line.substring(5))
        }
        else if(line.startsWith("MREF+")) {
            return Pair(UsageLineType.MREF, line.substring(5))
        }
        else if(line.startsWith("CRED+")) {
            return Pair(UsageLineType.CRED, line.substring(5))
        }
        else if(line.startsWith("DEBT+")) {
            return Pair(UsageLineType.DEBT, line.substring(5))
        }
        else if(line.startsWith("COAM+")) {
            return Pair(UsageLineType.COAM, line.substring(5))
        }
        else if(line.startsWith("OAMT+")) {
            return Pair(UsageLineType.OAMT, line.substring(5))
        }
        else if(line.startsWith("SVWZ+")) {
            return Pair(UsageLineType.SVWZ, line.substring(5))
        }
        else if(line.startsWith("ABWA+")) {
            return Pair(UsageLineType.ABWA, line.substring(5))
        }
        else if(line.startsWith("ABWE+")) {
            return Pair(UsageLineType.ABWE, line.substring(5))
        }
        else if(entry.usage.startsWith(line)) {
            return Pair(UsageLineType.NoSpecialType, line)
        }
        else {
            return Pair(UsageLineType.ContinuationFromLastLine, line)
        }
    }

    private fun tryToParseBookingDateFromUsageLine(entry: AccountingEntry, currentLine: String, typeLine: String) {
        if(currentLine.startsWith(DateStartString)) {
            tryToParseBookingDateFromUsageLine(entry, currentLine)
        }
        else if(typeLine.startsWith(DateStartString)) {
            tryToParseBookingDateFromUsageLine(entry, typeLine)
        }
    }

    private fun tryToParseBookingDateFromUsageLine(entry: AccountingEntry, line: String) {
        var subString = line.replace(DateStartString, "")
        val index = subString.indexOf(DateEndString)
        if(index > 0) {
            subString = subString.substring(0, index)
        }

        try {
            entry.bookingDate = DateFormat.parse(subString)
        } catch (e: Exception) {
            log.debug("Could not parse $subString from $line to a Date", e)
        }
    }


    /**
     * Ueber diesen Callback kommuniziert HBCI4Java mit dem Benutzer und fragt die benoetigten
     * Informationen wie Benutzerkennung, PIN usw. ab.
     */
    private class MyHBCICallback(private val credentials: AccountCredentials) : AbstractHBCICallback() {
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
                HBCICallback.NEED_PASSPHRASE_LOAD, HBCICallback.NEED_PASSPHRASE_SAVE -> retData.replace(0, retData.length, credentials.pin)

            // PIN wird benoetigt
                HBCICallback.NEED_PT_PIN -> retData.replace(0, retData.length, credentials.pin)

            // ADDED: Auswaehlen welches PinTan Verfahren verwendet werden soll
                HBCICallback.NEED_PT_SECMECH -> retData.replace(0, retData.length, "911") // TODO: i set it to a fixed value here, ask user

            // BLZ wird benoetigt
                HBCICallback.NEED_BLZ -> retData.replace(0, retData.length, credentials.bankleitzahl)

            // Die Benutzerkennung
                HBCICallback.NEED_USERID -> retData.replace(0, retData.length, credentials.customerId)

            // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
            // Bei manchen Banken kann man die auch leer lassen
                HBCICallback.NEED_CUSTOMERID -> retData.replace(0, retData.length, credentials.customerId)

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
            log.debug("New status for passport $passport: $statusTag $o")
        }

    }

}