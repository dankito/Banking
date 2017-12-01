package net.dankito.banking.javafx.dialogs.mainwindow.model

import net.dankito.banking.model.BankInfo


class AccountsBankInfoTreeItem(val bankInfo: BankInfo) : AccountsTreeItemBase(bankInfo.info.name) {

    init {
        isExpanded = true

        bankInfo.accounts.forEach { account ->
            children.add(AccountsAccountTreeItem(account))
        }
    }

}