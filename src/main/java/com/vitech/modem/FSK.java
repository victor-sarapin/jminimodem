package com.vitech.modem;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.ArithmeticUtils;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/fsk.c">source code</a>
 */
public class FSK {
    private int markBand;
    private int spaceBand;

    private int getMarkBand() {
        return markBand;
    }

    private int getSpaceBand() {
        return spaceBand;
    }

    private void setMarkBand(int markBand) {
        this.markBand = markBand;
    }

    private void setSpaceBand(int spaceBand) {
        this.spaceBand = spaceBand;
    }

    class FrameAnalysisInfo {
        private double   confidence;
        private byte   bits;
        private float   ampl;
        private int     startPos;

        FrameAnalysisInfo() {
            this(0.0f, (byte)0, 0.0f, 0);
        }

        FrameAnalysisInfo(float confidence, byte bits, float ampl, int startPos) {
            this.confidence = confidence;
            this.bits       = bits;
            this.ampl       = ampl;
            this.startPos   = startPos;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        public float getAmpl() {
            return ampl;
        }

        public void setAmpl(float ampl) {
            this.ampl = ampl;
        }

        public void setBits(byte bits) {
            this.bits = bits;
        }

        public byte getBits() {
            return bits;
        }

        public void setStartPos(int startPos) {
            this.startPos = startPos;
        }

        public int getStartPos() {
            return startPos;
        }
    }

    class BitAnalysisInfo {
        private byte     bit;
        private double   signalMag;
        private double   noiseMag;

        public BitAnalysisInfo() {
            this((byte) 0, 0, 0);
        }

        public BitAnalysisInfo(byte bit, double signalMag, double noiseMag) {
            this.bit        = bit;
            this.signalMag  = signalMag;
            this.noiseMag   = noiseMag;
        }

        public void set(byte b, double signalMag, double noiseMag) {
            this.bit        = b;
            this.signalMag  = signalMag;
            this.noiseMag   = noiseMag;
        }

        public void setBit(byte bit) {
            this.bit = bit;
        }

        public int getBit() {
            return bit;
        }

        public double getSignalMag() {
            return signalMag;
        }

        public void setSignalMag(float signalMag) {
            this.signalMag = signalMag;
        }

        public double getNoiseMag() {
            return noiseMag;
        }

        public void setNoiseMag(float noiseMag) {
            this.noiseMag = noiseMag;
        }
    }

    public static final String PROPERTY_NAME_FSK_AUTODETECT_MIN_FREQ    = "FSK_AUTODETECT_MIN_FREQ";
    public static final String PROPERTY_NAME_FSK_AUTODETECT_MAX_FREQ    = "FSK_AUTODETECT_MAX_FREQ";
    public static final String PROPERTY_NAME_FSK_MIN_MAGNITUDE          = "FSK_MIN_MAGNITUDE";
    public static final String PROPERTY_NAME_FSK_MIN_BIT_SNR            = "FSK_MIN_BIT_SNR";
    public static final String PROPERTY_NAME_FSK_AVOID_TRANSIENTS       = "FSK_AVOID_TRANSIENTS";
    public static final String PROPERTY_NAME_CONFIDENCE_ALGO            = "CONFIDENCE_ALGO";

    private Logger log = Logger.getLogger(FSK.class.getName());

    private int bandWidth;
    private int NBands;

    public int getBandWidth() {
        return bandWidth;
    }

    public int getNBands() {
        return NBands;
    }

    public FSK(int bandWidth, int NBands) {
        this.bandWidth  = bandWidth;
        this.NBands     = NBands;
    }

