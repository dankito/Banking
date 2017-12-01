package net.dankito.banking.persistence

import net.dankito.banking.model.AccountingEntry
import java.io.File


interface IImporter {

    fun importEntriesAsync(file: File, done: (List<AccountingEntry>) -> Unit)

    fun importEntries(file: File): List<AccountingEntry>

}