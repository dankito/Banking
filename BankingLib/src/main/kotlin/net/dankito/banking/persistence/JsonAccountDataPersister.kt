package net.dankito.banking.persistence

import net.dankito.banking.model.AccountingEntries
import java.io.File


class JsonAccountDataPersister : IAccountDataPersister, JsonPersisterBase() {


    override fun persistAccountData(destinationFile: File, accountingEntries: AccountingEntries) {
        saveObjectToFile(destinationFile, accountingEntries)
    }

    override fun getPersistedAccountData(file: File): AccountingEntries {
        return deserializePersistedObject(file, AccountingEntries::class.java)
    }

}