    public double findFrame(float[] samples, int tryFirstSample, int tryMaxNSamples, int tryStepNSamples, float confidenceSearchLimit, String expectBitsString, FrameAnalysisInfo bestFrameInfo) {
        int expect_n_bits = expectBitsString.length();
        float samples_per_bit = (float) samples.length / expect_n_bits;

        // Scan the frame positions starting with the one try_first_sample,
        // alternating between a step above that, a step below that, above, below,
        // and so on, until we've scanned the whole try_max_nsamples range.
        FrameAnalysisInfo currentFrameInfo = new FrameAnalysisInfo();

        for (int j = 0; ; j++) {
            int up = (j % 2) != 0 ? 1 : -1;
            int t = tryFirstSample + up * ((j + 1) / 2) * tryStepNSamples;

            if (t >= tryMaxNSamples) {
                break;
            }

            if (t < 0) {
                continue;
            }

            currentFrameInfo.setConfidence(0.0f);
            currentFrameInfo.setAmpl(0.0f);
            currentFrameInfo.setBits((byte) 0);
            currentFrameInfo.setStartPos(t);

            //debug_log("try fsk_frame_analyze at t=%d\n", t);
            frameAnalyze(samples, t, expect_n_bits, samples_per_bit, expectBitsString, currentFrameInfo);

            if (bestFrameInfo.getConfidence() < currentFrameInfo.getConfidence()) {
                bestFrameInfo.setStartPos(t);
                bestFrameInfo.setConfidence(currentFrameInfo.getConfidence());
                bestFrameInfo.setAmpl(currentFrameInfo.getAmpl());
                bestFrameInfo.setBits(currentFrameInfo.getBits());

                // If we find a frame with confidence > try_confidence_search_limit
                // quit searching.
                if (bestFrameInfo.getConfidence() >= confidenceSearchLimit) {
                    break;
                }
            }
        }

        return bestFrameInfo.getConfidence();
    }

    public int detectCarrier(float[] samples, float minMagnitudeThreshold) {
        int fftLength = getCeilPow2(samples.length);
        double[] fftSamples = convertFloatsToDoubles(Arrays.copyOf(samples, fftLength));
        Complex[] fftResult = new FastFourierTransformer(DftNormalization.STANDARD).transform(fftSamples, TransformType.FORWARD);

        //Now looking for band of max magnitude within configured boundaries
        double magScalar = 1.0d / (samples.length / 2.0d);
        double maxMag = 0.0;
        int maxMagBand = -1;
        int i = 1;	/* start detection at the first non-DC band */
        int nbands = fftLength / 2 + 1;

        if (isFskAutodetectMinFreq()) {
            i = Math.round((getFskMinFreq() + getBandWidth() / 2) / getBandWidth());
        }

        if (isFskAutodetectMaxFreq()) {
            nbands = Math.round((getFskMaxFreq() + getBandWidth() / 2) / getBandWidth());
            if (nbands > getNBands()) {
                nbands = getNBands();
            }
        }

        for (; i < nbands; i++) {
            double mag = fftResult[i].abs() * magScalar;

            if (mag < minMagnitudeThreshold) {
                continue;
            }

            if (maxMag < mag) {
                maxMag = mag;
                maxMagBand = i;
            }
        }

        if (maxMagBand < 0) {
            return -1;
        }

        return maxMagBand;
    }

    private int getCeilPow2(int value) {
        int fftLength = value;

        //Aligning to power of 2, as Commons-Math FastFourierTransformer requires
        while (ArithmeticUtils.isPowerOfTwo(fftLength)) {
            fftLength++;
        }

        return fftLength;
    }

    private void setTonesByBandshift(int mark, int bandShift) {
        Validate.isTrue(bandShift != 0);
        Validate.isTrue(mark < getNBands());
        int space = mark + bandShift;
        Validate.isTrue(space >= 0);
        Validate.isTrue(space < getNBands());
        setMarkBand(mark);
        setSpaceBand(space);
    }

    private void bitAnalyze(float[] samples, int startFrom, int bitCount, BitAnalysisInfo info) {
        int fftLength = getCeilPow2(samples.length);
        double[] fftSamples = convertFloatsToDoubles(samples, startFrom, bitCount, fftLength);
        Complex[] fftResult = new FastFourierTransformer(DftNormalization.STANDARD).transform(fftSamples, TransformType.FORWARD);

        double magScalar = 2.0f / samples.length;

        double magMark = fftResult[getMarkBand()].abs() * magScalar;
        double magSpace = fftResult[getSpaceBand()].abs() * magScalar;

        // mark==1, space==0
        if (magMark > magSpace) {
            info.set((byte)1, magMark, magSpace);
        } else {
            info.set((byte)0, magSpace, magMark);
        }
    }

