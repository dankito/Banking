package net.dankito.banking.javafx.dialogs.accountingentriesdetails

import net.dankito.banking.javafx.dialogs.DialogFragment
import net.dankito.banking.javafx.dialogs.accountingentriesdetails.controls.AccountingEntriesDetailsField
import net.dankito.banking.model.AccountingEntry
import tornadofx.*


class AccountingEntriesDetailsDialog : DialogFragment() {

    val entry: AccountingEntry by param()


    override val root = vbox {

        add(AccountingEntriesDetailsField(messages["accounting.entries.details.value.label"], entry.value.toString()))

        add(AccountingEntriesDetailsField(messages["accounting.entries.details.type.label"], entry.type))

        add(AccountingEntriesDetailsField(messages["accounting.entries.details.other.name.label"], entry.other.name + (entry.other.name2 ?: "")))

        add(AccountingEntriesDetailsField(messages["accounting.entries.details.iban.or.bank.code.label"], entry.other.iban ?: entry.other.blz))

        add(AccountingEntriesDetailsField(messages["accounting.entries.details.bic.or.account.number.label"], entry.other.bic ?: entry.other.number))

        add(AccountingEntriesDetailsField(messages["accounting.entries.details.usage.label"], entry.sepaVerwendungszweck ?: entry.usageWithNoSpecialType ?: entry.getUsage1()))

        entry.creditorIdentifier?.let { creditorIdentifier ->
            add(AccountingEntriesDetailsField(messages["accounting.entries.details.creditor.identifier.label"], creditorIdentifier))
        }

        entry.mandatsreferenz?.let { mandatsreferenz ->
            add(AccountingEntriesDetailsField(messages["accounting.entries.details.mandatsreferenz.label"], mandatsreferenz))
        }

        entry.endToEndReference?.let { endToEndReference ->
            add(AccountingEntriesDetailsField(messages["accounting.entries.details.end.to.end.reference.label"], endToEndReference))
        }

        entry.abweichenderZahlungsempfaenger?.let { abweichenderZahlungsempfaenger ->
            add(AccountingEntriesDetailsField(messages["accounting.entries.details.abweichender.zahlungsempfaenger.label"], abweichenderZahlungsempfaenger))
        }

        entry.abweichenderAuftraggeber?.let { abweichenderAuftraggeber ->
            add(AccountingEntriesDetailsField(messages["accounting.entries.details.abweichender.auftragsgeber.label"], abweichenderAuftraggeber))
        }

    }

}