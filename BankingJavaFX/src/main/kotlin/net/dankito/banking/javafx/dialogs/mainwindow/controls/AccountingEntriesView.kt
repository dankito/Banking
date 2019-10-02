package net.dankito.banking.javafx.dialogs.mainwindow.controls

import javafx.beans.binding.ObjectBinding
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.util.Callback
import net.dankito.banking.javafx.dialogs.mainwindow.MainWindowController
import net.dankito.banking.javafx.util.JavaFXDialogService
import net.dankito.banking.model.Account
import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.AccountingEntry
import net.dankito.utils.exception.ExceptionHelper
import net.dankito.utils.javafx.ui.controls.addButton
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import org.slf4j.LoggerFactory
import tornadofx.*
import java.text.DateFormat
import java.util.*
import kotlin.concurrent.schedule


class AccountingEntriesView(private val controller: MainWindowController) : View() {

    companion object {
        private const val CheckAccountsInterval = 6 * 24 * 60 * 60 * 1000L // check every 6 hours

        private val BookingDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

        private val logger = LoggerFactory.getLogger(AccountingEntriesView::class.java)
    }


    private var searchTextField: TextField by singleAssign()

    private var balanceLabel: Label by singleAssign()

    private var updateAccountingEntriesButton: Button by singleAssign()

    private var createCashTransferButton: Button by singleAssign()

    private var currentMenu: ContextMenu? = null


    private var currentSelectedAccount: Account? = null

    private val entriesOfSelectedAccount = FXCollections.observableArrayList<AccountingEntry>()

    private val entriesToShow = FXCollections.observableArrayList<AccountingEntry>()


    private val dialogService = JavaFXDialogService()

    private val exceptionHelper = ExceptionHelper()


    init {
        Timer().schedule(CheckAccountsInterval, CheckAccountsInterval) {
            // TODO: check all accounts not only currently displayed one periodically
            logger.info("Updating accounting data for $currentSelectedAccount")
            updateAccountingEntries()
        }
    }


    override val root = vbox {
        borderpane {
            fixedHeight = 36.0

            left = label(messages["accounting.entries.search.label"]) {
                borderpaneConstraints {
                    alignment = Pos.CENTER_LEFT
                    margin = Insets(4.0, 12.0, 4.0, 4.0)
                }
            }

            searchTextField = textfield {
                isDisable = true

                textProperty().addListener { _, _, newValue -> searchEntries(newValue) }
            }
            center = searchTextField

            right = hbox {
                alignment = Pos.CENTER_LEFT

                label(messages["accounting.entries.balance.label"]) {
                    hboxConstraints {
                        alignment = Pos.CENTER_LEFT
                        marginLeft = 48.0
                        marginRight = 12.0
                    }
                }

                balanceLabel = label {
                    minWidth = 50.0
                    alignment = Pos.CENTER_RIGHT
                }

                updateAccountingEntriesButton = button("Update") {
                    useMaxHeight = true
                    prefWidth = 125.0

                    isDisable = true

                    action { updateAccountingEntries() }

                    hboxConstraints {
                        marginLeft = 12.0
                    }
                }

                createCashTransferButton = addButton {
                    useMaxHeight = true

                    isDisable = true

                    action { createCashTransfer() }

                    hboxConstraints {
                        marginLeft = 12.0
                    }
                }
            }
        }

        tableview<AccountingEntry>(entriesToShow) {
            column(messages["accounting.entry.column.header.booking.date"], AccountingEntry::bookingDate).prefWidth(150.0).cellFormat {
                text = BookingDateFormat.format(it)
                alignment = Pos.CENTER_LEFT
                paddingLeft = 4.0
            }

            val usageColumn = TableColumn<AccountingEntry, AccountingEntry>(messages["accounting.entry.column.header.usage"])
            usageColumn.cellFragment(UsageCellFragment::class)
            usageColumn.cellValueFactory = Callback { object : ObjectBinding<AccountingEntry>() {
                override fun computeValue(): AccountingEntry {
                    return it.value
                }

            } }
            usageColumn.weightedWidth(4.0)
            columns.add(usageColumn)

            column(messages["accounting.entry.column.header.balance"], AccountingEntry::value).prefWidth(100.0).cellFormat {
                text = it.toString()
                alignment = Pos.CENTER_RIGHT
                paddingRight = 4.0

                style {
                    if(it.longValue < 0) {
                        textFill = Color.RED
                    }
                    else {
                        textFill = Color.GREEN
                    }
                }
            }


            columnResizePolicy = SmartResize.POLICY

            vgrow = Priority.ALWAYS

            setOnMouseClicked { tableClicked(it, this.selectionModel.selectedItem) }

            setOnContextMenuRequested { event -> showContextMenu(this, event) }
        }
    }

