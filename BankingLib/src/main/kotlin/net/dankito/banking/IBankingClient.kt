package net.dankito.banking

import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.GetAccountsResult
import org.kapott.hbci.structures.Konto


interface IBankingClient {

    fun getAccounts(): GetAccountsResult

    fun getAccountingEntries(account: Konto): AccountingEntries

}