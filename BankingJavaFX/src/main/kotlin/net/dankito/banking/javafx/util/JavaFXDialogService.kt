package net.dankito.banking.javafx.util

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Screen
import javafx.stage.Stage
import tornadofx.*
import java.io.PrintWriter
import java.io.StringWriter


class JavaFXDialogService()  {

    fun showInfoMessage(infoMessage: CharSequence, alertTitle: CharSequence?) {
        showInfoMessage(infoMessage, alertTitle, null)
    }

    fun showInfoMessage(infoMessage: CharSequence, alertTitle: CharSequence?, owner: Stage?) {
        FXUtils.runOnUiThread { showInfoMessageOnUiThread(infoMessage, alertTitle, owner) }
    }

    private fun showInfoMessageOnUiThread(infoMessage: CharSequence, alertTitle: CharSequence?, owner: Stage?) {
        val alert = createDialog(Alert.AlertType.INFORMATION, infoMessage, alertTitle, owner, ButtonType.OK)

        alert.showAndWait()
    }


//    fun showConfirmationDialog(message: CharSequence, alertTitle: CharSequence?, config: ConfirmationDialogConfig, optionSelected: (ConfirmationDialogButton) -> Unit) {
//        showConfirmationDialog(message, alertTitle, config, null, optionSelected)
//    }
//
//    fun showConfirmationDialog(message: CharSequence, alertTitle: CharSequence?, config: ConfirmationDialogConfig = ConfirmationDialogConfig(), owner: Stage?, optionSelected: (ConfirmationDialogButton) -> Unit) {
//        FXUtils.runOnUiThread { showConfirmationDialogOnUiThread(message, alertTitle, config, owner, optionSelected) }
//    }
//
//    private fun showConfirmationDialogOnUiThread(message: CharSequence, alertTitle: CharSequence?, config: ConfirmationDialogConfig, owner: Stage?, optionSelected: (ConfirmationDialogButton) -> Unit) {
//        val buttons = ArrayList<ButtonType>()
//        if(config.showNoButton) {
//            buttons.add(ButtonType.NO)
//        }
//        if(config.showThirdButton) {
//            buttons.add(ButtonType.CANCEL) // TODO: test
//        }
//        buttons.add(ButtonType.YES)
//
//        val alert = createDialog(Alert.AlertType.CONFIRMATION, message, alertTitle, owner, *buttons.toTypedArray())
//
//        when(alert.showAndWait().get()) {
//            ButtonType.NO -> return optionSelected(ConfirmationDialogButton.No)
//            else -> return optionSelected(ConfirmationDialogButton.Confirm)
//        }
//    }


    fun showErrorMessage(errorMessage: CharSequence, alertTitle: CharSequence?, exception: Exception?) {
        showErrorMessage(errorMessage, alertTitle, exception, null)
    }

    fun showErrorMessage(errorMessage: CharSequence, alertTitle: CharSequence?, exception: Exception?, owner: Stage?) {
        FXUtils.runOnUiThread { showErrorMessageOnUiThread(errorMessage, alertTitle, exception, owner) }
    }

    private fun showErrorMessageOnUiThread(errorMessage: CharSequence, alertTitle: CharSequence?, exception: Exception?, owner: Stage?) {
        val alert = createDialog(Alert.AlertType.ERROR, errorMessage, alertTitle, owner, ButtonType.OK)

        if (exception != null) {
            createExpandableException(alert, exception)
        }

        alert.showAndWait()
    }

    private fun createExpandableException(alert: Alert, exception: Exception) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        val exceptionText = sw.toString()

        val label = Label("The exception stacktrace was:")

        val textArea = TextArea(exceptionText)
        textArea.isEditable = false
        textArea.isWrapText = true

        textArea.useMaxWidth = true
        textArea.useMaxHeight = true
        GridPane.setVgrow(textArea, Priority.ALWAYS)
        GridPane.setHgrow(textArea, Priority.ALWAYS)

        val expContent = GridPane()
        expContent.useMaxWidth = true
        expContent.add(label, 0, 0)
        expContent.add(textArea, 0, 1)

        // Set expandable Exception into the dialog pane.
        alert.dialogPane.expandableContent = expContent
    }


    private fun createDialog(alertType: Alert.AlertType, message: CharSequence, alertTitle: CharSequence?, owner: Stage?, vararg buttons: ButtonType): Alert {
        val alert = Alert(alertType)

        (alertTitle as? String)?.let { alert.title = it }

        owner?.let { alert.initOwner(it) }

        (message as? String)?.let { setAlertContent(alert, it) }
        alert.headerText = null

        alert.buttonTypes.setAll(*buttons)

        return alert
    }

    private fun setAlertContent(alert: Alert, content: String) {
        var maxWidth = Screen.getPrimary().visualBounds.width

        if(alert.owner != null) {
//            FXUtils.getScreenWindowLeftUpperCornerIsIn(alert.owner)?.let { ownersScreen ->
//                maxWidth = ownersScreen.visualBounds.width
//            }
        }

        maxWidth *= 0.6 // set max width to 60 % of Screen width

        val contentLabel = Label(content)
        contentLabel.isWrapText = true
        contentLabel.prefHeight = Region.USE_COMPUTED_SIZE
        contentLabel.useMaxHeight = true
        contentLabel.maxWidth = maxWidth

        val contentPane = VBox(contentLabel)
        contentPane.prefHeight = Region.USE_COMPUTED_SIZE
        contentPane.useMaxHeight = true
        VBox.setVgrow(contentLabel, Priority.ALWAYS)

        alert.dialogPane.prefHeight = Region.USE_COMPUTED_SIZE
        alert.dialogPane.useMaxHeight = true
        alert.dialogPane.maxWidth = maxWidth
        alert.dialogPane.content = contentPane
    }


//    fun askForTextInput(questionText: CharSequence, alertTitleText: CharSequence?, defaultValue: CharSequence?, type: InputType, callback: (Boolean, String?) -> Unit) {
//        FXUtils.runOnUiThread { askForTextInputOnUiThread(questionText, alertTitleText, defaultValue, callback) }
//    }
//
//    private fun askForTextInputOnUiThread(questionText: CharSequence, alertTitleText: CharSequence?, defaultValue: CharSequence?, callback: (Boolean, String?) -> Unit) {
//        val dialog = TextInputDialog(defaultValue as? String)
//        dialog.headerText = null
//        dialog.title = alertTitleText as? String
//        dialog.contentText = questionText as? String
//
//        val result = dialog.showAndWait()
//
//        val enteredText = if(result.isPresent) result.get() else null
//        callback(result.isPresent, enteredText)
//    }

}