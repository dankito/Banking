package net.dankito.banking

import net.dankito.banking.callbacks.GetAccountingEntriesCallback
import net.dankito.banking.callbacks.GetAccountsCallback
import net.dankito.banking.model.Account
import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.GetAccountsResult
import java.util.*


interface IBankingClient {

    fun getAccountsAsync(callback: GetAccountsCallback) {
        getAccountsAsync {
            callback.done(it)
        }
    }

    fun getAccountsAsync(callback: (GetAccountsResult) -> Unit)


    fun getAccountingEntriesAsync(account: Account, startDate: Date?, callback: GetAccountingEntriesCallback) {
        getAccountingEntriesAsync(account, startDate) {
            callback.done(it)
        }
    }

    fun getAccountingEntriesAsync(account: Account, startDate: Date?, callback: (AccountingEntries) -> Unit)

    /**
     * According to PSD2 for the accounting entries of the last 90 days the two-factor authorization does not have to
     * be applied. It depends on the bank if they request a second factor or not.
     *
     * So we simply try to retrieve at accounting entries of the last 90 days and see if a second factor is required
     * or not.
     */
    fun getAccountingEntriesOfLast90DaysAsync(account: Account, callback: GetAccountingEntriesCallback) {
        getAccountingEntriesOfLast90DaysAsync(account) {
            callback.done(it)
        }
    }

    /**
     * According to PSD2 for the accounting entries of the last 90 days the two-factor authorization does not have to
     * be applied. It depends on the bank if they request a second factor or not.
     *
     * So we simply try to retrieve at accounting entries of the last 90 days and see if a second factor is required
     * or not.
     */
    fun getAccountingEntriesOfLast90DaysAsync(account: Account, callback: (AccountingEntries) -> Unit)

}