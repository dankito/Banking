package net.dankito.banking.javafx.dialogs.mainwindow

import javafx.scene.control.SplitPane
import net.dankito.banking.javafx.dialogs.mainwindow.controls.AccountingEntriesView
import net.dankito.banking.javafx.dialogs.mainwindow.controls.AccountsView
import net.dankito.banking.javafx.dialogs.mainwindow.controls.IMainView
import net.dankito.banking.javafx.util.JavaFXDialogService
import net.dankito.banking.model.Account
import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.BankInfo
import net.dankito.banking.util.ExceptionHelper
import tornadofx.*
import tornadofx.FX.Companion.messages


class MainWindow : View(messages["main.window.title"]), IMainView {

    private val controller: MainWindowController by inject()


    private var splpnContent: SplitPane by singleAssign()

    private var accountsView: AccountsView by singleAssign()

    private var accountingEntriesView: AccountingEntriesView by singleAssign()


    private val dialogService = JavaFXDialogService()

    private val exceptionHelper = ExceptionHelper()



    override val root = borderpane {
        prefHeight = 620.0
        prefWidth = 1150.0

        center {
            splpnContent = splitpane {
                accountsView = AccountsView(controller)
                add(accountsView.root)

                accountingEntriesView = AccountingEntriesView()
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
        controller.getAccountingEntriesAsync(account) { result ->
            runLater { retrievedAccountingEntriesResult(account, result) }
        }
    }

    private fun retrievedAccountingEntriesResult(account: Account, result: AccountingEntries) {
        if(result.successful) {
            accountingEntriesView.setEntriesOfCurrentAccount(result)
        }
        else {
            result.error?.let { showCouldNotRetrieveAccountingEntriesError(account, it) }
        }
    }

    override fun showAccounts(bankInfos: List<BankInfo>) {
        runLater {
            accountsView.showAccounts(bankInfos)
        }
    }

    private fun showCouldNotRetrieveAccountingEntriesError(account: Account, error: Exception) {
        val innerException = exceptionHelper.getInnerException(error)

        val message = String.format(messages["error.message.could.not.retrieve.accounting.entries"], account.credentials.customerId, innerException.localizedMessage)

        showError(message, error)
    }

    override fun showError(message: String, exception: Exception) {
        dialogService.showErrorMessage(message, null, exception, currentStage)
    }

}