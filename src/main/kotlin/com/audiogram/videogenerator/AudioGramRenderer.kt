package com.audiogram.videogenerator

import com.audiogram.videogenerator.utility.ShadowFactory
import com.audiogram.videogenerator.utility.TextFormat
import com.audiogram.videogenerator.utility.TextRenderer
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.jhlabs.image.GrayscaleFilter
import com.jhlabs.image.NoiseFilter
import com.twelvemonkeys.image.ConvolveWithEdgeOp
import com.xuggle.mediatool.IMediaWriter
import org.imgscalr.Scalr
import java.awt.*
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.TextAttribute
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.Kernel
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.*


class AudioGramRenderer(private val freqAmpData: ArrayList<FloatArray>, private val sigAmpData: ArrayList<FloatArray>, private val data: AudioGramData, private val writer: IMediaWriter) {

    fun start() {

        System.getProperties().setProperty("sun.java2d.opengl", "true")
        System.getProperties().setProperty("sun.java2d.accthreshold", "0")

        val points = freqAmpData.size
        var index = 1
        var progress = 0
        var currentPoint = 0

        val staticImage = createStaticRenderedImage(data)
        val bufferedImage = BufferedImage(data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt(), BufferedImage.TYPE_3BYTE_BGR)
        val g2d = bufferedImage.createGraphics().also { applyQualityRenderingHints(it) }

        var audioGramFrameGrabber: AudioGramFrameGrabber? = null
        val plotters = AudioGramPlotter().getPlotters(data)
        val effectsManager = EffectsManager(data)

        if (data.videoUrl != null)
            audioGramFrameGrabber = AudioGramFrameGrabber(data, 30)


        while (AudioGramTaskManager.taskIsRunning(data.id)) {

            Thread.sleep(0)
            if (currentPoint < points) {


                val trackProgress = (currentPoint / points.toDouble()) * 100
                if (trackProgress.roundToInt() != progress) {
                    println("task with id:${data.id} at $progress%")
                    progress = trackProgress.roundToInt()
                    AudioGramDBManager.updateProgress(data.id, progress)
                }

                g2d.clearRect(0, 0, data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt())
                if (data.videoUrl != null)
                    g2d.drawRenderedImage(audioGramFrameGrabber!!.grabNext(), null)


                g2d.drawRenderedImage(staticImage, null)

                if (data.meta.tracker.display!!)
                    renderTrackProgress(trackProgress, g2d, data.meta.tracker)

                effectsManager.render(freqAmpData, currentPoint, g2d)

                for (plotter in plotters) {
                    if (plotter.waveform.type == AudioGramWaveformType.SAD)
                        plotter.plot(sigAmpData, currentPoint, g2d)
                    else
                        plotter.plot(freqAmpData, currentPoint, g2d)
                }
                currentPoint++
                index++

                writer.encodeVideo(0, bufferedImage, (33333333.3 * index).roundToLong(), TimeUnit.NANOSECONDS)
                bufferedImage.flush()


            } else break
        }
        writer.close()
        writer.flush()
        Runtime.getRuntime().gc()
        System.gc()
        AudioGramDBManager.updateProgress(data.id, 100)
        AudioGramDBManager.updateStatus(data.id, "FINISHED")
        println("writer closed")

    }

