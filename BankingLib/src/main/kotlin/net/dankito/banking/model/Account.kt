package net.dankito.banking.model

import org.kapott.hbci.structures.Konto


open class Account(val info: Konto, val credentials: AccountCredentials) { // TODO: map org.kapott.hbci.structures.Konto completely

    internal constructor() : this(Konto(), AccountCredentials()) // for Jackson

}