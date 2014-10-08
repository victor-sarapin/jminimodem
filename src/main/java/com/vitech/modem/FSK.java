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
    class FrameAnalysisInfo {
        private float   confidence;
        private boolean[]   bits;
        private float   ampl;
        private int     startPos;

        FrameAnalysisInfo() {
            this(0.0f, new boolean[]{false}, 0.0f, 0);
        }

        FrameAnalysisInfo(float confidence, boolean[] bits, float ampl, int startPos) {
            this.confidence = confidence;
            this.bits       = bits;
            this.ampl       = ampl;
            this.startPos   = startPos;
        }
    }

    class BitAnalysisInfo {
        private boolean[]     bit;
        private float   signalMag;
        private float   noiseMag;

        public BitAnalysisInfo() {
            this(new boolean[]{false}, 0, 0);
        }

        public BitAnalysisInfo(boolean[] bit, int signalMag, int noiseMag) {
            this.bit        = bit;
            this.signalMag  = signalMag;
            this.noiseMag   = noiseMag;
        }
    }

    public static final String PROPERTY_NAME_FSK_AUTODETECT_MIN_FREQ    = "FSK_AUTODETECT_MIN_FREQ";
    public static final String PROPERTY_NAME_FSK_AUTODETECT_MAX_FREQ    = "FSK_AUTODETECT_MAX_FREQ";

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

    public void findFrame(float[] samples, int tryFirstSample, int tryMaxNSamples, int tryStepNSamples, float confidenceSearchLimit, String expectBitsString) {}

    public int detectCarrier(float[] samples, float minMagnitudeThreshold) {
        int fftLength = samples.length;

        //Aligning to power of 2, as Commons-Math FastFourierTransformer requires
        while (ArithmeticUtils.isPowerOfTwo(fftLength)) {
            fftLength++;
        }

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

    private void setTonesByBandshift(int mark, int bandShift) {}
    private BitAnalysisInfo bitAnalyze(float[] samples) {  return null;    }
    private FrameAnalysisInfo frameAnalyze(float[] samples) {  return null;    }

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
}
