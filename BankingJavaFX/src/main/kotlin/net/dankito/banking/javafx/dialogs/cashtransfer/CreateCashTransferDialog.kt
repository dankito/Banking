package net.dankito.banking.javafx.dialogs.cashtransfer

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import net.dankito.banking.javafx.dialogs.DialogFragment
import net.dankito.banking.javafx.dialogs.mainwindow.MainWindowController
import net.dankito.banking.javafx.util.JavaFXDialogService
import net.dankito.banking.model.*
import net.dankito.utils.javafx.ui.controls.doubleTextfield
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.fixedWidth
import tornadofx.*
import java.math.BigDecimal


class CreateCashTransferDialog : DialogFragment() {

    companion object {
        private val TextFieldHeight = 32.0

        private val ButtonHeight = 40.0
        private val ButtonWidth = 150.0
    }


    val controller: MainWindowController by param()

    val account: Account by param()

    val selectedRemitteeName: String by param("")
    val selectedRemitteeIban: String by param("")


    private var btnOk: Button by singleAssign()


    private val remitteeName = SimpleStringProperty(selectedRemitteeName)
    private val remitteeIban = SimpleStringProperty(selectedRemitteeIban)
    private val remitteeBankName = SimpleStringProperty()
    private var remitteeBank: BankInfo? = null

    private val amount = SimpleDoubleProperty()

    private val usage = SimpleStringProperty()

    private val dialogService = JavaFXDialogService()


    init {
        remitteeIban.addListener { _, _, newValue -> checkIfIsValidBank(newValue) }
        amount.addListener { _, _, _ -> checkIfRequiredInformationEntered() }
    }


    override val root = form {
        prefWidth = 550.0

        fieldset {
            field(messages["create.cash.transfer.remittee.name.label"]) {
                textfield(remitteeName) {
                    fixedHeight = TextFieldHeight

                    setOnKeyReleased { keyReleased(it) }
                }
            }

            field(messages["create.cash.transfer.remittee.iban.label"]) {
                textfield(remitteeIban) {
                    fixedHeight = TextFieldHeight

                    setOnKeyReleased { keyReleased(it) }

                    if (remitteeName.value.isNotBlank()) {
                        runLater {
                            requestFocus()
                        }
                    }
                }
            }

            field(messages["create.cash.transfer.remittee.bank.label"]) {
                label(remitteeBankName) {
                    isDisable = true

                    paddingLeft = 8.0
                }
            }

            field(messages["create.cash.transfer.amount.label"]) {
                anchorpane {
                    doubleTextfield(amount, false) {
                        fixedHeight = TextFieldHeight
                        fixedWidth = 100.0
                        alignment = Pos.CENTER_RIGHT

                        setOnKeyReleased { keyReleased(it) }

                        if (remitteeName.value.isNotBlank() && remitteeIban.value.isNotBlank()) {
                            runLater {
                                requestFocus()
                            }
                        }

                        anchorpaneConstraints {
                            topAnchor = 0.0
                            rightAnchor = 20.0
                            bottomAnchor = 0.0
                        }
                    }

                    label("€") {

                        anchorpaneConstraints {
                            topAnchor = 0.0
                            rightAnchor = 0.0
                            bottomAnchor = 0.0
                        }
                    }
                }
            }

            field(messages["create.cash.transfer.usage.label"]) {
                textfield(usage) {
                    fixedHeight = TextFieldHeight

                    setOnKeyReleased { keyReleased(it) }
                }
            }
        }

        hbox {
            alignment = Pos.CENTER_RIGHT

            button(messages["dialog.button.cancel"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                action { cancelCashTransfer() }

                hboxConstraints {
                    margin = Insets(6.0, 0.0, 4.0, 0.0)
                }
            }

            btnOk = button(messages["dialog.button.ok"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                isDisable = true

                action { doCashTransfer() }

                hboxConstraints {
                    margin = Insets(6.0, 4.0, 4.0, 12.0)
                }
            }
        }

        checkIfIsValidBank(remitteeIban.value)
    }


    private fun checkIfIsValidBank(enteredIban: String?) {
        enteredIban?.let {
            remitteeBank = controller.findBankByIban(account, enteredIban)

            val foundBankLabel = determineFoundBankLabel(enteredIban, remitteeBank?.info)
            remitteeBankName.value = foundBankLabel

            checkIfRequiredInformationEntered()
        }
    }

    private fun determineFoundBankLabel(enteredIban: String?, bankInfo: org.kapott.hbci.manager.BankInfo?): String? {
        if (bankInfo != null) {
            return bankInfo.name + " " + bankInfo.location
        }
        else {
            return if (enteredIban.isNullOrBlank()) {
                messages["create.cash.transfer.bank.name.will.be.entered.automatically"]
            }
            else {
                messages["create.cash.transfer.bank.not.found.for.iban"]
            }
        }
    }

    private fun checkIfRequiredInformationEntered() {
        val requiredInformationEntered =
                remitteeName.value.isNotBlank()
                        && remitteeBank != null
                        && amount.value > 0

        btnOk.isDisable = !!!requiredInformationEntered
    }

    private fun keyReleased(event: KeyEvent) {
        if(event.code == KeyCode.ENTER) {
            doCashTransfer()
        }
    }


    private fun cancelCashTransfer() {
        close()
    }

    private fun doCashTransfer() {
        remitteeBank?.let { remitteeBank ->

            val source = SepaParty(account.info.name, account.info.iban, account.info.bic, account.info.country, account
                    .info.blz, account.info.number)
            val remittee = SepaParty(remitteeName.value, remitteeIban.value, remitteeBank.info.bic)

            val cashTransfer = CashTransfer(source, remittee, BigDecimal.valueOf(amount.value), usage.value)

            controller.transferCashAsync(account, cashTransfer) { result -> showTransferCashResult(cashTransfer, result) }
        }
    }

    private fun showTransferCashResult(transfer: CashTransfer, result: CashTransferResult) {
        runLater {
            showTransferCashResultOnUiThread(transfer, result)
        }
    }

    private fun showTransferCashResultOnUiThread(transfer: CashTransfer, result: CashTransferResult) {
        if (result.successful) {
            dialogService.showInfoMessage(String.format(messages["create.cash.transfer.message.transfer.cash.success"], transfer
                    .amount.toDouble(), transfer.destination.name), null, currentStage)
        }
        else {
            dialogService.showErrorMessage(String.format(messages["create.cash.transfer.message.transfer.cash.error"], transfer
                    .amount.toDouble(), transfer.destination.name, result.message), null, result.error, currentStage)
        }

        close()
    }

}