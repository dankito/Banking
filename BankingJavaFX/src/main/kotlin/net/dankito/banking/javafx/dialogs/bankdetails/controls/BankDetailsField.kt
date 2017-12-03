package net.dankito.banking.javafx.dialogs.bankdetails.controls

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.Priority
import tornadofx.*


class BankDetailsField(private val fieldName: String, private val fieldValue: String) : View() {

    companion object {
        private val FieldHeight = 32.0
        private val FieldNameWidth = 150.0
        private val FieldValueWidth = 500.0
    }


    private var lblFieldName: Label by singleAssign()

    private var txtfldFieldValue: TextField by singleAssign()


    override val root = hbox {

        lblFieldName = label(fieldName) {
            prefHeight = FieldHeight
            prefWidth = FieldNameWidth

            hboxConstraints {
                alignment = Pos.CENTER_RIGHT
                marginRight = 12.0
            }
        }

        txtfldFieldValue = textfield(fieldValue) {
            isEditable = false
            prefHeight = FieldHeight
            prefWidth = FieldValueWidth

            hboxConstraints {
                hgrow = Priority.ALWAYS
                marginRight = 4.0
            }
        }

        vboxConstraints {
            marginBottom = 6.0
        }

    }

}