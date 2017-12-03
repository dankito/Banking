package net.dankito.banking.persistence

import net.dankito.banking.model.BankInfo
import java.io.File


class JsonAccountSettingsPersister : IAccountSettingsPersister, JsonPersisterBase() {


    override fun persistAccounts(destinationFile: File, bankInfos: List<BankInfo>) {
        saveObjectToFile(destinationFile, bankInfos)  // TODO: encrypt, passwords as well as file as whole
    }

    override fun getPersistedAccounts(file: File): List<BankInfo> {
        return deserializePersistedObject(file, objectMapper.typeFactory.constructParametricType(List::class.java, BankInfo::class.java))
    }

}