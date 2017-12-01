package net.dankito.banking.model

import org.kapott.hbci.manager.BankInfo


open class BankInfo(val info: BankInfo, val accounts: List<Account>) { // TODO: map org.kapott.hbci.manager.BankInfo completely

    companion object {

        /**
         * BankInfo has no public constructor or static creator method -> we have to make default constructor accessible via reflection
         */
        private fun createBankInfoViaReflection(): BankInfo {
            val constructor = BankInfo::class.java.getDeclaredConstructor()
            constructor.isAccessible = true

            return constructor.newInstance()
        }

    }

    internal constructor() : this(createBankInfoViaReflection(), listOf()) // for Jackson


    override fun toString(): String {
        return "${info.name}"
    }

}