    private fun tableClicked(event: MouseEvent, selectedItem: AccountingEntry?) {
        if(event.clickCount == 2 && event.button == MouseButton.PRIMARY) {
            if(selectedItem != null) {
                controller.showAccountingEntriesDetailsDialog(selectedItem)
            }
        }
    }

    private fun showContextMenu(table: TableView<AccountingEntry>, event: ContextMenuEvent) {
        currentMenu?.hide()

        val selectedItems = table.selectionModel.selectedItems

        if (selectedItems.isNotEmpty()) {
            currentMenu = createContextMenuForItems(selectedItems.get(0))

            currentMenu?.show(table, event.screenX, event.screenY)
        }
    }

    private fun createContextMenuForItems(selectedEntry: AccountingEntry): ContextMenu? {
        val contextMenu = ContextMenu()

        contextMenu.apply {
            item(String.format(messages["accounting.entry.context.menu.create.cash.transfer"], selectedEntry.other.name)) {
                action { createCashTransfer(selectedEntry.other.name, selectedEntry.other.iban) }
            }
        }

        return contextMenu
    }


    private fun searchEntries() {
        searchEntries(searchTextField.text)
    }

    private fun searchEntries(query: String) {
        if(query.isEmpty()) {
            entriesToShow.setAll(entriesOfSelectedAccount)
        }
        else {
            entriesToShow.setAll(getSearchEntriesResult(query))
        }
    }

    private fun getSearchEntriesResult(query: String): ArrayList<AccountingEntry> {
        val result = ArrayList<AccountingEntry>()
        val lowerCaseQuery = query.toLowerCase()

        entriesOfSelectedAccount.forEach { entry ->
            if (entry.usage.toLowerCase().contains(lowerCaseQuery)
                    || entry.other.name?.toLowerCase()?.contains(lowerCaseQuery) == true
                    || BookingDateFormat.format(entry.bookingDate).contains(lowerCaseQuery)
                    || entry.value.bigDecimalValue.toString().contains(lowerCaseQuery)) {
                result.add(entry)
            }
        }

        return result
    }


    fun setCurrentAccountAndUpdateAccountingEntries(account: Account) {
        currentSelectedAccount = account

        searchTextField.isDisable = false
        updateAccountingEntriesButton.isDisable = false
        createCashTransferButton.isDisable = false

        setEntriesOfCurrentAccount(account, AccountingEntries(false))

        updateAccountingEntries(account)
    }

    private fun updateAccountingEntries() {
        currentSelectedAccount?.let { account ->
            updateAccountingEntries(account)
        }
    }

    private fun updateAccountingEntries(account: Account) {
        runLater {
            updateAccountingEntriesButton.isDisable = true
        }

        controller.getAccountingEntriesAsync(account) { result ->
            runLater { retrievedAccountingEntriesResultOnUiThread(account, result) }
        }
    }

    private fun retrievedAccountingEntriesResultOnUiThread(account: Account, result: AccountingEntries) {
        updateAccountingEntriesButton.isDisable = false

        if(result.successful) {
            setEntriesOfCurrentAccount(account, result)
        }
        else {
            result.error?.let { showCouldNotRetrieveAccountingEntriesError(account, it) }
        }
    }

    private fun setEntriesOfCurrentAccount(account: Account, accountingEntries: AccountingEntries) {
        val entries = controller.getStoredAccountingEntries(account) ?: accountingEntries

        setEntriesOfCurrentAccount(entries)
    }

    private fun setEntriesOfCurrentAccount(accountingEntries: AccountingEntries) {
        entriesOfSelectedAccount.setAll(accountingEntries.entries)

        searchEntries()

        balanceLabel.text = accountingEntries.saldo?.toString() ?: ""
    }

    private fun showCouldNotRetrieveAccountingEntriesError(account: Account, error: Exception) {
        val innerException = exceptionHelper.getInnerException(error)

        val message = String.format(messages["error.message.could.not.retrieve.accounting.entries"], account.credentials.customerId, innerException.localizedMessage)

        showError(message, error)
    }


    private fun createCashTransfer(remitteeName: String = "", remitteeIban: String = "") {
        currentSelectedAccount?.let { account ->
            controller.showCreateCashTransferDialog(account, remitteeName, remitteeIban)
        }
    }


    private fun showError(message: String, exception: Exception) {
        dialogService.showErrorMessage(message, null, exception, currentStage)
    }

}
