package net.dankito.banking.javafx.dialogs.tan

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import net.dankito.banking.javafx.dialogs.DialogFragment
import net.dankito.banking.tan.SelectTanProcedure
import tornadofx.*


class SelectTanProcedureDialog(private val selectableTanProcedures: List<SelectTanProcedure>,
                               private val procedureSelected: (SelectTanProcedure?) -> Unit
) : DialogFragment() {

    companion object {
        private val ButtonHeight = 40.0
        private val ButtonWidth = 150.0
    }


    private val selectedTanProcedure = SimpleObjectProperty<SelectTanProcedure>(null)


    override val root = vbox {
        prefWidth = 400.0
        paddingAll = 8.0

        label(messages["select.tan.procedure.select.tan.procedure.label"])

        combobox(selectedTanProcedure, selectableTanProcedures) {
            prefHeight = 34.0

            cellFormat {
                text = it.displayName
            }

            vboxConstraints {
                marginTop = 12.0
                marginBottom = 12.0
            }
        }

        hbox {
            alignment = Pos.CENTER_RIGHT

            button(messages["dialog.button.cancel"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                action { cancelSelectingTanProcedure() }

                hboxConstraints {
                    margin = Insets(6.0, 0.0, 4.0, 0.0)
                }
            }

            button(messages["dialog.button.ok"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                action { tanProcedureSelected() }

                hboxConstraints {
                    margin = Insets(6.0, 4.0, 4.0, 12.0)
                }
            }

            vboxConstraints {
                marginTop = 4.0
            }
        }
    }


    private fun tanProcedureSelected() {
        procedureSelected(selectedTanProcedure.value)

        close()
    }

    private fun cancelSelectingTanProcedure() {
        procedureSelected(null)

        close()
    }

}