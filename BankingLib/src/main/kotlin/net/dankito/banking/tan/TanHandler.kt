package net.dankito.banking.tan

import net.dankito.banking.callbacks.HbciClientCallback


open class TanHandler(private val callback: HbciClientCallback? = null) {

    open fun getTanFromUser(messageToShowToUser: String, flickerData: String): String? {
        // Wenn per "retData" Daten uebergeben wurden, dann enthalten diese
        // den fuer chipTAN optisch zu verwendenden Flickercode.
        // Falls nicht, ist es eine TAN-Abfrage, fuer die keine weiteren
        // Parameter benoetigt werden (z.B. smsTAN, iTAN oder aehnliches)

        // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
        // den bankspezifischen Text mit den Instruktionen fuer den User.
        // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
        // werden.

        if (flickerData.isNullOrEmpty() == false) {
            // for Sparkasse messageToShowToUser started with "chipTAN optisch\nTAN-Nummer\n\n"
            val usefulMessage = messageToShowToUser.split("\n").last().trim()

            return callback?.enterTan(ChipTanData(flickerData, TanProcedure.ChipTan, usefulMessage))
        }
        else {
            return callback?.enterTan(TanData(TanProcedure.EnterTan, messageToShowToUser))
        }
    }



    open fun selectTanProcedure(selectableTanProceduresString: String): SelectTanProcedure? {
        val selectableTanProcedures = parseSelectableTanProcedures(selectableTanProceduresString)

        if (selectableTanProcedures.isNotEmpty()) {
            return callback?.selectTanProcedure(selectableTanProcedures)
        }

        return null
    }

    protected open fun parseSelectableTanProcedures(selectTanProceduresString: String): List<SelectTanProcedure> {
        return selectTanProceduresString.split('|')
                .map { mapToSelectTanProcedure(it) }
                .filterNotNull()
    }

    protected open fun mapToSelectTanProcedure(selectTanProcedureString: String): SelectTanProcedure? {
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

}