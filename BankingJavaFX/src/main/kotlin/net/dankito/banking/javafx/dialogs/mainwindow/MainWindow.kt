package net.dankito.banking.javafx.dialogs.mainwindow

import javafx.scene.control.SplitPane
import net.dankito.banking.javafx.dialogs.mainwindow.controls.AccountingEntriesView
import net.dankito.banking.javafx.dialogs.mainwindow.controls.AccountsView
import net.dankito.banking.javafx.dialogs.mainwindow.controls.IMainView
import net.dankito.banking.model.Account
import net.dankito.banking.model.BankInfo
import tornadofx.*
import tornadofx.FX.Companion.messages


class MainWindow : View(messages["main.window.title"]), IMainView {

    private val controller: MainWindowController by inject()


    private var splpnContent: SplitPane by singleAssign()

    private var accountsView: AccountsView by singleAssign()

    private var accountingEntriesView: AccountingEntriesView by singleAssign()



    override val root = borderpane {
        prefHeight = 620.0
        prefWidth = 1150.0

        center {
            splpnContent = splitpane {
                accountsView = AccountsView(controller)
                add(accountsView.root)

                accountingEntriesView = AccountingEntriesView(controller)
                add(accountingEntriesView)
            }

            splpnContent.setDividerPosition(0, 0.2)
        }
    }


    init {
        accountsView.selectedAccountChangedListener = { selectedAccountChanged(it) }

        controller.mainView = this
    }


    private fun selectedAccountChanged(account: Account) {
        accountingEntriesView.retrieveAndShowEntriesForAccount(account)
    }

    override fun showAccounts(bankInfos: List<BankInfo>) {
        runLater {
            accountsView.showAccounts(bankInfos)
        }
    }

}