package net.dankito.banking.model

import org.kapott.hbci.structures.Konto
import org.kapott.hbci.structures.Value
import java.util.*


class AccountingEntry(val value: Value, var bookingDate: Date, val valutaDate: Date, val type: String, val other: Konto, val usage: String) {

    var endToEndReference: String? = null

    var kundenreferenz: String? = null

    var mandatsreferenz: String? = null

    var creditorIdentifier: String? = null

    var originatorsIdentificationCode: String? = null

    var compensationAmount: String? = null

    var originalAmount: String? = null

    var sepaVerwendungszweck: String? = null

    var abweichenderAuftraggeber: String? = null

    var abweichenderZahlungsempfaenger: String? = null

    var usageWithNoSpecialType: String? = null

    val parsedUsages = ArrayList<String>()


    fun showOtherName(): Boolean {
        return other.name.isNullOrBlank() == false && type != "ENTGELTABSCHLUSS" && type != "AUSZAHLUNG"
    }

    fun getUsage1(): String {
        abweichenderAuftraggeber?.let { return it }

        sepaVerwendungszweck?.let { return it }

        originatorsIdentificationCode?.let { return it }

        endToEndReference?.let { return it }

        usageWithNoSpecialType?.let { return it }

        return "" // this should never happen
    }

    fun getUsage2(): String? {
        sepaVerwendungszweck?.let { if(it != getUsage1()) return it }

        originatorsIdentificationCode?.let { if(it != getUsage1()) return it }

        endToEndReference?.let { if(it != getUsage1()) return it }

        usageWithNoSpecialType?.let { if(it != getUsage1()) return it }

        return null
    }

}