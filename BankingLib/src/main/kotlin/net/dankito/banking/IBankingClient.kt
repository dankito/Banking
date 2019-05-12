package net.dankito.banking

import net.dankito.banking.model.Account
import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.GetAccountsResult
import java.util.*


interface IBankingClient {

    fun getAccountsAsync(callback: (GetAccountsResult) -> Unit)

    fun getAccountingEntriesAsync(account: Account, startDate: Date?, callback: (AccountingEntries) -> Unit)

}