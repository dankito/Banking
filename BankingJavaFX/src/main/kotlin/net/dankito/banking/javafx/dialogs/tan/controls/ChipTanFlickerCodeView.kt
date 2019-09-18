package net.dankito.banking.javafx.dialogs.tan.controls

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Pos
import javafx.scene.paint.Color
import net.dankito.utils.javafx.ui.extensions.fixedHeight
import net.dankito.utils.javafx.ui.extensions.fixedWidth
import net.dankito.utils.javafx.ui.extensions.setBackgroundToColor
import org.kapott.hbci.manager.FlickerRenderer
import tornadofx.*


class ChipTanFlickerCodeView(private val flickerCodeData: String): View() {

    companion object {
        private const val ChangeSizeStripeHeightChange = 7.0
        private const val ChangeSizeStripeWidthChange = 2.0
        private const val ChangeSizeSpaceBetweenStripesChange = 1.0

        const val MinFlickerCodeViewWidth = 124.0 + ChangeSizeStripeWidthChange + ChangeSizeSpaceBetweenStripesChange // below space between stripes aren't visible anymore
        const val MaxFlickerCodeViewWidth = 1000.0 // what is a senseful value?

        private const val IconWidth = 26.0
        private const val IconHeight = 26.0
    }


    private val flickerCodeLeftRightMargin = SimpleDoubleProperty(31.0)

    private val stripeHeight = SimpleDoubleProperty(127.0)
    private val stripeWidth = SimpleDoubleProperty(42.0)
    private val spaceBetweenStripes = SimpleDoubleProperty(10.0)

    private val flickerCodeViewWidth = SimpleDoubleProperty()

    private val stripe1 = SimpleBooleanProperty()
    private val stripe2 = SimpleBooleanProperty()
    private val stripe3 = SimpleBooleanProperty()
    private val stripe4 = SimpleBooleanProperty()
    private val stripe5 = SimpleBooleanProperty()

    private val isMinSizeReached = SimpleBooleanProperty(false)
    private val isMaxSizeReached = SimpleBooleanProperty(false)

    private val renderer = object : FlickerRenderer(flickerCodeData) {
        override fun paint(showStripe1: Boolean, showStripe2: Boolean, showStripe3: Boolean, showStripe4: Boolean, showStripe5: Boolean) {
            super.paint(showStripe1, showStripe2, showStripe3, showStripe4, showStripe5)

            runLater {
                paintFlickerCode(showStripe1, showStripe2, showStripe3, showStripe4, showStripe5)
            }
        }
    }
    
    
    init {
        flickerCodeViewWidth.bind(stripeWidth.add(spaceBetweenStripes).multiply(4).add(stripeWidth).add(flickerCodeLeftRightMargin).add(flickerCodeLeftRightMargin))

        renderer.setFrequency(20)
    }


    override val root = vbox {
        alignment = Pos.CENTER
        usePrefHeight = true

        prefWidthProperty().bind(flickerCodeViewWidth)
        fixedWidth = flickerCodeViewWidth.value

        flickerCodeViewWidth.addListener { _, _, newValue ->
            fixedWidth = newValue.toDouble()
        }


        hbox {
            fixedHeight = IconHeight + 6.0

            alignment = Pos.CENTER

            label(messages["enter.tan.dialog.size.label"])

            button("+") {
                prefHeight = IconHeight
                prefWidth = IconWidth

                disableWhen(isMaxSizeReached)

                action { increaseSize() }

                hboxConstraints {
                    marginLeft = 6.0
                    marginRight = 4.0
                }
            }

            button("-") {
                prefHeight = IconHeight
                prefWidth = IconWidth

                disableWhen(isMinSizeReached)

                action { decreaseSize() }
            }

            // TODO: add speed (frequency) controls
        }

        vbox {
            setBackgroundToColor(Color.BLACK)

            vbox {
                anchorpane {

                    add(TanGeneratorMarkerView().apply {
                        setLeftMarkerPosition(this)

                        stripeWidth.addListener { _, _, _ -> setLeftMarkerPosition(this) }
                    })

                    add(TanGeneratorMarkerView().apply {
                        setRightMarkerPosition(this)

                        stripeWidth.addListener { _, _, _ -> setRightMarkerPosition(this) }
                    })

                    vboxConstraints {
                        marginBottom = 6.0
                    }
                }

                hbox {
                    minHeight = 150.0

                    add(ChipTanFlickerCodeStripeView(stripe1, stripeWidth, stripeHeight, spaceBetweenStripes))
                    add(ChipTanFlickerCodeStripeView(stripe2, stripeWidth, stripeHeight, spaceBetweenStripes))
                    add(ChipTanFlickerCodeStripeView(stripe3, stripeWidth, stripeHeight, spaceBetweenStripes))
                    add(ChipTanFlickerCodeStripeView(stripe4, stripeWidth, stripeHeight, spaceBetweenStripes))
                    add(ChipTanFlickerCodeStripeView(stripe5, stripeWidth, stripeHeight, SimpleDoubleProperty(0.0)))
                }

                vboxConstraints {
                    marginTopBottom(24.0)
                    marginLeftRight(flickerCodeLeftRightMargin.value)
                }
            }
        }

        renderer.start()
    }


    private fun paintFlickerCode(showStripe1: Boolean, showStripe2: Boolean, showStripe3: Boolean, showStripe4: Boolean, showStripe5: Boolean) {
        stripe1.set(showStripe1)
        stripe2.set(showStripe2)
        stripe3.set(showStripe3)
        stripe4.set(showStripe4)
        stripe5.set(showStripe5)
    }

    private fun setLeftMarkerPosition(component: UIComponent) {
        component.root.anchorpaneConstraints {
            leftAnchor = (stripeWidth.value / 2)
        }
    }

    private fun setRightMarkerPosition(component: UIComponent) {
        component.root.anchorpaneConstraints {
            rightAnchor = (stripeWidth.value / 2)
        }
    }

    private fun increaseSize() {
        if (isMaxSizeReached.value == false) {
            stripeHeight.set(stripeHeight.get() + ChangeSizeStripeHeightChange)
            stripeWidth.set(stripeWidth.get() + ChangeSizeStripeWidthChange)
            spaceBetweenStripes.set(spaceBetweenStripes.get() + ChangeSizeSpaceBetweenStripesChange)
        }

        updateMinAndMaxSizeReached()
    }

    private fun decreaseSize() {
        if (isMinSizeReached.value == false) {
            stripeHeight.set(stripeHeight.get() - ChangeSizeStripeHeightChange)
            stripeWidth.set(stripeWidth.get() - ChangeSizeStripeWidthChange)
            spaceBetweenStripes.set(spaceBetweenStripes.get() - ChangeSizeSpaceBetweenStripesChange)
        }

        updateMinAndMaxSizeReached()
    }

    private fun updateMinAndMaxSizeReached() {
        val flickerCodeWidth = stripeWidth.value * 5 + spaceBetweenStripes.value * 4

        isMinSizeReached.set(flickerCodeWidth < MinFlickerCodeViewWidth)
        isMaxSizeReached.set(flickerCodeWidth > MaxFlickerCodeViewWidth)
    }

}