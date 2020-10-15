package com.audiogram.videogenerator

import org.h2.store.fs.FileUtils
import ws.schild.jave.*
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem


sealed class AudioGramAudioConverter {

    companion object {
        fun convert(path: String): String? {
            val source = AudioGramFileManager.getResource(path)
            val buffer = AudioGramFileManager.getResource(path.substringBefore(".") + "_converted.wav")
            val target = AudioGramFileManager.getResource(path.substringBefore(".") + "_converted.mp3")

            try {
                val inStream = AudioSystem.getAudioInputStream(source)
                val sourceFormat = inStream.format

                if ((sourceFormat.sampleSizeInBits == 32 || sourceFormat.sampleSizeInBits == 24)) {
                    val convertFormat = AudioFormat(
                            44100f, 16,
                            sourceFormat.channels,
                            true,
                            false)

                    println("source can be converted: ${AudioSystem.isConversionSupported(convertFormat, sourceFormat)}")

                    val convertedStream = AudioSystem.getAudioInputStream(convertFormat, inStream)
                    AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, buffer)

                    val url = mp3Convert(buffer, target)
                    FileUtils.delete(buffer.absolutePath)
                    return url
                }
                return mp3Convert(source, target)

            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        private fun mp3Convert(source: File, target: File): String {

            val listener = object : EncoderProgressListener {
                override fun message(p0: String?) {
                }

                override fun sourceInfo(p0: MultimediaInfo?) {
                }

                override fun progress(p0: Int) {
                }

            }

            val audio = AudioAttributes()
            audio.setCodec("libmp3lame")
            audio.setBitRate(128000)
            audio.setChannels(2)
            audio.setSamplingRate(44100)
            val attrs = EncodingAttributes()
            attrs.format = "mp3"
            attrs.audioAttributes = audio
            val encoder = Encoder()
            encoder.encode(MultimediaObject(source), target, attrs, listener)
            return target.absolutePath

        }
    }
}