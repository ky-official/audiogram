package com.audiogram.videogenerator

enum class AudioGramWaveformDesign {
    DEFAULT, SPECTRAL_FLUX, FRINGE, ARC_REACTOR
}

enum class AudioGramWaveformType {
    FAD, SAD
}

enum class AudioGramMaskType {
    NONE, CIRCLE, SQUARE
}

enum class AudioGramFrameType {
    NONE, THIN, NORMAL, SOLID
}

enum class AudioGramFontWeight {
    NONE, THIN, NORMAL, BOLD
}

enum class AudioGramFontStyle {
    NONE, ITALIC
}

enum class AudioGramWaterMark {
    LIGHT, DARK
}

enum class AudioGramSpacing {
    TIGHT, NORMAL, LOOSE
}

enum class AudioGramImageAlign {
    NONE, CENTER, LEFT, RIGHT
}

enum class AudioGramScaleMode {
    UP_SCALING, EQUI_SCALING, DOWN_SCALING
}

enum class AudioGramAudioTrackerType {
    BOX_BORDER, HORIZONTAL_BAR
}

enum class AudioGramEffectType {
    PARTICLE, DISTORTION, VINYL
}

enum class AudioGramEffectMode {
    DEFAULT, ALPHA, BETA, GAMMA
}

enum class AudioGramShapeType {
    BOX, CIRCLE, LINE, SVG
}

enum class AudioGramFilterType {
    NONE, SCREEN,
}

enum class AudioGramImageEffect {
    NONE, BLUR, JITTER, MONOCHROME
}