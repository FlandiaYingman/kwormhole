package top.anagke.kwormhole.view

import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.util.Callback

class ListViewPlaceholder : ListView<Any?>() {
    init {
        style = "-fx-background-insets: 0; -fx-padding: 0;"
        items.add(null)
        cellFactory = Callback<ListView<Any?>?, ListCell<Any?>?> {
            object : ListCell<Any?>() {
                override fun updateItem(item: Any?, empty: Boolean) {
                    super.updateItem(item, true)
                }
            }
        }
    }
}