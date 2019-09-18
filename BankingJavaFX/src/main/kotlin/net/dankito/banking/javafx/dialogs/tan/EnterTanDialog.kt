package net.dankito.banking.javafx.dialogs.tan

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import net.dankito.banking.javafx.dialogs.DialogFragment
import net.dankito.banking.javafx.dialogs.tan.controls.ChipTanFlickerCodeView
import net.dankito.banking.tan.ChipTanData
import net.dankito.banking.tan.TanData
import net.dankito.banking.tan.TanProcedure
import tornadofx.*


class EnterTanDialog(private val data: TanData,
                     private val enteringTanDone: (String?) -> Unit) : DialogFragment() {

    companion object {
        private val ButtonHeight = 40.0
        private val ButtonWidth = 150.0
    }


    private val enteredTan = SimpleStringProperty("")

    private val isChipTanProcedure = data.procedure == TanProcedure.ChipTan


    override val root = vbox {
        paddingAll = 4.0

        (data as? ChipTanData)?.let { chipTanData ->
            hbox {
                alignment = Pos.CENTER

                vboxConstraints {
                    marginLeftRight(30.0)
                }

                add(ChipTanFlickerCodeView(chipTanData.flickerData))
            }
        }

        hbox {
            maxWidth = 400.0

            label(data.messageToShowToUser) {
                isWrapText = true
            }

            vboxConstraints {
                marginTopBottom(6.0)

                if (isChipTanProcedure) {
                    marginTop = 18.0
                }
            }
        }

        hbox {
            alignment = Pos.CENTER_LEFT

            label(messages["enter.tan.dialog.enter.tan.label"])

            textfield(enteredTan) {
                prefHeight = 30.0
                prefWidth = 110.0

                runLater {
                    requestFocus()
                }

                setOnKeyReleased { keyReleasedOnTanTextField(it) }

                hboxConstraints {
                    marginLeft = 6.0
                }
            }

            vboxConstraints {
                marginBottom = 4.0
            }
        }

        hbox {
            alignment = Pos.CENTER_RIGHT

            button(messages["dialog.button.cancel"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                action { cancelEnteringTang() }

                hboxConstraints {
                    margin = Insets(6.0, 0.0, 4.0, 0.0)
                }
            }

            button(messages["dialog.button.ok"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                action { tanEntered() }

                hboxConstraints {
                    margin = Insets(6.0, 4.0, 4.0, 12.0)
                }
            }
        }
    }


    private fun keyReleasedOnTanTextField(event: KeyEvent) {
        if(event.code == KeyCode.ENTER) {
            tanEntered()
        }
    }

    private fun tanEntered() {
        if (enteredTan.value.isNullOrEmpty()) {
            enteringTanDone(null)
        }
        else {
            enteringTanDone(enteredTan.value)
        }

        close()
    }

    private fun cancelEnteringTang() {
        enteringTanDone(null)

        close()
    }

}