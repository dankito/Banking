package net.dankito.banking.model

import kotlin.Exception


class CashTransferResult(val successful: Boolean,
                         val message: String,
                         val exception: Exception? = null) {

    override fun toString(): String {
        return "Successful? $successful. $message"
    }

}