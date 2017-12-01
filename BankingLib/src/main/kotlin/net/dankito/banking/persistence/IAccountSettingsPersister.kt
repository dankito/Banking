package net.dankito.banking.persistence

import net.dankito.banking.model.BankInfo
import java.io.File


interface IAccountSettingsPersister {

    fun persistAccounts(destinationFile: File, bankInfos: List<BankInfo>)

    fun getPersistedAccounts(file: File): List<BankInfo>

}