    private fun renderTrackProgress(percent: Double, g2d: Graphics2D, meta: AudioTracker) {
        when (meta.type) {
            AudioGramAudioTrackerType.HORIZONTAL_BAR -> {
                val bar = RoundRectangle2D.Double(meta.posX!!, meta.posY!!, meta.length!! * (percent / 100), 10.0, 0.0, 0.0)
                val color = Color.decode(meta.fill)
                val opacity = (255 * (meta.opacity!! / 100.0)).toInt()

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
                    AudioGramImageEffect.MONOCHROME -> {
                        var effect = GrayscaleFilter()
                        val dest = effect.createCompatibleDestImage(source, ColorModel.getRGBdefault())
                        effect.filter(source, dest)
                        source = dest
                        dest.flush()
                    }
                    AudioGramImageEffect.JITTER -> {
                        var effect = NoiseFilter()
                        val dest = effect.createCompatibleDestImage(source, ColorModel.getRGBdefault())
                        effect.filter(source, dest)
                        source = dest
                        dest.flush()
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

        val g2d = source.createGraphics()
        val screen = Rectangle(0, 0, source.width, source.height)
        val fill = Color.decode(fill)
        val opacity = (255 * (50 / 100.0)).toInt()

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

        val width = img.width
        val height = img.height
        var diameter = 0
        var oval: Area?
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

        private val factory1 = ShadowFactory(5, 1f, Color.white)
        private val factory2 = ShadowFactory(5, 1f, Color.white)
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
                    val storage = StorageOptions.newBuilder().setProjectId("audiogram-292422").build().service
                    val blobs = storage.list("audiogram_resources", Storage.BlobListOption.prefix("fonts/"))

                    for (f in blobs.iterateAll()) {
                        if (f.name.substringAfter("/") != "") {
                            var file = File("${AudioGramFileManager.ROOT}/${f.name}")
                            file.createNewFile()
                            f.downloadTo(file.toPath())
                            graphicsEnvironment.registerFont(Font.createFont(Font.TRUETYPE_FONT, file))
                        }
                    }
                    loadedAppFont = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun drawCurve(points: java.util.ArrayList<Point>, path: GeneralPath, inBend: Int, outBend: Int) {

            /*control points*/
            var cpOneX: Double
            var cpOneY: Double
            var cpTwoX: Double
            var cpTwoY: Double

            path.moveTo(points[0].x, points[0].y)
            for (point in 1 until points.size) {

                val cpx = points[point].x
                val cpy = points[point].y


                if (point == 1) {

                    //sp will be the same as move coordinates

                    val spx = points[0].x
                    val spy = points[0].y

                    val npx = points[2].x
                    val npy = points[2].y

                    cpOneX = spx + (cpx - spx) / outBend
                    cpOneY = spy + (cpy - spy) / inBend

                    cpTwoX = cpx - (npx - spx) / outBend
                    cpTwoY = cpy - (npy - spy) / inBend

                    path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

                } else if (point > 1 && point <= points.size - 2) {

                    var pp0x: Double
                    var pp0y: Double

                    if (point == 2) {
                        pp0x = points[0].x
                        pp0y = points[0].y
                    } else {
                        pp0x = points[point - 2].x
                        pp0y = points[point - 2].y
                    }

                    val ppx = points[point - 1].x
                    val ppy = points[point - 1].y

                    val npx = points[point + 1].x
                    val npy = points[point + 1].y

                    cpOneX = ppx + (cpx - pp0x) / outBend
                    cpOneY = ppy + (cpy - pp0y) / inBend

                    cpTwoX = cpx - (npx - ppx) / outBend
                    cpTwoY = cpy - (npy - ppy) / inBend

                    path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

                } else {
                    val pp0x = points[point - 2].x
                    val pp0y = points[point - 2].y

                    val ppx = points[point - 1].x
                    val ppy = points[point - 1].y



                    cpOneX = ppx + (cpx - pp0x) / outBend
                    cpOneY = ppy + (cpy - pp0y) / inBend

                    cpTwoX = cpx - (cpx - ppx) / outBend
                    cpTwoY = cpy - (cpy - ppy) / inBend

                    path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

                }
            }
        }

        fun fillGradient(g2d: Graphics2D, shape: Shape, fill1: Color, fill2: Color, fill3: Color?, mode: Int) {

            var shapePath = GeneralPath(shape)
            var x = shapePath.bounds.x.toFloat()
            var y = shapePath.bounds.y.toFloat()
            var height = shapePath.bounds.height
            var width = shapePath.bounds.width

            var gradientPaint: GradientPaint = GradientPaint(0f, 0f, fill1, 0f, 0f, fill2)

            if (fill3 == null) {

                when (mode) {
                    1 -> gradientPaint = GradientPaint(x + width / 2, y, fill1, x + width / 2, y + height, fill2)
                    2 -> gradientPaint = GradientPaint(x, y, fill1, x + width, y + height, fill2)
                    3 -> gradientPaint = GradientPaint(x + width, y, fill1, x, y + height, fill2)

                }

            } else {
                when (mode) {
                    1 -> {
                        g2d.paint = GradientPaint(x + width / 2, y, fill1, x + width / 2, y + height * 0.66f, fill2)
                        g2d.fill(shape)
                        gradientPaint = GradientPaint(x + width / 2, y + height * 0.66f, Color(fill2.red, fill2.green, fill2.blue, 0), x + width / 2, y + height, fill3)
                    }
                    2 -> {
                        g2d.paint = GradientPaint(x, y, fill1, x + width, y + height * 0.66f, fill2)
                        g2d.fill(shape)
                        gradientPaint = GradientPaint(x + width, y + height * 0.66f, Color(fill2.red, fill2.green, fill2.blue, 0), x, y + height, fill3)
                    }
                    3 -> {
                        g2d.paint = GradientPaint(x + width, y, fill1, x, y + height * 0.66f, fill2)
                        g2d.fill(shape)
                        gradientPaint = GradientPaint(x, y + height * 0.66f, Color(fill2.red, fill2.green, fill2.blue, 0), x + width, y + height, fill3)
                    }
                }
            }

            g2d.paint = gradientPaint
            g2d.fill(shape)
        }

        fun generateGlow(shape: Shape, g2: Graphics2D, color: Color, size: Int, opacity: Float) {


            val x = shape.bounds.x.toDouble()
            val y = shape.bounds.y.toDouble()

            val shape2 = GeneralPath(shape)
            shape2.transform(AffineTransform.getTranslateInstance(-x, -y))

            val buffer = BufferedImage(shape2.bounds.width, shape2.bounds.height, BufferedImage.TYPE_INT_ARGB)
            val graphics2D = buffer.createGraphics()

            graphics2D.color = color
            graphics2D.stroke = BasicStroke(5f)
            graphics2D.fill(shape2)

            factory1.color = Color.white
            factory1.opacity = opacity
            factory1.size = size

            factory2.color = color
            factory2.size = size

            val glowLayer = factory2.createShadow(AudioGramRenderer.factory1.createShadow(buffer))
            val deltaX = x - (glowLayer.width - shape.bounds.width) / 2.0
            val deltaY = y - (glowLayer.height - shape.bounds.height) / 2.0

            g2.drawImage(glowLayer, AffineTransform.getTranslateInstance(deltaX, deltaY), null)

        }

    }

}
