package com.audiogram.videogenerator

import com.audiogram.videogenerator.utility.TextAlignment

class LemonMeta {
    lateinit var video: Video
    lateinit var waveform: Waveform
    lateinit var tracker: AudioTracker
}

class Shape {
    var shapeType: AudioGramShapeType? = null
    var posX: Int? = null
    var posY: Int? = null
    var width: Int? = null
    var height: Int? = null
    val fill: String? = null
    var svg: String? = null
    var outline: Boolean? = false
    var outlineWidth: Int? = null
    var outlineColor: String? = null
    var zIndex: Int? = null
    var opacity: Int? = null
}

class LemonImage {
    var url: String? = null
    var frame: AudioGramFrameType? = null
    var frameColor: String? = null
    var width: Double? = null
    var height: Double? = null
    var mask: AudioGramMaskType? = null
    var transform: String? = null
    var posX: Double? = null
    var posY: Double? = null
    var zIndex: Int? = null
    var align: AudioGramImageAlign? = null
    var imageEffect: AudioGramImageEffect? = null
    var filter: AudioGramFilterType? = null
    var filterFill: String? = null
    var opacity: Int? = null
}

class LemonText {
    var value: String? = null
    var font: String? = null
    var fontSize: Int? = null
    var fontStyle: AudioGramFontStyle? = null
    var fontWeight: AudioGramFontWeight? = null
    var color: String? = null
    var posX: Int? = null
    var posY: Int? = null
    var zIndex: Int? = null
    var align: TextAlignment? = null
    var width: Int? = null
    var spacing: AudioGramSpacing? = null
    var opacity: Int? = null
}

class Effect {
    var effectType: AudioGramEffectType? = null
    var effectMode: AudioGramEffectMode? = null
    var posX: Int? = null
    var posY: Int? = null
    var width: Double? = null
    var height: Double? = null
    val fill: String? = null
}


class Video {
    val fill: String? = null
    var width: Double? = null
    var height: Double? = null
    var waterMark: Boolean? = null
    var waterMarkType: AudioGramWaterMark? = null
}

class Waveform {
    var type: AudioGramWaveformType? = null
    var fill_1: String? = null
    var fill_2: String? = null
    var fill_3: String? = null
    var stroke: String? = null
    var design: AudioGramWaveformDesign? = null
    var width: Double? = null
    var height: Double? = null
    var posX: Double? = null
    var posY: Double? = null
    var opacity: Int? = null
}

class AudioTracker {
    var display: Boolean? = null
    var type: AudioGramAudioTrackerType? = null
    var fill: String? = null
    var posX: Double? = null
    var posY: Double? = null
    var opacity: Int? = null
    var length: Double? = null
}

class Point(x: Double, y: Double) {
    val x = x
    val y = y
}
