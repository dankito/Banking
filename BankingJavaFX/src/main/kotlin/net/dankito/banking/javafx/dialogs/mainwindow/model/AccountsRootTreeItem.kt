package net.dankito.banking.javafx.dialogs.mainwindow.model

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import net.dankito.banking.javafx.util.FXUtils
import net.dankito.banking.model.BankInfo
import tornadofx.*
import tornadofx.FX.Companion.messages


class AccountsRootTreeItem(bankInfos: ObservableList<BankInfo>) : AccountsTreeItemBase(messages["accounts.view.accounts.label"]) {

    init {
        bankInfos.forEach { bankInfo ->
            children.add(AccountsBankInfoTreeItem(bankInfo))
        }

        bankInfos.addListener(ListChangeListener<BankInfo> { c ->
            FXUtils.runOnUiThread { bankInfosChanged(c) }
        })
    }

    private fun bankInfosChanged(c: ListChangeListener.Change<out BankInfo>) {
        while(c.next()) {
            c.removed.forEach { removedBankInfo ->
                ArrayList(children).forEach { child ->
                    if(child is AccountsBankInfoTreeItem && child.bankInfo == removedBankInfo) {
                        children.remove(child)
                        return@forEach
                    }
                }
            }

            c.addedSubList.forEach { addedBankInfo ->
                children.add(AccountsBankInfoTreeItem(addedBankInfo))
            }
        }
    }
}