    private double frameAnalyze(float[] samples, int startPos, int n_bits, float samples_per_bit, String expectBitsString, FrameAnalysisInfo resultFrameInfo) {
        int bit_nsamples = Math.round(samples_per_bit + 0.5f);
        BitAnalysisInfo bits[] = new BitAnalysisInfo[32];
        int bitnum = 0;
        int bit_begin_sample;

        /* pass #1 - process and check only the "required" (1/0) expect_bits */
        for (bitnum = startPos; bitnum < startPos + n_bits; ++bitnum) {
            // expect_bits[bitnum] == 'd'
            char c = (char) (((expectBitsString.charAt(bitnum) & 0x00FF) << 8) + (expectBitsString.charAt(bitnum) & 0x00FF));
            if (c == 'd') {
                continue;
            }

            Validate.isTrue(expectBitsString.charAt(bitnum) == 1 || expectBitsString.charAt(bitnum) == 0);
            bit_begin_sample = Math.round(samples_per_bit * bitnum + 0.5f);

            //debug_log( " bit# %2u @ %7u: ", bitnum, bit_begin_sample);

            bits[bitnum].setBit((byte) expectBitsString.charAt(bitnum));
            bitAnalyze(samples, bit_begin_sample, bit_nsamples, bits[bitnum]);

            if (expectBitsString.charAt(bitnum) != bits[bitnum].getBit()) {
                resultFrameInfo.setConfidence(0.0f);
                return 0.0f; /* does not match expected; abort frame analysis. */
            }

            if (isFskMinBitSnr()) {
                double bit_snr = bits[bitnum].getSignalMag() / bits[bitnum].getNoiseMag();

                if (bit_snr < getFskMinBitSnr()) {
                    resultFrameInfo.setConfidence(0.0f);
                    return 0.0f;
                }
            }

            if (isFskMinMagnitude()) {
                // Performance hack: reject frame early if sig mag isn't even half
                // of FSK_MIN_MAGNITUDE
                if (bits[bitnum].getSignalMag() < getFskMinMagnitude() / 2.0f) {
                    resultFrameInfo.setConfidence(0.0f);
                    return 0.0f; // too weak; abort frame analysis
                }
            }
        }

         /* pass #2 - process only the dontcare ('d') expect_bits */
        for (bitnum = startPos; bitnum < startPos + n_bits; ++bitnum) {
            // expect_bits[bitnum] == 'd'
            char c = (char) (((expectBitsString.charAt(bitnum) & 0x00FF) << 8) + (expectBitsString.charAt(bitnum) & 0x00FF));
            if (c != 'd') {
                continue;
            }

            bit_begin_sample = Math.round(samples_per_bit * bitnum + 0.5f);

            // debug_log( " bit# %2u @ %7u: ", bitnum, bit_begin_sample);
            bitAnalyze(samples, bit_begin_sample, bit_nsamples, bits[bitnum]);

            if (isFskMinBitSnr()) {
                double bit_snr = bits[bitnum].getSignalMag() / bits[bitnum].getNoiseMag();
                if (bit_snr < getFskMinBitSnr()) {
                    resultFrameInfo.setConfidence(0.0f);
                    return 0.0f;
                }
            }
        }

        double confidence = 0.0f;
        if (isConfidenceAlgo()) {
            int confidenceAlgo = getConfidenceAlgo();

            if (confidenceAlgo == 5 || confidenceAlgo == 6) {
                float total_bit_sig = 0.0f;
                float total_bit_noise = 0.0f;
                for (bitnum = 0; bitnum < n_bits; ++bitnum) {
                    // Deal with floating point data type quantization noise...
                    // If total_bit_noise <= FLT_EPSILON, then assume it to be 0.0,
                    // so that we end up with snr==inf.
                    total_bit_sig += bits[bitnum].getSignalMag();
                    if (bits[bitnum].getNoiseMag() > Float.MIN_VALUE) {
                        total_bit_noise += bits[bitnum].getNoiseMag();
                    }
                }

                // Compute the "frame SNR"
                float snr = total_bit_sig / total_bit_noise;

                // Compute avg bit sig and noise magnitudes
                float avg_bit_sig = total_bit_sig / n_bits;

                float divergence = 0.0f;
                if (confidenceAlgo == 6) {
                    // Compute average "divergence": bit_mag_divergence / other_bits_mag
                    for (bitnum = 0; bitnum < n_bits; ++bitnum) {
                        double avg_bit_sig_other = (total_bit_sig - Math.abs(bits[bitnum].getSignalMag())) / (n_bits - 1);
                        divergence += Math.abs(bits[bitnum].getSignalMag() - avg_bit_sig_other) / avg_bit_sig_other;
                    }
                    divergence *= 2;
                    divergence /= n_bits;
                } // if (confidenceAlgo == 6)

                if (isFskMinMagnitude() && avg_bit_sig < getFskMinMagnitude()) {
                    resultFrameInfo.setConfidence(0.0f);
                    return 0.0f; // too weak; reject frame
                }

                if (confidenceAlgo == 6) {
                    // Frame confidence is the frame ( SNR * consistency )
                    confidence = snr * (1.0f - divergence);
                } else {
                    // Frame confidence is the frame SNR
                    confidence = snr;
                } // if (confidenceAlgo == 6)

                resultFrameInfo.setAmpl(avg_bit_sig);
            }
        }

        // least significant bit first ... reverse the bits as we place them
        // into the bits_outp word.
        resultFrameInfo.setBits((byte) 0);

        for (bitnum = 0; bitnum < n_bits; bitnum++) {
            int bits_outp = resultFrameInfo.getBits() | (bits[bitnum].getBit() << bitnum);
            resultFrameInfo.setBits((byte) bits_outp);
        }

        String logMsg = String.format(" frame algo=%u confidence=%f ampl=%f\n", getConfidenceAlgo(), resultFrameInfo.getConfidence(), resultFrameInfo.getAmpl());
        log.finest(logMsg);

        return confidence;
    }

