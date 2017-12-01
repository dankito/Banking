package net.dankito.banking.persistence

import net.dankito.banking.model.Account
import net.dankito.banking.model.AccountingEntry
import java.io.File


interface IExporter {

    fun exportAccountEntriesAsync(destinationFile: File, account: Account, entries: List<AccountingEntry>, done: () -> Unit)

    fun exportAccountEntries(destinationFile: File, account: Account, entries: List<AccountingEntry>)

}