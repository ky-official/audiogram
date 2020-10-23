package com.audiogram.videogenerator

import com.audiogram.videogenerator.AudioGramRenderer.Companion.drawCurve
import com.audiogram.videogenerator.AudioGramRenderer.Companion.fillGradient
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.*
import kotlin.math.cos
import kotlin.math.sin


class AudioGramPlotter {

    var plotters = ArrayList<Plotter>()
    fun getPlotters(audioGramData: AudioGramData): ArrayList<Plotter> {

        for (waveform in audioGramData.waveforms) {
            when (waveform.design) {
                AudioGramWaveformDesign.SPECTROGRAM -> plotters.add(SigAmpPlotterSpectrogram(waveform, audioGramData))
                AudioGramWaveformDesign.ARC_REACTOR -> plotters.add(SigAmpPlotterArcReactor(waveform, audioGramData))
                AudioGramWaveformDesign.RAIN_BARS -> plotters.add(SigAmpPlotterRainBars(waveform, audioGramData))
                AudioGramWaveformDesign.SPECTRAL_FLUX -> plotters.add(FreqAmpPlotterSpectralFlux(waveform, audioGramData))
                AudioGramWaveformDesign.PELICAN_GRID -> plotters.add(FreqAmpPlotterPelicanGrid(waveform, audioGramData))
                AudioGramWaveformDesign.MORPH_STACK -> plotters.add(FreqAmpPlotterMorphStack(waveform, audioGramData))
                AudioGramWaveformDesign.DEFAULT -> plotters.add(SigAmpPlotterSpectrogram(waveform, audioGramData))
            }
        }
        return plotters
    }

    internal class SigAmpPlotterSpectrogram(waveform: Waveform, data: AudioGramData) : Plotter {

        override var data: AudioGramData = data
        override var waveform: Waveform = waveform

