package net.dankito.banking.persistence

import net.dankito.banking.model.AccountingEntries
import java.io.File


interface IAccountDataPersister {

    fun persistAccountData(destinationFile: File, accountingEntries: AccountingEntries)

    fun getPersistedAccountData(file: File): AccountingEntries

}