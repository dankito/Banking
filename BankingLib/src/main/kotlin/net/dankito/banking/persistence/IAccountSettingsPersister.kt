package net.dankito.banking.persistence

import net.dankito.banking.model.BankInfo


interface IAccountSettingsPersister {

    fun persistAccounts(bankInfos: List<BankInfo>)

    fun getPersistedAccounts(): List<BankInfo>

}