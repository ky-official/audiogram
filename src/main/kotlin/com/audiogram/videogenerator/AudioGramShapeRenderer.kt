package com.audiogram.videogenerator

import com.kitfox.svg.SVGUniverse
import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets


class AudioGramShapeRenderer {

    private var universe = SVGUniverse()
    private var shapeCount = 0

    fun drawBasicShape(shape: Shape, g2d: Graphics2D) {

        if (shape.shapeType != AudioGramShapeType.SVG) {
            when (shape.shapeType) {
                AudioGramShapeType.BOX -> {
                    var box = Rectangle(shape.posX!!, shape.posY!!, shape.width!!, shape.height!!)
                    if (shape.outline!!) {
                        g2d.color = Color.decode(shape.outlineColor)
                        g2d.stroke = BasicStroke(shape.outlineWidth!!.toFloat())
                        g2d.draw(box)


                        val color = Color.decode(shape.fill)
                        g2d.color = Color(color.red, color.green, color.blue, (255 * (shape.opacity!! / 100.0)).toInt())
                        g2d.fill(box)
                        g2d.stroke = BasicStroke(0f)
                        shapeCount++
                    } else {
                        val color = Color.decode(shape.fill)
                        g2d.color = Color(color.red, color.green, color.blue, (255 * (shape.opacity!! / 100.0)).toInt())
                        g2d.fill(box)
                        shapeCount++
                    }
                }
                AudioGramShapeType.CIRCLE -> {
                    val ellipse = Ellipse2D.Double(shape.posX!!.toDouble(), shape.posY!!.toDouble(), shape.width!!.toDouble(), shape.height!!.toDouble())
                    if (shape.outline!!) {
                        g2d.color = Color.decode(shape.outlineColor)
                        g2d.stroke = BasicStroke(shape.outlineWidth!!.toFloat())
                        g2d.draw(ellipse)
                        val color = Color.decode(shape.fill)
                        g2d.color = Color(color.red, color.green, color.blue, (255 * (shape.opacity!! / 100.0)).toInt())
                        g2d.fill(ellipse)
                        g2d.stroke = BasicStroke(0f)
                        shapeCount++
                    } else {
                        val color = Color.decode(shape.fill)
                        g2d.color = Color(color.red, color.green, color.blue, (255 * (shape.opacity!! / 100.0)).toInt())
                        g2d.fill(ellipse)
                        shapeCount++
                    }
                }
                AudioGramShapeType.LINE -> {
                    val x1 = shape.posX!!.toDouble()
                    val x2 = shape.posX!!.toDouble() + shape.width!!.toDouble()
                    val y1 = shape.posY!!.toDouble()
                    val y2 = shape.posY!!.toDouble()

                    val line = Line2D.Double(x1, y1, x2, y2)
                    val color = Color.decode(shape.fill)
                    g2d.color = Color(color.red, color.green, color.blue, (255 * (shape.opacity!! / 100.0)).toInt())
                    g2d.stroke = BasicStroke(shape.outlineWidth!!.toFloat())
                    g2d.draw(line)
                    g2d.stroke = BasicStroke(0f)
                    shapeCount++
                }
            }
        } else {
            println("Invalid basic shape passed to the render")
        }
    }


    fun drawVectorShape(shape: Shape, g2d: Graphics2D) {
        val diagram = universe.getDiagram(universe.loadSVG(shape.svg!!.byteInputStream(StandardCharsets.UTF_8), "shape_${++shapeCount}"))

        val svgBuffer = BufferedImage(diagram.width.toInt(), diagram.height.toInt(), BufferedImage.TYPE_INT_ARGB)
        var g2dSvgBuffer = svgBuffer.createGraphics()
        AudioGramRenderer.applyQualityRenderingHints(g2dSvgBuffer)
        g2dSvgBuffer.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (shape.opacity!! / 100f))

        diagram.render(g2dSvgBuffer)
        g2d.drawImage(svgBuffer, null, shape.posX!!, shape.posY!!)
    }

    override fun toString(): String {
        return "${super.toString()} :: Total number of shapes rendered: $shapeCount"
    }
}