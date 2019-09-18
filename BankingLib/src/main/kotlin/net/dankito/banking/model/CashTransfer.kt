package net.dankito.banking.model

import java.math.BigDecimal


class CashTransfer(val source: SepaParty,
                   val destination: SepaParty,
                   val amount: BigDecimal,
                   val usage: String) {

    override fun toString(): String {
        return "$amount from $source to $destination with usage $usage"
    }

}