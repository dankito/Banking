package net.dankito.banking.model

import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.structures.Value


open class AccountingEntries(successful: Boolean, var saldo: Value? = null, var entries: List<GVRKUms.UmsLine> = listOf(), error: Exception? = null)
    : ResultBase(successful, error)