package net.dankito.banking.javafx.util

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Region
import javafx.scene.paint.Color


object FXUtils {

    fun runOnUiThread(runnable: () -> Unit) {
        if(Platform.isFxApplicationThread()) {
            runnable()
        }
        else {
            Platform.runLater(runnable)
        }
    }

    fun ensureNodeOnlyUsesSpaceIfVisible(node: Node) {
        node.managedProperty().bind(node.visibleProperty())
    }

    fun setBackgroundToColor(region: Region, color: Color) {
        region.background = Background(BackgroundFill(color, CornerRadii(0.0), Insets(0.0)))
    }

}
