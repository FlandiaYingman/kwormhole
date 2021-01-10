package top.anagke.kwormhole.util

import javafx.event.EventTarget
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.layout.Region
import tornadofx.*

var Region.squareSize: Double
    get() {
        return prefHeight
    }
    set(value) {
        prefHeight = value
        prefWidth = value
        maxHeight = value
        maxWidth = value
        minHeight = value
        minWidth = value
    }

fun EventTarget.iconButton(url: String = "", size: Double, op: Button.() -> Unit = {}): Button {
    return button {
        graphic = imageview(Image(url, size, size, true, true))
        squareSize = size + 6
        op()
    }
}