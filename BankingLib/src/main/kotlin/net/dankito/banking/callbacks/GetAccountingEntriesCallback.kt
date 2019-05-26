package net.dankito.banking.callbacks

import net.dankito.banking.model.AccountingEntries


interface GetAccountingEntriesCallback {

    fun done(entries: AccountingEntries)

}