    private static boolean isFskAutodetectMinFreq() {
        return StringUtils.isNotEmpty(System.getProperty(PROPERTY_NAME_FSK_AUTODETECT_MIN_FREQ));
    }

    private static float getFskMinFreq() {
        return Float.valueOf(System.getProperty(PROPERTY_NAME_FSK_AUTODETECT_MIN_FREQ));
    }

    private static boolean isFskAutodetectMaxFreq()  {
        return StringUtils.isNotEmpty(System.getProperty(PROPERTY_NAME_FSK_AUTODETECT_MAX_FREQ));
    }

    private static float getFskMaxFreq() {
        return Float.valueOf(System.getProperty(PROPERTY_NAME_FSK_AUTODETECT_MAX_FREQ));
    }

    private static boolean isFskMinBitSnr() {
        return StringUtils.isNotEmpty(System.getProperty(PROPERTY_NAME_FSK_MIN_BIT_SNR));
    }

    private static float getFskMinBitSnr() {
        return Float.valueOf(System.getProperty(PROPERTY_NAME_FSK_MIN_BIT_SNR));
    }

    private static boolean isFskMinMagnitude() {
        return StringUtils.isNotEmpty(System.getProperty(PROPERTY_NAME_FSK_MIN_MAGNITUDE));
    }

    private static float getFskMinMagnitude() {
        return Float.valueOf(System.getProperty(PROPERTY_NAME_FSK_MIN_MAGNITUDE));
    }

    private static boolean isFskAvoidTransients() {
        return StringUtils.isNotEmpty(System.getProperty(PROPERTY_NAME_FSK_AVOID_TRANSIENTS));
    }

    private static float getFskAvoidTransients() {
        return Float.valueOf(System.getProperty(PROPERTY_NAME_FSK_AVOID_TRANSIENTS));
    }

    private static boolean isConfidenceAlgo() {
        return StringUtils.isNotEmpty(System.getProperty(PROPERTY_NAME_CONFIDENCE_ALGO));
    }

    private static int getConfidenceAlgo() {
        return Integer.valueOf(System.getProperty(PROPERTY_NAME_CONFIDENCE_ALGO));
    }

    private static double[] convertFloatsToDoubles(float[] input)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = input[i];
        }
        return output;
    }

    private static double[] convertFloatsToDoubles(float[] input, int from, int count, int newLength)
    {
        if (input == null)
        {
            return null; // Or throw an exception - your choice
        }
        double[] output = new double[newLength];
        for (int i = from; i < count; i++)
        {
            output[i] = input[i];
        }
        return output;
    }
}
