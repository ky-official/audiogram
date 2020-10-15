package com.audiogram.videogenerator

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameUtils
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.roundToInt


class AudioGramFrameGrabber(data: AudioGramData, fpsOut: Int) {


    private var data = data
    private var fpsIn: Int
    private var mode = AudioGramScaleMode.EQUI_SCALING //default mode

    private var grabberCounter: Int = 0
    private var outputCounter: Int = 0
    private var currentFrameIndex: Int = 0

    private var frameGrabber: FFmpegFrameGrabber
    private var outputImage: BufferedImage? = null

    private lateinit var scaleArray: IntArray

    init {

        val file = File(data.videoUrl!!)
        frameGrabber = FFmpegFrameGrabber(file)
        frameGrabber.start()
        fpsIn = frameGrabber.videoFrameRate.roundToInt()

        when {
            fpsOut > fpsIn -> {
                mode = AudioGramScaleMode.UP_SCALING
                scaleArray = findIndexes(fpsOut, fpsIn)

            }
            fpsOut < fpsIn -> {
                mode = AudioGramScaleMode.DOWN_SCALING
                scaleArray = findIndexes(fpsIn, fpsOut)
            }
            else -> mode = AudioGramScaleMode.EQUI_SCALING
        }
    }

    private fun findIndexes(n: Int, r: Int): IntArray {

        val values = IntArray(n) { 0 }
        val offset = r - 1

        for (i in 0 until r) {
            val pos = (offset + (i * n)) / r
            values[pos % n] = 1
        }
        return values
    }

    fun flush() {
        outputImage!!.flush()
    }

    fun grabNext(): BufferedImage? {
        if (grabberCounter == frameGrabber.lengthInVideoFrames) {
            frameGrabber.restart()
            grabberCounter = 0
        }
        when (mode) {
            AudioGramScaleMode.EQUI_SCALING -> {
                val frame = frameGrabber.grabImage()
                if (frame != null)
                    outputImage = Java2DFrameUtils.toBufferedImage(frame)
                grabberCounter++
            }
            AudioGramScaleMode.DOWN_SCALING -> {

                if (currentFrameIndex >= scaleArray.size - 1) {
                    currentFrameIndex = 0
                }
                for (i in currentFrameIndex until scaleArray.size) {
                    if (scaleArray[i] != 0) {
                        val frame = frameGrabber.grabImage()
                        if (frame != null)
                            outputImage = Java2DFrameUtils.toBufferedImage(frame)
                        currentFrameIndex = i + 1
                        break
                    }
                }
            }
            AudioGramScaleMode.UP_SCALING -> {

                if (currentFrameIndex >= scaleArray.size - 1) {
                    currentFrameIndex = 0
                }

                if (scaleArray[currentFrameIndex] != 0) {
                    val frame = frameGrabber.grabImage()
                    if (frame != null)
                        outputImage = Java2DFrameUtils.toBufferedImage(frame)
                    currentFrameIndex++
                    grabberCounter++

                } else {
                    currentFrameIndex++
                }
            }
        }
        outputCounter++
        //return Scalr.resize(outputImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_HEIGHT, data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt(), Scalr.OP_ANTIALIAS)
        return outputImage
    }

}