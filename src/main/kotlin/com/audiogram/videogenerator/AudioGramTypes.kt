package com.audiogram.videogenerator

enum class AudioGramWaveformDesign {
    DEFAULT, SPECTRAL_FLUX, ARC_REACTOR, SPECTROGRAM, MORPH_STACK, PELICAN_GRID, RAIN_BARS
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
    THIN, NORMAL, BOLD
}

enum class AudioGramFontStyle {
    ITALIC
}

enum class AudioGramSpacing {
    TIGHT, NORMAL, LOOSE
}

enum class AudioGramImageAlign {
    CENTER, LEFT, RIGHT
}

enum class AudioGramScaleMode {
    UP_SCALING, EQUI_SCALING, DOWN_SCALING
}

enum class AudioGramAudioTrackerType {
    HORIZONTAL_BAR
}

enum class AudioGramEffectType {
    PARTICLE
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

enum class FillMode {
    MONO, GRADIENT_MID, GRADIENT_LR, GRADIENT_RL, TRIAD
}