        override fun plot(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {
            val path = GeneralPath(Path2D.WIND_NON_ZERO, 5)
            val points = ArrayList<Point>()
            val width = 25.0

            var x = waveform.posX
            var y = waveform.posY

            points.add(Point(x!!, y!!))

            for (i in 0 until ampData[0].size * 2) {

                var ampPoint = i
                if (i >= ampData[0].size)
                    ampPoint = i - ampData[0].size

                points.add(Point(x + width * (i + 1), y - ampData[currentPoint][ampPoint] * 1.0))
                if (i == (ampData[0].size * 2) - 1) {
                    points.add(Point(points.last().x + width, y))
                }
            }
            drawCurve(points, path, 5, 5)

            val scalex = waveform.width!! / path.bounds.width
            val scaley = 1.0

            var flip = AffineTransform.getTranslateInstance(0.0, data.meta.video.height!!.toDouble()).also { it.scale(1.0, -1.0) }
            var scale = AffineTransform.getScaleInstance(scalex, scaley)
            var restorePosition = AffineTransform.getTranslateInstance(0.0, 2 * waveform.posY!! - data.meta.video.height!!)

            path.closePath()

            var path2 = GeneralPath(path)
            var fill = Color.decode(waveform.fill1)
            path.transform(flip)
            path.transform(restorePosition)
            path.append(path2, false)

            path.transform(AffineTransform.getTranslateInstance(-waveform.posX!!, -waveform.posY!!))
            path.transform(scale)
            path.transform(AffineTransform.getTranslateInstance(waveform.posX!!, waveform.posY!!))

            when (waveform.fillMode) {

                FillMode.MONO -> {
                    val fill = Color.decode(waveform.fill1)
                    g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                    g2d.fill(path)

                    if (waveform.stroke!!) {
                        val stroke = Color.decode(waveform.strokeFill)
                        g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                        g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                        g2d.draw(path)
                    }
                }
                FillMode.GRADIENT_LR -> {
                    fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 2)
                    if (waveform.stroke!!) {
                        val stroke = Color.decode(waveform.strokeFill)
                        g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                        g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                        g2d.draw(path)
                    }
                }
                FillMode.GRADIENT_MID -> {
                    fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 1)
                    if (waveform.stroke!!) {
                        val stroke = Color.decode(waveform.strokeFill)
                        g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                        g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                        g2d.draw(path)
                    }
                }
                FillMode.GRADIENT_RL -> {
                    fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 3)
                    if (waveform.stroke!!) {
                        val stroke = Color.decode(waveform.strokeFill)
                        g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                        g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                        g2d.draw(path)
                    }
                }
            }

        }

    }

    internal class SigAmpPlotterRainBars(waveform: Waveform, data: AudioGramData) : Plotter {

        override var data: AudioGramData = data
        override var waveform: Waveform = waveform

        override fun plot(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {
            val span = waveform.width!!
            val gap = span / 17
            val width = gap * 2

            val x = waveform.posX!!
            val y = waveform.posY!!


            for (i in 0 until 6) {

                val posx = x + (width + gap) * i
                val posy = y - (ampData[currentPoint][i] + width) / 2

                val path = RoundRectangle2D.Double(posx, posy, width, width + ampData[currentPoint][i], width, width)

                when (waveform.fillMode) {

                    FillMode.MONO -> {
                        val fill = Color.decode(waveform.fill1)
                        g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                        g2d.fill(path)

                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_LR -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 2)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_MID -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 1)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_RL -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 3)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.TRIAD -> {
                        when (i) {
                            in 0..1 -> {
                                val fill = Color.decode(waveform.fill1)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                            in 2..3 -> {
                                val fill = Color.decode(waveform.fill2)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                            in 4..5 -> {
                                val fill = Color.decode(waveform.fill3)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                        }
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                }

            }
        }

    }

    internal class SigAmpPlotterArcReactor(waveform: Waveform, data: AudioGramData) : Plotter {

        override var data: AudioGramData = data
        override var waveform: Waveform = waveform

        override fun plot(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {

            val width = waveform.width!!
            val x = waveform.posX!!
            val y = waveform.posY!!

            val points = ArrayList<Point>()
            val points2 = ArrayList<Point>()


            var path: GeneralPath

            for (i in 0 until 3) {
                val amp = (ampData[currentPoint][i * 2] + ampData[currentPoint][i * 2 + 1]) / (1.2 + 0.4 * i)
                if (i > 0) {
                    points.clear()
                    points2.clear()
                }

                points.add(Point(x, y))
                points.add(Point(x + (width / 5), y - amp / 6))
                points.add(Point(x + width / 2, y - amp))
                points.add(Point(x + (width * 4 / 5), y - amp / 6))
                points.add(Point(x + width, y))


                points2.add(Point(x, y))
                points2.add(Point(x + (width / 5), y + amp / 6))
                points2.add(Point(x + width / 2, y + amp))
                points2.add(Point(x + (width * 4 / 5), y + amp / 6))
                points2.add(Point(x + width, y))

                path = GeneralPath()
                drawCurve(points, path, 5, 5)
                drawCurve(points2, path, 7, 5)

                when (waveform.fillMode) {

                    FillMode.MONO -> {
                        val fill = Color.decode(waveform.fill1)
                        g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                        g2d.fill(path)

                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_LR -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 2)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_MID -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 1)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_RL -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 3)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.TRIAD -> {
                        when (i) {
                            1 -> {
                                val fill = Color.decode(waveform.fill1)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                            2 -> {
                                val fill = Color.decode(waveform.fill2)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                            3 -> {
                                val fill = Color.decode(waveform.fill3)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                        }
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }

                }
            }
        }

    }

    internal class FreqAmpPlotterPelicanGrid(waveform: Waveform, data: AudioGramData) : Plotter {

        override var data: AudioGramData = data
        override var waveform: Waveform = waveform

        override fun plot(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {

            val x = waveform.posX!!
            val y = waveform.posY!!
            val width = waveform.width!!
            val diameter = width / 11


            val grid = ArrayList<GridCell>()

            //low
            grid.add(GridCell(-120.0, 0.6, x - (width - diameter) / 2, y - diameter))
            grid.add(GridCell(-60.0, 0.6, x - (width - diameter) / 2, y + diameter))

            grid.add(GridCell(-135.0, 0.6, x - (diameter * 3), y - diameter * 2))
            grid.add(GridCell(-90.0, 0.6, x - (diameter * 3), y))
            grid.add(GridCell(-45.0, 0.6, x - (diameter * 3), y + diameter * 2))


            //mid - high
            //column1
            grid.add(GridCell(180.0, 0.6, x - diameter, y - diameter * 3))
            grid.add(GridCell(0.0, 0.6, x - diameter, y + diameter * 3))

            grid.add(GridCell(180.0, 0.4, x - diameter, y - diameter))
            grid.add(GridCell(0.0, 0.4, x - diameter, y + diameter))

            //column2
            grid.add(GridCell(180.0, 0.6, x + diameter, y - diameter * 3))
            grid.add(GridCell(0.0, 0.6, x + diameter, y + diameter * 3))

            grid.add(GridCell(180.0, 0.4, x + diameter, y - diameter))
            grid.add(GridCell(0.0, 0.4, x + diameter, y + diameter))

            //low
            grid.add(GridCell(135.0, 0.6, x + (diameter * 3), y - diameter * 2))
            grid.add(GridCell(90.0, 0.6, x + (diameter * 3), y))
            grid.add(GridCell(45.0, 0.6, x + (diameter * 3), y + diameter * 2))

            grid.add(GridCell(120.0, 0.6, x + (width - diameter) / 2, y - diameter))
            grid.add(GridCell(60.0, 0.6, x + (width - diameter) / 2, y + diameter))
            //cells widths of 25 and


            for (i in 0 until grid.size) {
                val cell = grid[i]
                var amp: Double

                amp = if (i <= 4 || i > 12) {
                    cell.DisplacementFactor * (ampData[currentPoint][0] + ampData[currentPoint][1]) / 1.8
                } else {
                    cell.DisplacementFactor * (ampData[currentPoint][2] + ampData[currentPoint][3] + ampData[currentPoint][4] + ampData[currentPoint][5]) / 1.8
                }


                var path = Ellipse2D.Double((cell.x - diameter / 2) + (amp * sin((cell.displacementDirection / 180) * Math.PI)), (cell.y - diameter / 2) + (amp * cos((cell.displacementDirection / 180) * Math.PI)), diameter, diameter, )

                when (waveform.fillMode) {

                    FillMode.MONO -> {
                        val fill = Color.decode(waveform.fill1)
                        g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                        g2d.fill(path)

                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_LR -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 2)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_MID -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 1)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.GRADIENT_RL -> {
                        fillGradient(g2d, path, Color.decode(waveform.fill1!!), Color.decode(waveform.fill2!!), null, 3)
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                    FillMode.TRIAD -> {
                        when (i) {
                            in 0..4 -> {
                                val fill = Color.decode(waveform.fill1)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                            in 5..12 -> {
                                val fill = Color.decode(waveform.fill2)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                            in 13..17 -> {
                                val fill = Color.decode(waveform.fill3)
                                g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
                                g2d.fill(path)
                            }
                        }
                        if (waveform.stroke!!) {
                            val stroke = Color.decode(waveform.strokeFill)
                            g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                            g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                            g2d.draw(path)
                        }
                    }
                }
            }

        }

    }

    internal class FreqAmpPlotterMorphStack(waveform: Waveform, data: AudioGramData) : Plotter {

        override var data: AudioGramData = data
        override var waveform: Waveform = waveform

        override fun plot(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {

            val amp1 = (ampData[currentPoint][0] + ampData[currentPoint][1]) / 2
            val amp2 = (ampData[currentPoint][2] + ampData[currentPoint][3]) / 1.6
            val amp3 = (ampData[currentPoint][4] + ampData[currentPoint][5]) / 0.5

            val x = waveform.posX!!
            val y = waveform.posY!!
            val width = waveform.width!!

            val rad1 = width / 2
            val rad2 = width / 4
            val rad3 = width / 8

            val path1 = Ellipse2D.Double(x - rad1 - (amp1 / 2), y - rad1 - (amp1 / 2), (rad1 * 2) + amp1, (rad1 * 2) + amp1)
            val path2 = Ellipse2D.Double(x - rad2 - (amp2 / 2), y - rad2 - (amp2 / 2), (rad2 * 2) + amp2, (rad2 * 2) + amp2)
            val path3 = Ellipse2D.Double(x - rad3 - (amp3 / 2), y - rad3 - (amp3 / 2), (rad3 * 2) + amp3, (rad3 * 2) + amp3)


            var fill = Color.decode(waveform.fill1)
            g2d.color = fill
            g2d.stroke = BasicStroke(6f)
            g2d.draw(path1)
            g2d.stroke = BasicStroke(0f)

            fill = Color.decode(waveform.fill2)
            g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
            // if()
            AudioGramRenderer.generateGlow(path2, g2d, fill, (rad2 * 1.5).toInt(), 0.6f)
            g2d.fill(path2)

            fill = Color.decode(waveform.fill3)
            g2d.color = Color(fill.red, fill.green, fill.blue, (255 * (waveform.opacity!! / 100.0)).toInt())
            g2d.fill(path3)


            if (waveform.stroke!!) {
                val stroke = Color.decode(waveform.strokeFill)
                g2d.stroke = BasicStroke(waveform.strokeWidth!!.toFloat())
                g2d.color = Color(stroke.red, stroke.green, stroke.blue, (255 * (waveform.strokeOpacity!! / 100.0)).toInt())
                g2d.draw(path2)
                g2d.draw(path3)

            }

        }

    }

    internal class FreqAmpPlotterSpectralFlux(waveform: Waveform, data: AudioGramData) : Plotter {

        override var data: AudioGramData = data
        override var waveform: Waveform = waveform

        override fun plot(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {

            var path = GeneralPath(Path2D.WIND_NON_ZERO, 5)
            val points = ArrayList<Point>()

            var x = waveform.posX!!
            var y = waveform.posY!!

            var angle = 44.0 / (7) - (44.0 / 28)
            val radius = waveform.width!! / 2

            for (i in 0 until 7) {

                if (i % 2 == 0) {
                    var radX = x + (radius * cos(angle))
                    var radY = y + (radius * sin(angle))
                    points.add(Point(radX, radY))

                } else {
                    var radX = x + ((radius + (ampData[currentPoint][i - 1] + ampData[currentPoint][i]) * 0.7) * cos(angle))
                    var radY = y + ((radius + (ampData[currentPoint][i - 1] + ampData[currentPoint][i]) * 0.7) * sin(angle))
                    points.add(Point(radX, radY))
                }
                angle += (44.0 / 7) * (1.0 / 12)
            }
            angle = 44.0 / (7) - (44.0 / 28)
            for (i in 6 downTo 0) {

                if (i == 6) {
                    angle += (44.0 / 7) * (1.0 / 12)
                    continue
                }
                if (i % 2 == 0) {
                    var radX = x - (radius * cos(angle))
                    var radY = y - (radius * sin(angle))
                    points.add(Point(radX, radY))

                } else {
                    var radX = x - ((radius + (ampData[currentPoint][i - 1] + ampData[currentPoint][i]) * 0.7) * cos(angle))
                    var radY = y - ((radius + (ampData[currentPoint][i - 1] + ampData[currentPoint][i]) * 0.7) * sin(angle))
                    points.add(Point(radX, radY))
                }
                angle += (44.0 / 7) * (1.0 / 12)
            }


            drawCurve(points, path, 5, 5)
            path.closePath()

            var color = Color.decode(waveform.fill1!!)
            g2d.color = Color(color.red, color.green, color.blue, (255 * (10 / 100.0)).toInt())
            g2d.fill(path)

            var path2 = GeneralPath(path)
            path2.transform(AffineTransform.getTranslateInstance(-x, -y))
            path2.transform(AffineTransform.getScaleInstance(0.9, 0.9))
            path2.transform(AffineTransform.getTranslateInstance(x, y))

            g2d.color = Color.decode(waveform.fill2!!)
            g2d.fill(path2)

            var path3 = GeneralPath(path)
            path3.transform(AffineTransform.getTranslateInstance(-x, -y))
            path3.transform(AffineTransform.getScaleInstance(0.65, 0.65))
            path3.transform(AffineTransform.getTranslateInstance(x, y))

            g2d.color = Color.decode(waveform.fill3!!)
            g2d.fill(path3)

            var w = radius * 1.4 + ampData[currentPoint][1] / 4

            var circle = Ellipse2D.Double(x - w / 2, y - w / 2, w, w)
            g2d.color = Color.decode(waveform.fill1!!)
            g2d.fill(circle)
        }

    }

    interface Plotter {
        var data: AudioGramData
        var waveform: Waveform
        fun plot(ampData: java.util.ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D)
    }

    internal class GridCell(var displacementDirection: Double, var DisplacementFactor: Double, var x: Double, var y: Double)
}

