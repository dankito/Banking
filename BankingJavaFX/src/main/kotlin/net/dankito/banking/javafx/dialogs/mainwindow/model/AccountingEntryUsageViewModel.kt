package net.dankito.banking.javafx.dialogs.mainwindow.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import net.dankito.banking.model.AccountingEntry
import tornadofx.*


class AccountingEntryUsageViewModel : ItemViewModel<AccountingEntry>() {

    val type = bind { SimpleStringProperty(item?.type) }

    val showOtherName = bind { SimpleBooleanProperty(item?.showOtherName() ?: false)}

    val otherName = bind { SimpleStringProperty(item?.other?.name) }

    val usage1 = bind { SimpleStringProperty(item?.getUsage1()) }

    val isUsage2Set = bind { SimpleBooleanProperty(item?.getUsage2() != null) }

    val usage2 = bind { SimpleStringProperty(item?.getUsage2()) }

}