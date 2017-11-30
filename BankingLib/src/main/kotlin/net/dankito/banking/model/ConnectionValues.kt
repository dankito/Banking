package net.dankito.banking.model

import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.passport.HBCIPassport


open class ConnectionValues(successful: Boolean, val handle: HBCIHandler? = null, val passport: HBCIPassport? = null, error: Exception? = null)
    : ResultBase(successful, error)