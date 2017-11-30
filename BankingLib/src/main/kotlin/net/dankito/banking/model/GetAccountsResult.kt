package net.dankito.banking.model

import org.kapott.hbci.structures.Konto


class GetAccountsResult(successful: Boolean, val accounts: List<Konto> = listOf(), error: Exception? = null) : ResultBase(successful, error)