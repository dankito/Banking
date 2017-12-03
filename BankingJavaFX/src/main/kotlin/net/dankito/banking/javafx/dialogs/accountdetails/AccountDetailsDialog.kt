package net.dankito.banking.javafx.dialogs.accountdetails

import net.dankito.banking.javafx.dialogs.DialogFragment
import net.dankito.banking.javafx.dialogs.accountdetails.controls.AccountDetailsField
import net.dankito.banking.model.Account
import tornadofx.*


class AccountDetailsDialog : DialogFragment() {

    val account: Account by param()


    override val root = vbox {

        add(AccountDetailsField(messages["account.details.name.label"], account.info.name + (account.info.name2 ?: "")))

        add(AccountDetailsField(messages["account.details.type.label"], account.info.type))

        add(AccountDetailsField(messages["account.details.bank.code.label"], account.info.blz))

        add(AccountDetailsField(messages["account.details.account.number.label"], account.info.number))

        add(AccountDetailsField(messages["account.details.iban.label"], account.info.iban))

        add(AccountDetailsField(messages["account.details.bic.label"], account.info.bic))

    }

}