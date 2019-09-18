package net.dankito.banking.tan


class ChipTanData(val flickerData: String, procedure: TanProcedure, messageToShowToUser: String)
    : TanData(procedure, messageToShowToUser) {

    override fun toString(): String {
        return super.toString() + ", flicker data = $flickerData"
    }

}