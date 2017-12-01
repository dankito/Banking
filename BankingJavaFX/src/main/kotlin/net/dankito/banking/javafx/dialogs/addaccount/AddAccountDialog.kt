package net.dankito.banking.javafx.dialogs.addaccount

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import net.dankito.banking.javafx.dialogs.DialogFragment
import net.dankito.banking.javafx.dialogs.mainwindow.MainWindowController
import net.dankito.banking.model.AccountCredentials
import net.dankito.banking.model.GetAccountsResult
import tornadofx.*


class AddAccountDialog : DialogFragment() {

    companion object {
        private val LabelMargins = Insets(6.0, 4.0, 6.0, 4.0)

        private val TextFieldHeight = 36.0
        private val TextFieldMargins = Insets(0.0, 4.0, 12.0, 4.0)

        private val ButtonHeight = 40.0
        private val ButtonWidth = 150.0
    }


    private var txtfldBankCode: TextField by singleAssign()

    private var txtfldCustomerId: TextField by singleAssign()

    private var txtfldPassword: TextField by singleAssign()


    val controller: MainWindowController by param()


    override val root = vbox {
        prefWidth = 350.0

        label(messages["add.account.dialog.bank.code.label"]) {
            vboxConstraints {
                margin = LabelMargins
            }
        }

        txtfldBankCode = textfield {
//            promptText = messages[""]
            prefHeight = TextFieldHeight

            setOnKeyReleased { keyReleased(it) }

            vboxConstraints {
                margin = TextFieldMargins
            }
        }

        label(messages["add.account.dialog.customer.id"]) {
            vboxConstraints {
                margin = LabelMargins
            }
        }

        txtfldCustomerId = textfield {
            promptText = messages["add.account.dialog.customer.id.hint"]
            prefHeight = TextFieldHeight

            setOnKeyReleased { keyReleased(it) }

            vboxConstraints {
                margin = TextFieldMargins
            }
        }

        label(messages["add.account.dialog.password"]) {
            vboxConstraints {
                margin = LabelMargins
            }
        }

        txtfldPassword = passwordfield {
            promptText = messages["add.account.dialog.password.hint"]
            prefHeight = TextFieldHeight

            setOnKeyReleased { keyReleased(it) }

            vboxConstraints {
                margin = TextFieldMargins
            }
        }

        hbox {
            alignment = Pos.CENTER_RIGHT

            button(messages["dialog.button.cancel"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                setOnMouseClicked { clickedCancelButton(it) }

                hboxConstraints {
                    margin = Insets(6.0, 0.0, 4.0, 0.0)
                }
            }

            button(messages["dialog.button.ok"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                setOnMouseClicked { clickedOkButton(it) }

                hboxConstraints {
                    margin = Insets(6.0, 4.0, 4.0, 12.0)
                }
            }
        }
    }


    private fun clickedCancelButton(event: MouseEvent) {
        if(event.button == MouseButton.PRIMARY) {
            close()
        }
    }

    private fun clickedOkButton(event: MouseEvent) {
        if(event.button == MouseButton.PRIMARY) {
            checkEnteredCredentials()
        }
    }

    private fun keyReleased(event: KeyEvent) {
        if(event.code == KeyCode.ENTER) {
            checkEnteredCredentials()
        }
    }

    private fun checkEnteredCredentials() {
        val credentials = AccountCredentials(txtfldBankCode.text, txtfldCustomerId.text, txtfldPassword.text)

        controller.addAccountAsync(credentials) { result ->
            runLater { retrievedGetAccountsResult(credentials, result) }
        }
    }

    private fun retrievedGetAccountsResult(credentials: AccountCredentials, result: GetAccountsResult) {
        result.error?.let { showError(messages["error.message.could.not.add.account"], credentials, it) }

        result.bankInfo?.let { close() }
    }

    private fun showError(errorMessage: String, credentials: AccountCredentials, error: Exception) {
        // TODO
    }

}