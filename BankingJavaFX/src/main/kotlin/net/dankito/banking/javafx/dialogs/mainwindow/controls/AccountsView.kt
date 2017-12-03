package net.dankito.banking.javafx.dialogs.mainwindow.controls

import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority
import javafx.stage.StageStyle
import net.dankito.banking.javafx.dialogs.accountdetails.AccountDetailsDialog
import net.dankito.banking.javafx.dialogs.bankdetails.BankDetailsDialog
import net.dankito.banking.javafx.dialogs.mainwindow.MainWindowController
import net.dankito.banking.javafx.dialogs.mainwindow.model.AccountsAccountTreeItem
import net.dankito.banking.javafx.dialogs.mainwindow.model.AccountsBankInfoTreeItem
import net.dankito.banking.javafx.dialogs.mainwindow.model.AccountsRootTreeItem
import net.dankito.banking.model.Account
import net.dankito.banking.model.BankInfo
import tornadofx.*


class AccountsView(private val controller: MainWindowController) : View() {

    var selectedAccountChangedListener: ((Account) -> Unit)? = null


    private val bankInfos = FXCollections.observableArrayList<BankInfo>()

    private var trvwAccounts: TreeView<String> by singleAssign()


    override val root = vbox {
        borderpane {
            minHeight = 36.0
            maxHeight = 36.0

            left = label(messages["accounts.view.accounts.label"]) {
                borderpaneConstraints {
                    alignment = Pos.CENTER_LEFT
                    marginLeft = 4.0
                }
            }

            right = button("+") {
                minWidth = 32.0
                maxWidth = 32.0
                minHeight = 32.0
                maxHeight = 32.0

                setOnMouseClicked { clickedAddAccount(it) }

                borderpaneConstraints {
                    alignment = Pos.CENTER_RIGHT
                    marginTopBottom(2.0)
                }
            }
        }

        trvwAccounts = treeview(AccountsRootTreeItem(bankInfos)) {
            root.isExpanded = true
            isShowRoot = false

            contextmenu {
                item(messages["accounts.view.context.menu.info"]) {
                    action {
                        this@treeview.selectionModel.selectedItem?.let { showInfo(it) }
                    }
                }
            }

            vboxConstraints {
                vGrow = Priority.ALWAYS
            }
        }
    }


    init {
        trvwAccounts.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if(newValue is AccountsAccountTreeItem) {
                selectedAccountChangedListener?.invoke(newValue.account)
            }
        }
    }

    fun showAccounts(bankInfos: List<BankInfo>) {
        this.bankInfos.setAll(bankInfos)

        if(trvwAccounts.selectionModel.selectedItem == null) {
            if(trvwAccounts.root.children.size > 0 && trvwAccounts.root.children[0].children.size > 0) { // select first Account TreeItem
                trvwAccounts.selectionModel.select(trvwAccounts.root.children[0].children[0])
            }
        }
    }


    private fun clickedAddAccount(event: MouseEvent) {
        if(event.button == MouseButton.PRIMARY) {
            controller.showAddAccountDialog()
        }
    }

    private fun showInfo(treeItem: TreeItem<String>) {
        if(treeItem is AccountsBankInfoTreeItem) {
            find(BankDetailsDialog::class.java, mapOf(BankDetailsDialog::bankInfo to treeItem.bankInfo))
                    .show(messages["bank.details.name.title"], stageStyle = StageStyle.UTILITY, owner = currentStage)
        }
        else if(treeItem is AccountsAccountTreeItem) {
            find(AccountDetailsDialog::class.java, mapOf(AccountDetailsDialog::account to treeItem.account))
                    .show(messages["account.details.name.title"], stageStyle = StageStyle.UTILITY, owner = currentStage)
        }
    }

}