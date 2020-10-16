package com.audiogram.videogenerator

import com.audiogram.videogenerator.utility.TextFormat
import com.audiogram.videogenerator.utility.TextRenderer
import com.twelvemonkeys.image.ConvolveWithEdgeOp
import com.xuggle.mediatool.IMediaWriter
import org.imgscalr.Scalr
import java.awt.*
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.Kernel
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.*


class AudioGramRenderer {

    fun start(data: AudioGramData, writer: IMediaWriter, ampData: ArrayList<FloatArray>) {

        System.getProperties().setProperty("sun.java2d.opengl", "true")
        System.getProperties().setProperty("sun.java2d.accthreshold", "0")

        var points = ampData.size
        var index = 1
        var progress = 0
        var currentPoint = 0

        val staticImage = createStaticRenderedImage(data)
        val bufferedImage = BufferedImage(data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt(), BufferedImage.TYPE_3BYTE_BGR)
        val g2d = bufferedImage.createGraphics()
        applyQualityRenderingHints(g2d)

        var audioGramFrameGrabber: AudioGramFrameGrabber? = null
        var plotter = AudioGramPlotter().getPlotter(data)
        var effectsManager = EffectsManager(data)

        if (data.videoUrl != null)
            audioGramFrameGrabber = AudioGramFrameGrabber(data, 30)


        while (AudioGramTaskManager.taskIsRunning(data.id)) {

            Thread.sleep(0)
            if (currentPoint < points) {

                g2d.clearRect(0, 0, data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt())
                val trackProgress = (currentPoint / points.toDouble()) * 100

                if (trackProgress.roundToInt() != progress) {
                    println("task with id:${data.id} at $progress%")
                    progress = trackProgress.roundToInt()
                    // AudioGramDBManager.updateProgress(data.id, progress)
                }

                if (data.videoUrl != null) {
                    g2d.drawRenderedImage(audioGramFrameGrabber!!.grabNext(), null)
                }

                g2d.drawRenderedImage(staticImage, null)

                if (data.meta.tracker.display!!) {
                    renderTrackProgress(trackProgress, g2d, data.meta.tracker)
                }

                effectsManager.render(ampData, currentPoint, g2d)
                plotter(data, ampData, currentPoint, g2d)

                currentPoint++
                index++

                writer.encodeVideo(0, bufferedImage, (33333333.3 * index).roundToLong(), TimeUnit.NANOSECONDS)
                bufferedImage.flush()
                //writer.flush()

            } else break
        }
        writer.close()
        writer.flush()
        Runtime.getRuntime().gc()
        System.gc()
        // AudioGramDBManager.updateProgress(data.id, 100)
        //  AudioGramDBManager.updateStatus(data.id, "FINISHED")
        println("writer closed")

    }

    private fun renderTrackProgress(percent: Double, g2d: Graphics2D, meta: AudioTracker) {
        when (meta.type) {
            AudioGramAudioTrackerType.HORIZONTAL_BAR -> {
                var bar = RoundRectangle2D.Double(meta.posX!!, meta.posY!!, meta.length!! * (percent / 100), 10.0, 0.0, 0.0)
                var color = Color.decode(meta.fill)
                var opacity = (255 * (meta.opacity!! / 100.0)).toInt()

                g2d.color = Color(color.red, color.green, color.blue, opacity)
                g2d.fill(bar)
            }
        }
    }

