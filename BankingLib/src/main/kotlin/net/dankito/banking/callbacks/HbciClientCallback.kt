package net.dankito.banking.callbacks

import net.dankito.banking.tan.SelectTanProcedure
import net.dankito.banking.tan.TanData


interface HbciClientCallback {

    fun selectTanProcedure(selectableTanProcedures: List<SelectTanProcedure>): SelectTanProcedure?

    fun enterTan(tanData: TanData): String?

}