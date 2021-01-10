package top.anagke.kwormhole.view

import com.google.gson.Gson
import javafx.beans.InvalidationListener
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BackgroundSize
import javafx.scene.paint.Color
import javafx.scene.text.FontPosture
import javafx.util.Callback
import top.anagke.kwormhole.util.fromJson
import top.anagke.kwormhole.util.iconButton
import top.anagke.kwormhole.util.listFromJson
import top.anagke.kwormhole.util.squareSize
import tornadofx.*
import java.io.File
import javax.swing.text.StyledEditorKit

class KwormholeView : View("Kwormhole") {


    private lateinit var listView: ListView<String>

    private val syncDirs = observableListOf<String>().apply {
        addAll(Gson().listFromJson<String>(app.config.string("syncDirs")))
        addListener(InvalidationListener {
            app.config.set("syncDirs" to Gson().toJson(this@apply))
            app.config.save()
        })
    }

    override val root = borderpane {
        setPrefSize(300.0, 600.0)
        style {
            fontFamily = "Noto Sans"
        }
        center = listview(syncDirs) {
            listView = this
            placeholder = ListViewPlaceholder()
            cellFormat {
                graphic = cache {
                    form {
                        label(File(it).name) {
                            style {
                                fontSize = 22.px
                            }
                        }
                        label(File(it).canonicalPath) {
                            style {
                                fontSize = 12.px
                                fontStyle = FontPosture.ITALIC
                                textFill = Color.GRAY
                            }
                        }
                    }
                }
            }
        }

        top = hbox {
            iconButton("/icon/folder-add.png", 24.0) {
                action {
                    val dir = chooseDirectory()
                    if (dir != null) {
                        syncDirs += dir.canonicalPath
                    }
                }
            }
            iconButton("/icon/delete.png", 24.0) {
                action {
                    if (listView.selectionModel.selectedIndices.isNotEmpty()) {
                        val confirm = confirmation("Do you really want to delete the selected directories?")
                        if (confirm.result.buttonData == ButtonBar.ButtonData.OK_DONE) {
                            listView.selectionModel.selectedIndices.forEach { syncDirs.removeAt(it) }
                        }
                    }
                }
            }
            iconButton("/icon/filesync.png", 24.0) {

            }
        }
    }

    init {
        setStageIcon(Image("/icon/kwormhole.png"))
    }

}
