package net.dankito.banking.model

open class ResultBase(val successful: Boolean, val error: Exception? = null)