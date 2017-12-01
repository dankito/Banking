package net.dankito.banking.javafx.dialogs.mainwindow.controls

import javafx.geometry.Insets
import net.dankito.banking.javafx.dialogs.mainwindow.model.AccountingEntryUsageViewModel
import net.dankito.banking.javafx.util.FXUtils
import net.dankito.banking.model.AccountingEntry
import tornadofx.*


class UsageCellFragment : TableCellFragment<AccountingEntry, AccountingEntry>() {

    companion object {
        private val LabelMargin = Insets(4.0, 0.0, 4.0, 4.0)
    }


    val entry = AccountingEntryUsageViewModel().bindToItem(this)


    override val root = vbox {
        label(entry.type) {
            vboxConstraints {
                margin = LabelMargin
            }
        }

        label(entry.otherName) {
            visibleProperty().bind(entry.showOtherName)
            FXUtils.ensureNodeOnlyUsesSpaceIfVisible(this)

            vboxConstraints {
                margin = LabelMargin
            }
        }

        label(entry.usage1) {
            vboxConstraints {
                margin = LabelMargin
            }
        }

        label(entry.usage2) {
            visibleProperty().bind(entry.isUsage2Set)
            FXUtils.ensureNodeOnlyUsesSpaceIfVisible(this)

            vboxConstraints {
                margin = Insets(0.0, LabelMargin.right, LabelMargin.bottom, LabelMargin.left)
            }
        }
    }

}