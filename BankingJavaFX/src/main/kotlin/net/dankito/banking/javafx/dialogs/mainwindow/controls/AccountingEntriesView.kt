package net.dankito.banking.javafx.dialogs.mainwindow.controls

import javafx.beans.binding.ObjectBinding
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.StageStyle
import javafx.util.Callback
import net.dankito.banking.javafx.dialogs.accountingentriesdetails.AccountingEntriesDetailsDialog
import net.dankito.banking.model.AccountingEntries
import net.dankito.banking.model.AccountingEntry
import tornadofx.*
import java.text.DateFormat


class AccountingEntriesView : View() {

    companion object {
        private val BookingDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    }


    private var searchTextField: TextField by singleAssign()

    private var balanceLabel: Label by singleAssign()


    private val entriesOfSelectedAccount = FXCollections.observableArrayList<AccountingEntry>()

    private val entriesToShow = FXCollections.observableArrayList<AccountingEntry>()


    override val root = vbox {
        borderpane {
            minHeight = 36.0
            maxHeight = 36.0

            left = label(messages["accounting.entries.search.label"]) {
                borderpaneConstraints {
                    alignment = Pos.CENTER_LEFT
                    margin = Insets(4.0, 12.0, 4.0, 4.0)
                }
            }

            searchTextField = textfield {
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
                add(balanceLabel)
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
            usageColumn.weigthedWidth(4.0)
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
        }
    }

    private fun tableClicked(event: MouseEvent, selectedItem: AccountingEntry?) {
        if(event.clickCount == 2 && event.button == MouseButton.PRIMARY) {
            if(selectedItem != null) {
                find(AccountingEntriesDetailsDialog::class.java, mapOf(AccountingEntriesDetailsDialog::entry to selectedItem))
                        .show(messages["accounting.entries.details.title"], stageStyle = StageStyle.UTILITY, owner = currentStage)
            }
        }
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


    fun setEntriesOfCurrentAccount(accountingEntries: AccountingEntries) {
        entriesOfSelectedAccount.setAll(accountingEntries.entries)

        searchEntries()

        balanceLabel.text = accountingEntries.saldo?.toString() ?: ""
    }

}
