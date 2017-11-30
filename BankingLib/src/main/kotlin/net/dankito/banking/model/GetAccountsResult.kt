package net.dankito.banking.model

import org.kapott.hbci.manager.BankInfo
import org.kapott.hbci.structures.Konto


class GetAccountsResult(successful: Boolean, val accounts: List<Konto> = listOf(), val bankInfo: BankInfo? = null, error: Exception? = null)
    : ResultBase(successful, error)