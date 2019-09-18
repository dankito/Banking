package net.dankito.banking.model


class CashTransferResult(successful: Boolean,
                         val message: String,
                         error: Exception? = null
) : ResultBase(successful, error) {

    override fun toString(): String {
        return "Successful? $successful. $message"
    }

}