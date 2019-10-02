package net.dankito.banking.callbacks

import net.dankito.banking.tan.TanData


interface HbciClientCallback {

    fun enterTan(tanData: TanData): String?

}