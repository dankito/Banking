package net.dankito.banking.tan


open class TanData(val procedure: TanProcedure, val messageToShowToUser: String) {

    override fun toString(): String {
        return "$procedure: $messageToShowToUser"
    }

}