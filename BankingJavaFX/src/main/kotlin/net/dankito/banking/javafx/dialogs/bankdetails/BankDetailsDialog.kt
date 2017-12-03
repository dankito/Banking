package net.dankito.banking.javafx.dialogs.bankdetails

import net.dankito.banking.javafx.dialogs.DialogFragment
import net.dankito.banking.javafx.dialogs.bankdetails.controls.BankDetailsField
import net.dankito.banking.model.BankInfo
import tornadofx.*


class BankDetailsDialog : DialogFragment() {

    val bankInfo: BankInfo by param()


    override val root = vbox {

        add(BankDetailsField(messages["bank.details.name.label"], bankInfo.info.name))

        add(BankDetailsField(messages["bank.details.location.label"], bankInfo.info.location))

        add(BankDetailsField(messages["bank.details.bank.code.label"], bankInfo.info.blz))

        add(BankDetailsField(messages["bank.details.bic.label"], bankInfo.info.bic))

        add(BankDetailsField(messages["bank.details.pin.tan.address.label"], bankInfo.info.pinTanAddress))

        add(BankDetailsField(messages["bank.details.pin.tan.version.label"], bankInfo.info.pinTanVersion.getName()))

        add(BankDetailsField(messages["bank.details.rdh.address.label"], bankInfo.info.rdhAddress))

        add(BankDetailsField(messages["bank.details.rdh.version.label"], bankInfo.info.rdhVersion.getName()))

    }

}