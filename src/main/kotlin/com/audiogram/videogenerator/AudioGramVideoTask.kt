package com.audiogram.videogenerator

import com.audiogram.videogenerator.analysis.FFT
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.*
import kotlin.math.abs
import kotlin.math.roundToInt

/*
* Represents a waveform video task.
* Handles the video rendering.
* Instances are constructed with a lemonData object. Which contains the meta data needed.
* */
class AudioGramVideoTask(data: AudioGramData) {

    var done = false
    private val frameWidth = data.meta.video.width!!.toInt()
    private val frameHeight = data.meta.video.height!!.toInt()
    private var data = data
    private var audioUrl: String = data.audioUrl
    private var videoUrl: String = AudioGramFileManager.createVideoContainer(data.id)
    private var writer = ToolFactory.makeWriter(videoUrl)

    private val MAX_VALUE = 1.0f / java.lang.Short.MAX_VALUE
    private val size = 1024
    private val sampleRate = 44100f

    private val fft = FFT(size, sampleRate)

    private val monoSamples: FloatArray = FloatArray(size)
    private var freqAmpData: ArrayList<FloatArray> = ArrayList()
    private var sigAmpData: ArrayList<FloatArray> = ArrayList()
    private var smooth = FloatArray(6) { _ -> 1f }

    init {

        writer.addVideoStream(
                0,
                0,
                ICodec.ID.CODEC_ID_H264,
                IRational.make(30.0),
                frameWidth,
                frameHeight
        )
        writer.addAudioStream(1, 1, 2, 44100)

        this.decode()
        println("audio source decoded")

    }

    fun render() {
        val render = AudioGramRenderer()
        if (data.meta.waveform.type == AudioGramWaveformType.FAD) {
            render.start(data, writer, freqAmpData)
        } else if (data.meta.waveform.type == AudioGramWaveformType.SAD) {
            render.start(data, writer, sigAmpData)
        }
    }

    private fun decode() {

        var ampData: ArrayList<ArrayList<Float>> = ArrayList()

        val audioContainer: IContainer = IContainer.make()
        audioContainer.open(this.audioUrl, IContainer.Type.READ, null)
        data.trackLength = audioContainer.duration / 1000000.0

        val stream = audioContainer.getStream(0)
        val coder: IStreamCoder = stream.streamCoder
        coder.open()

        val packet: IPacket = IPacket.make()

        for (i in 0..size / 2) {
            ampData.add(ArrayList<Float>())
        }
        var inputSamples = IAudioSamples.make(512, coder.channels.toLong(), IAudioSamples.Format.FMT_S32)
        while (audioContainer.readNextPacket(packet) >= 0) {

            var offset = 0
            while (offset < packet.size) {
                val bytesDecoded = coder.decodeAudio(inputSamples, packet, offset)
                if (bytesDecoded < 0) {
                    throw RuntimeException("could not detect audio")
                }
                offset += bytesDecoded
                if (inputSamples.isComplete) {

                    if (data.meta.waveform.type == AudioGramWaveformType.FAD) {
                        for (index in 0 until size) {
                            var amp1 = inputSamples.getSample(index.toLong(), 0, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var amp2 = inputSamples.getSample(index.toLong(), 1, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var monoAmp = (amp1 + amp2) / 2
                            monoSamples[index] = monoAmp
                        }
                        fft.forward(monoSamples)
                        var array = FloatArray(6)

                        array[0] = 10 * Math.log(fft.calcAvg(20f, 80f) * 1.0).toFloat() * 3
                        array[1] = 10 * Math.log(fft.calcAvg(80f, 200f) * 1.0).toFloat() * 3
                        array[2] = 10 * Math.log(fft.calcAvg(200f, 1000f) * 1.0).toFloat() * 3
                        array[3] = 10 * Math.log(fft.calcAvg(1000f, 2000f) * 1.0).toFloat() * 3
                        array[4] = 10 * Math.log(fft.calcAvg(2000f, 4000f) * 1.0).toFloat() * 3
                        array[5] = 10 * Math.log(fft.calcAvg(4000f, 20000f) * 1.0).toFloat() * 3

                        for (i in 0 until array.size) {
                            smooth[i] = 0.35f * array[i] + 0.65f * smooth[i]
                            if (smooth[i] == Float.NEGATIVE_INFINITY) {
                                smooth[i] = 0f
                            }

                            if (smooth[i] < 0.0f) {
                                smooth[i] = 0f
                            }
                        }

                        freqAmpData.add(smooth.clone())
                        writer.encodeAudio(1, inputSamples)
                    } else if (data.meta.waveform.type == AudioGramWaveformType.SAD) {
                        for (index in 0 until size) {
                            var amp1 = inputSamples.getSample(index.toLong(), 0, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var amp2 = inputSamples.getSample(index.toLong(), 1, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var monoAmp = (amp1 + amp2) / 2
                            monoSamples[index] = abs(monoAmp * 110)
                        }

                        var array = arraySampler(monoSamples, 6)

                        for (i in 0 until array.size) {
                            smooth[i] = 0.3f * array[i] + 0.7f * smooth[i]
                            if (smooth[i] == Float.NEGATIVE_INFINITY) {
                                smooth[i] = 0f
                            }

                            if (smooth[i] < 0.0f) {
                                smooth[i] = 0f
                            }
                        }
                        sigAmpData.add(smooth.clone())
                        writer.encodeAudio(1, inputSamples)
                    }

                }
            }
        }
        if (data.meta.waveform.type == AudioGramWaveformType.FAD) {
            freqAmpData = arraySampler(freqAmpData, (30 * data.trackLength!!).roundToInt())
        } else if (data.meta.waveform.type == AudioGramWaveformType.SAD) {
            sigAmpData = arraySampler(sigAmpData, (30 * data.trackLength!!).roundToInt())

        }
    }

    private inline fun <reified T> arraySampler(array: ArrayList<T>, sampleSize: Int): ArrayList<T> {

        if (sampleSize > array.size) {
            return array
        }
        val result: ArrayList<T> = ArrayList<T>()
        val totalItems = array.size
        val interval = totalItems.toDouble() / sampleSize

        for (i in 0 until sampleSize) {
            val evenIndex = Math.floor(i * interval + interval / 2).toInt()
            result.add(array[evenIndex])
        }
        return result
    }

    private fun arraySampler(array: FloatArray, sampleSize: Int): FloatArray {

        if (sampleSize > array.size) {
            return array
        }
        val result: FloatArray = FloatArray(sampleSize)
        val totalItems = array.size
        val interval = totalItems.toDouble() / sampleSize

        for (i in 0 until sampleSize) {
            val evenIndex = Math.floor(i * interval + interval / 2).toInt()
            result[i] = (array[evenIndex])
        }
        return result
    }

}


