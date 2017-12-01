package net.dankito.banking.javafx.dialogs.mainwindow.model

import net.dankito.banking.model.Account


class AccountsAccountTreeItem(val account: Account) : AccountsTreeItemBase("${account.info.number} (${account.info.type})")