package main


import javafx.application.Application
import javafx.beans.InvalidationListener
import javafx.embed.swing.SwingFXUtils
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.WritableImage
import javafx.scene.layout.*
import javafx.stage.FileChooser
import javafx.stage.Popup
import javafx.stage.Stage
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.text.html.ImageView
import kotlin.math.abs
import javafx.stage.FileChooser.ExtensionFilter
import java.awt.*
import java.io.InputStream
import java.util.*
import javax.swing.plaf.basic.BasicOptionPaneUI

public class PopupMessage {
    constructor(f:String?){
        file = f
    }
    var file : String? = null


    private var isShow : Boolean = false

    fun isPopupShowing() : Boolean {
        return isShow
    }
    fun Show() {
        isShow = true
        val stage = Stage()
        val popup = Popup()
        stage.height = 100.0
        stage.width = 400.0
        val label = Label("Screenshot is saved to $file")
        //popup.content.add(label)
        val button = Button("Ok")

        val popupRoot = HBox()

        popupRoot.style = "-fx-background-color: lightgrey"
        popupRoot.children.addAll(label, button)

        popup.content.add(popupRoot)
        stage.show()
        popup.show(stage)
        button.onAction = EventHandler {
            popup.hide()
            stage.hide()
            isShow = false
        }
    }
}


class Screenshot : Application() {



    var brushSize = 18.0

    override fun start(primaryStage: Stage) {

       val slider = Slider()

       slider.min = 100.0
       slider.max = 1000.0
       var wait : Double = slider.value
        val takeScreenshot = Button("Screenshot")
        val chooseFile = Button("ChooseFile")
        val cropImage = Button("Crop")
        val fileChooser = FileChooser()
        val save = Button("Save")
        val cancel = Button("Cancel")
        val paint = Button("Paint")

       val menu  = MenuBar()

       val ch = CheckBox("hide application")
       ch.isSelected = true

       menu.add(Menu("Select File"))
       menu.add(Menu("Save"))
       menu.add(Menu("Close application"))

        var image : Image = Image(InputStream.nullInputStream())

        val panel : HBox = HBox(slider, takeScreenshot, chooseFile, cropImage, save, cancel, paint, ch)
        val root : VBox = VBox(panel)
       var isMousePressed : Boolean = false
        var isMouseMoved : Boolean = false
        slider.maxWidth = 500.0
        val canvas = Canvas()
        val cropRect = Canvas()
        val g = canvas.graphicsContext2D
        val g2 = cropRect.graphicsContext2D
        val pane = Pane()
        val scene = Scene(root, 1500.0, 800.0)
        var mouseX : Double = 0.0
        var mouseY : Double = 0.0

        canvas.height = scene.height - 100
        canvas.width = scene.width - 100
        cropRect.width = canvas.width
        cropRect.height = canvas.height

        var x: Int = 0
        var y: Int = 0
        var width : Int = canvas.width.toInt()
        var height : Int = canvas.height.toInt()

        cropRect.setOnMousePressed {
            println("mousePressed")
            g2.clearRect(0.0, 0.0, canvas.width, canvas.height)
            x = (it.x).toInt()
            y = (it.y).toInt()
            isMousePressed = true
            mouseX = it.x
            mouseY = it.y
        }

        cropRect.setOnMouseReleased {
            println("mouseUp")
            isMousePressed = false
        }

        var isToPaint : Boolean = false
        var isToCrop : Boolean = false
        cropRect.setOnMouseDragged {
            println("mouseMoved")
            if (isToCrop) {
                g2.clearRect(0.0, 0.0, canvas.width, canvas.height)
                x = if (it.x - mouseX < 0) (it.x).toInt() else mouseX.toInt()
                y = if (it.y - mouseY < 0) (it.y).toInt() else mouseY.toInt()
                width = abs(it.x - mouseX).toInt()
                height = abs(it.y - mouseY).toInt()
                g2.strokeRect(x.toDouble(), y.toDouble(), abs(it.x - mouseX), abs(it.y - mouseY))
            }
            if (isToPaint) {
                g.strokeLine(x.toDouble(), y.toDouble(), it.x, it.y)
                x = (it.x).toInt()
                y = (it.y).toInt()
            }
        }

        var wi : WritableImage

        val stackpane = StackPane(canvas, cropRect)
       // stackpane.style = "-fx-background-color: red"
        pane.children.add(stackpane)
        root.children.add(pane)
        primaryStage.scene = scene
        primaryStage.title = "Paint"
        primaryStage.scene = scene
        primaryStage.show()
        primaryStage.x = 0.0
        primaryStage.y = 0.0

        takeScreenshot.onAction = EventHandler {

                println("Take screenshot")
                if (ch.isSelected) {
                    primaryStage.hide()
                }
                Thread.sleep(wait.toLong())
                val bufImage : BufferedImage? =  saveAsPng()
                image = SwingFXUtils.toFXImage(bufImage, null)
                g.drawImage(image, 0.0, 0.0,  canvas.width, canvas.height)
                if (ch.isSelected) {
                    primaryStage.show()
                }

        }


        cropImage.onAction = EventHandler {
            isToCrop = true
            isToPaint = false
        }
        paint.onAction = EventHandler {
            isToPaint = true
            isToCrop = false
        }

        save.onAction = EventHandler {
            fileChooser.extensionFilters.addAll(
                FileChooser.ExtensionFilter("Image Files", "*.png")
            )
            fileChooser.title = "Save Image"
            val file = fileChooser.showSaveDialog(primaryStage)
            if (file != null) {
                fileChooser.initialDirectory = file.parentFile
                if (isToCrop) {
                    g2.clearRect(0.0, 0.0, canvas.width, canvas.height)
                }

                val pr : PixelReader = image.pixelReader
                wi = WritableImage(pr, 0, 0, width, height)
                if (isToPaint) {
                    canvas.snapshot(SnapshotParameters(), wi)
                }
                val renderedImage : RenderedImage = SwingFXUtils.fromFXImage(wi, null)
                ImageIO.write(renderedImage, "png", file)
                image = Image("file:$file")
                if (isToCrop) {
                    g.clearRect(0.0, 0.0, canvas.width, canvas.height)
                    g.drawImage(image, 0.0, 0.0,  image.width, image.height)
                }
                isToCrop = false
                isToPaint = false
                val popup = PopupMessage(file.toString())
                popup.Show()
            }
        }

       slider.valueProperty().addListener(InvalidationListener {
           if (slider.isPressed) {
                val newWait = slider.value
                wait = newWait
           }
       })

        cancel.onAction = EventHandler {
            g2.clearRect(0.0, 0.0, canvas.width, canvas.height)
            g.clearRect(0.0, 0.0, canvas.width, canvas.height)
            g.drawImage(image, 0.0, 0.0, canvas.width, canvas.height)
            isToCrop = false
            isToPaint = false
        }

        chooseFile.onAction = EventHandler {
            try {
                val file : File = fileChooser.showOpenDialog(primaryStage)
                image = Image(file.toURI().toString())
                g.drawImage(image, 0.0, 0.0,  canvas.width, canvas.height)
            }
            catch(e : IllegalStateException){

            }

        }
    }

    fun saveAsPng() : BufferedImage? {
        try {
            val robot = Robot()

            val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            val screenFullImage = robot.createScreenCapture(screenRect)

           // ImageIO.write(screenFullImage, "jpg", File(fileName))

            print("Done")
            return screenFullImage
        } catch (ex: IOException) {
            print(ex)
            return null
        }

    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Screenshot::class.java)
        }
    }
}