    private fun createStaticRenderedImage(data: AudioGramData): BufferedImage {

        val sortedImages = data.images.sortedWith(compareBy { it.zIndex })
        val sortedTexts = data.texts.sortedWith(compareBy { it.zIndex })
        val sortedShapes = data.shapes.sortedWith(compareBy { it.zIndex })

        val bufferedImage = BufferedImage(data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt(), BufferedImage.TYPE_INT_ARGB)
        val g2d = bufferedImage.createGraphics()
        applyQualityRenderingHints(g2d)

        val shapeRenderer = AudioGramShapeRenderer()
        for (image in sortedImages) {

            var source = ImageIO.read(AudioGramFileManager.getResource(image.url))
            if (image.width != 0.0 || image.height != 0.0) {
                source = Scalr.resize(source, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, image.width!!.toInt(), image.height!!.toInt(), Scalr.OP_ANTIALIAS)
            }

            if (image.imageEffect != AudioGramImageEffect.NONE) {
                when (image.imageEffect) {
                    AudioGramImageEffect.BLUR -> {
                        source = blurImage(source)
                    }
                }
            }
            if (image.filter != AudioGramFilterType.NONE) {
                when (image.filter) {
                    AudioGramFilterType.SCREEN -> {
                        screenImage(source, image.filterFill!!)
                    }

                }
            }
            when (image.align) {
                AudioGramImageAlign.CENTER -> image.posX = (data.meta.video.width!! - source.width) / 2
                AudioGramImageAlign.RIGHT -> image.posX = (data.meta.video.width!! - source.width) * 3 / 4
                AudioGramImageAlign.LEFT -> image.posX = (data.meta.video.width!! - source.width) / 4
            }
            if (image.mask != AudioGramMaskType.NONE) {
                when (image.mask) {
                    AudioGramMaskType.CIRCLE -> {
                        source = maskImageToCircle(source)
                    }
                    AudioGramMaskType.SQUARE -> {

                    }
                }
            }
            if (image.transform != null && image.transform != "none") {
                val degree = image.transform!!.substringAfterLast(":").trim().toDouble()
                source = rotateImageByDegrees(source, degree)
            }

            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (image.opacity!! / 100f))
            g2d.drawImage(source, null, image.posX!!.toInt(), image.posY!!.toInt())
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f)

