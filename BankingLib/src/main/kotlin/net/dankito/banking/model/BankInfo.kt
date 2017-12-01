package net.dankito.banking.model

import org.kapott.hbci.manager.BankInfo


open class BankInfo(val info: BankInfo, val accounts: List<Account>) { // TODO: map org.kapott.hbci.manager.BankInfo completely

}