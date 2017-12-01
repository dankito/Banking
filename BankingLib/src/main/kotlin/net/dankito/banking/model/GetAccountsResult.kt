package net.dankito.banking.model


class GetAccountsResult(successful: Boolean, val bankInfo: BankInfo? = null, error: Exception? = null)
    : ResultBase(successful, error)