            if (image.frame != AudioGramFrameType.NONE && image.mask != AudioGramMaskType.CIRCLE) {
                val frameColor = Color.decode(image.frameColor)
                var frameWidth = 0f
                when (image.frame) {
                    AudioGramFrameType.THIN -> {
                        frameWidth = 2f
                    }
                    AudioGramFrameType.NORMAL -> {
                        frameWidth = 5f
                    }
                    AudioGramFrameType.SOLID -> {
                        frameWidth = 10f
                    }
                }
                g2d.color = frameColor
                g2d.stroke = BasicStroke(frameWidth)
                g2d.drawRect(image.posX!!.roundToInt() + frameWidth.toInt() / 2, image.posY!!.roundToInt(), source.width, source.height)
                println(image.frame)
            }
        }
        for (shape in sortedShapes) {
            if (shape.shapeType != AudioGramShapeType.SVG)
                shapeRenderer.drawBasicShape(shape, g2d)
            else
                shapeRenderer.drawVectorShape(shape, g2d)
        }

        for (text in sortedTexts) {
            val attributes = HashMap<TextAttribute, Any>()
            attributes[TextAttribute.POSTURE] = if (text.fontStyle == AudioGramFontStyle.ITALIC) TextAttribute.POSTURE_OBLIQUE else TextAttribute.POSTURE_REGULAR
            attributes[TextAttribute.SIZE] = text.fontSize!!
            when (text.fontWeight) {
                AudioGramFontWeight.BOLD -> attributes[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_BOLD
                AudioGramFontWeight.NORMAL -> attributes[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_REGULAR
                AudioGramFontWeight.THIN -> attributes[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_LIGHT
            }
            when (text.spacing) {
                AudioGramSpacing.LOOSE -> attributes[TextAttribute.TRACKING] = 0.1
                AudioGramSpacing.NORMAL -> attributes[TextAttribute.TRACKING] = 0.0
                AudioGramSpacing.TIGHT -> attributes[TextAttribute.TRACKING] = -0.1
            }
            var color = Color.decode(text.color)
            TextRenderer.drawString(
                    g2d,
                    text.value,
                    Font.decode(text.font!!).deriveFont(attributes),
                    Color(color.red, color.green, color.blue, (255 * (text.opacity!! / 100.0)).toInt()),
                    Rectangle(text.posX!!, text.posY!!, text.width!!, 100),
                    text.align,
                    TextFormat.FIRST_LINE_VISIBLE
            )
        }
        return bufferedImage
    }

    private fun screenImage(source: BufferedImage, fill: String) {

        var g2d = source.createGraphics()
        var screen = Rectangle(0, 0, source.width, source.height)
        var fill = Color.decode(fill)
        var opacity = (255 * (50 / 100.0)).toInt()

        g2d.color = Color(fill.red, fill.green, fill.blue, opacity)
        g2d.fill(screen)
    }

    private fun rotateImageByDegrees(img: BufferedImage, angle: Double): BufferedImage {

        val rads = Math.toRadians(angle)
        val sin = abs(sin(rads))
        val cos = abs(cos(rads))
        val w = img.width
        val h = img.height
        val newWidth = floor(w * cos + h * sin).toInt()
        val newHeight = floor(h * cos + w * sin).toInt()

        val rotated = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)

        val at = AffineTransform()
        val x = w / 2
        val y = h / 2
        at.rotate(rads, x.toDouble(), y.toDouble())

        val g2d = rotated.createGraphics()
        applyQualityRenderingHints(g2d)
        g2d.transform = at
        g2d.drawRenderedImage(img, null)
        g2d.dispose()

        return rotated
    }

    private fun blurFilter(radius: Int, horizontal: Boolean): ConvolveWithEdgeOp {
        if (radius < 1) {
            throw IllegalArgumentException("Radius must be >= 1")
        }

        val size = radius * 2 + 1
        val data = FloatArray(size)

        val sigma = radius / 3.0f
        val twoSigmaSquare = 2.0f * sigma * sigma
        val sigmaRoot = sqrt(twoSigmaSquare * Math.PI).toFloat()
        var total = 0.0f

        for (i in -radius..radius) {
            val distance = (i * i).toFloat()
            val index = i + radius
            data[index] = exp((-distance / twoSigmaSquare).toDouble()).toFloat() / sigmaRoot
            total += data[index]
        }

        for (i in data.indices) {
            data[i] /= total
        }

        var kernel: Kernel?
        kernel = if (horizontal) {
            Kernel(size, 1, data)
        } else {
            Kernel(1, size, data)
        }
        return ConvolveWithEdgeOp(kernel, ConvolveWithEdgeOp.EDGE_REFLECT, null)
    }

    private fun blurImage(bufferedImage: BufferedImage): BufferedImage {
        var bi = blurFilter(200, true).filter(bufferedImage, null)
        bi = blurFilter(200, false).filter(bi, null)
        return bi
    }

    private fun maskImageToCircle(img: BufferedImage): BufferedImage {

        var width = img.width
        var height = img.height
        var diameter = 0
        var oval: Area? = null
        diameter = if (width > height || width == height) {
            height
        } else {
            width
        }
        oval = if (width > height) {
            Area(Ellipse2D.Double((width - diameter.toDouble()) / 2, 0.0, diameter.toDouble(), diameter.toDouble()))
        } else {
            Area(Ellipse2D.Double((width - diameter.toDouble()) / 2, 0.0, diameter.toDouble(), diameter.toDouble()))
        }
        var masked = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = masked.createGraphics()
        applyQualityRenderingHints(g2d)
        g2d.clip(oval)
        g2d.drawRenderedImage(img, null)
        g2d.dispose()
        return masked
    }

    companion object {

        private var loadedAppFont = false
        fun applyQualityRenderingHints(g2d: Graphics2D) {
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        }

        fun loadApplicationFonts() {
            if (!loadedAppFont) {
                try {
                    val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    var fontDir = File("src/main/kotlin/com/audiogram/videogenerator/resources/fonts")
                    for (fontFile in fontDir.listFiles()!!) {
                        val font = Font.createFont(Font.TRUETYPE_FONT, fontFile)
                        graphicsEnvironment.registerFont(font)
                    }
                    loadedAppFont = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

}
