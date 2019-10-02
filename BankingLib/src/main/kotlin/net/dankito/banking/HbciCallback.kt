package net.dankito.banking

import net.dankito.banking.callbacks.HbciClientCallback
import net.dankito.banking.model.AccountCredentials
import net.dankito.banking.tan.SelectTanProcedure
import net.dankito.banking.tan.TanHandler
import net.dankito.banking.tan.TanProcedure
import org.kapott.hbci.callback.AbstractHBCICallback
import org.kapott.hbci.callback.HBCICallback
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.passport.HBCIPassport
import org.slf4j.LoggerFactory
import java.util.*


/**
 * Ueber diesen Callback kommuniziert HBCI4Java mit dem Benutzer und fragt die benoetigten
 * Informationen wie Benutzerkennung, PIN usw. ab.
 */
class HbciCallback(private val credentials: AccountCredentials,
                   private val callback: HbciClientCallback? = null
) : AbstractHBCICallback() {

    companion object {
        private val log = LoggerFactory.getLogger(HbciCallback::class.java)
    }


    private val tanHandler = TanHandler(callback)


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
            // if you like or need to change your pin, return your old one for NEED_PASSPHRASE_LOAD and your new
            // one for NEED_PASSPHRASE_SAVE
            HBCICallback.NEED_PASSPHRASE_LOAD, HBCICallback.NEED_PASSPHRASE_SAVE -> retData.replace(0, retData.length, credentials.pin)

            // PIN wird benoetigt
            HBCICallback.NEED_PT_PIN -> retData.replace(0, retData.length, credentials.pin)

            // ADDED: Auswaehlen welches PinTan Verfahren verwendet werden soll
            HBCICallback.NEED_PT_SECMECH -> selectTanProcedure(retData)

            // BLZ wird benoetigt
            HBCICallback.NEED_BLZ -> retData.replace(0, retData.length, credentials.bankleitzahl)

            // Die Benutzerkennung
            HBCICallback.NEED_USERID -> retData.replace(0, retData.length, credentials.customerId)

            // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
            // Bei manchen Banken kann man die auch leer lassen
            HBCICallback.NEED_CUSTOMERID -> retData.replace(0, retData.length, credentials.customerId)

            // chipTan
            HBCICallback.NEED_PT_TAN -> {
                tanHandler.getTanFromUser(msg, retData.toString())?.let { enteredTan ->
                    retData.replace(0, retData.length, enteredTan)
                }
            }

            // Manche Fehlermeldungen werden hier ausgegeben
            HBCICallback.HAVE_ERROR -> log.error(msg)

            else -> { // Wir brauchen nicht alle der Callbacks
            }
        }
    }

    private fun selectTanProcedure(retData: StringBuffer) {
        log.info("Available TAN procedures: $retData") // TODO: remove again

        val selectableTanProcedures = parseSelectableTanProcedures(retData.toString())

        if (selectableTanProcedures.isNotEmpty()) {
            callback?.selectTanProcedure(selectableTanProcedures)?.let { selectedTanProcedure ->
                retData.replace(0, retData.length, selectedTanProcedure.procedureCode)
            }
        }
    }

    private fun parseSelectableTanProcedures(selectTanProceduresString: String): List<SelectTanProcedure> {
        return selectTanProceduresString.split('|')
                .map { mapToSelectTanProcedure(it) }
                .filterNotNull()
    }

    private fun mapToSelectTanProcedure(selectTanProcedureString: String): SelectTanProcedure? {
        val parts = selectTanProcedureString.split(':')

        if (parts.size > 1) {
            val code = parts[0]
            val procedureName = parts[1]
            val nameLowerCase = procedureName.toLowerCase()

            return when {
                nameLowerCase.contains("chiptan") -> {
                    if (nameLowerCase.contains("qr")) {
                        SelectTanProcedure(TanProcedure.ChipTanQrCode, procedureName, code)
                    }
                    else {
                        SelectTanProcedure(TanProcedure.ChipTan, procedureName, code)
                    }
                }
                nameLowerCase.contains("sms") -> SelectTanProcedure(TanProcedure.SmsTan, procedureName, code)
                nameLowerCase.contains("push") -> SelectTanProcedure(TanProcedure.PushTan, procedureName, code)
                // we filter out iTAN and Einschritt-Verfahren as they are not permitted anymore according to PSD2
                else -> null
            }
        }

        return null
    }

    /**
     * @see org.kapott.hbci.callback.HBCICallback.status
     */
    override fun status(passport: HBCIPassport, statusTag: Int, o: Array<Any>?) {
        // So aehnlich wie log(String,int,Date,StackTraceElement) jedoch fuer Status-Meldungen.
        val param = if(o == null) "null" else o.joinToString()
        log.debug("New status for passport $passport: $statusTag $param")
    }

}