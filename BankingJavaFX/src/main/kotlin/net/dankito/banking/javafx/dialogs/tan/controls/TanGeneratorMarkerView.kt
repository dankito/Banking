package net.dankito.banking.javafx.dialogs.tan.controls

import javafx.scene.paint.Color
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.fixedWidth
import net.dankito.utils.javafx.ui.extensions.setBackgroundToColor
import tornadofx.*


class TanGeneratorMarkerView: View() {

    override val root = label {
        fixedHeight = 20.0

        fixedWidth = 8.0

        setBackgroundToColor(Color.GRAY)
    }

}