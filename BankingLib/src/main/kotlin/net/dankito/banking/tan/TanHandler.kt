package net.dankito.banking.tan


open class TanHandler(private val enterTanCallback: ((TanData) -> String?)? = null) {

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

            return enterTanCallback?.invoke(ChipTanData(flickerData, TanProcedure.ChipTan, usefulMessage))
        }
        else {
            return enterTanCallback?.invoke(TanData(TanProcedure.EnterTan, messageToShowToUser))
        }
    }

}