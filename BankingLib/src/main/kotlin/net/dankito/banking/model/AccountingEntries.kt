package net.dankito.banking.model

import org.kapott.hbci.structures.Value


open class AccountingEntries(successful: Boolean, var saldo: Value? = null, var entries: List<AccountingEntry> = listOf(), error: Exception? = null)
    : ResultBase(successful, error) {


    internal constructor() : this(false) // for Jackson


    override fun toString(): String {
        return "$saldo, ${entries.size} entries"
    }

}