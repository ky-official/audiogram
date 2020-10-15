package com.audiogram.videogenerator

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import java.util.*
import kotlin.math.cos
import kotlin.math.sin


class AudioGramPlotter {


    fun getPlotter(audioGramData: AudioGramData):
            (AudioGramData, ArrayList<FloatArray>, Int, Graphics2D) -> Unit {

        if (audioGramData.meta.waveform.type == AudioGramWaveformType.SAD) {
            when (audioGramData.meta.waveform.design) {
                AudioGramWaveformDesign.DEFAULT -> return ::sigAmpPlotterSpectrogram
                AudioGramWaveformDesign.ARC_REACTOR -> TODO()
                null -> TODO()
            }
        } else if (audioGramData.meta.waveform.type == AudioGramWaveformType.FAD) {
            when (audioGramData.meta.waveform.design) {
                AudioGramWaveformDesign.DEFAULT -> return ::sigAmpPlotterSpectrogram
                AudioGramWaveformDesign.SPECTRAL_FLUX -> return ::freqAmpPlotterSpectralFlux
                null -> TODO()
            }
        }
        return { _: AudioGramData, _: ArrayList<FloatArray>, _: Int, _: Graphics2D -> run { println("invalid plotter parameters") } }
    }

    private fun sigAmpPlotterSpectrogram(data: AudioGramData, ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {

        val path = GeneralPath(Path2D.WIND_NON_ZERO, 5)
        val points = ArrayList<Point>()
        val width = 25.0

        var x = data.meta.waveform.posX
        var y = data.meta.waveform.posY

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

        val scalex = data.meta.waveform.width!! / path.bounds.width
        val scaley = 1.0

        var flip = AffineTransform.getTranslateInstance(0.0, data.meta.video.height!!.toDouble()).also { it.scale(1.0, -1.0) }
        var scale = AffineTransform.getScaleInstance(scalex, scaley)
        var restorePosition = AffineTransform.getTranslateInstance(0.0, 2 * data.meta.waveform.posY!! - data.meta.video.height!!)

        path.closePath()

        var path2 = GeneralPath(path)
        var fill = Color.decode(data.meta.waveform.fill_1)

        path.transform(flip)
        path.transform(restorePosition)
        path.append(path2, false)

        path.transform(AffineTransform.getTranslateInstance(-data.meta.waveform.posX!!, -data.meta.waveform.posY!!))
        path.transform(scale)
        path.transform(AffineTransform.getTranslateInstance(data.meta.waveform.posX!!, data.meta.waveform.posY!!))

        g2d.color = fill
        g2d.draw(path)
        g2d.fill(path)

    }

    private fun freqAmpPlotterSpectralFlux(data: AudioGramData, ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {


        var path = GeneralPath(Path2D.WIND_NON_ZERO, 5)
        val points = ArrayList<Point>()

        var x = data.meta.waveform.posX!!
        var y = data.meta.waveform.posY!!

        var angle = 44.0 / (7) - (44.0 / 28)
        val radius = data.meta.waveform.width!! / 2

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

        var color = Color.decode(data.meta.waveform.fill_1!!)
        g2d.color = Color(color.red, color.green, color.blue, (255 * (10 / 100.0)).toInt())
        g2d.fill(path)

        var path2 = GeneralPath(path)
        path2.transform(AffineTransform.getTranslateInstance(-x, -y))
        path2.transform(AffineTransform.getScaleInstance(0.9, 0.9))
        path2.transform(AffineTransform.getTranslateInstance(x, y))

        g2d.color = Color.decode(data.meta.waveform.fill_2!!)
        g2d.fill(path2)

        var path3 = GeneralPath(path)
        path3.transform(AffineTransform.getTranslateInstance(-x, -y))
        path3.transform(AffineTransform.getScaleInstance(0.65, 0.65))
        path3.transform(AffineTransform.getTranslateInstance(x, y))

        g2d.color = Color.decode(data.meta.waveform.fill_3!!)
        g2d.fill(path3)

        var w = radius * 1.4 + ampData[currentPoint][1] / 4

        var circle = Ellipse2D.Double(x - w / 2, y - w / 2, w, w)
        g2d.color = Color.decode(data.meta.waveform.fill_1!!)
        g2d.fill(circle)
    }


    private fun drawCurve(points: ArrayList<Point>, path: GeneralPath, inBend: Int, outBend: Int) {

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
}

