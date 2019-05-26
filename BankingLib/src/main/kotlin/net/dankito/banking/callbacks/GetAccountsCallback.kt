package net.dankito.banking.callbacks

import net.dankito.banking.model.GetAccountsResult


interface GetAccountsCallback {

    fun done(result: GetAccountsResult)

}