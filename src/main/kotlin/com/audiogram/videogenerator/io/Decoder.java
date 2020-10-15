package com.audiogram.videogenerator.io;

/**
 * Interface for audio decoders that return successive
 * amplitude frames.
 *
 * @author mzechner
 */
public interface Decoder {
    /**
     * Reads in samples.length samples from the decoder. Returns
     * the actual number read in. If this number is smaller than
     * samples.length then the end of stream has been reached.
     *
     * @param samples The number of read samples.
     */
    int readSamples(float[] samples);
}
