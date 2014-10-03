package com.vitech.modem;

import com.sun.istack.internal.logging.Logger;
import java.util.Arrays;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;

public final class FskPlanUtils {
    public static final String PROPERTY_NAME_USE_FFT                    = "USE_FFT";
    public static final String PROPERTY_NAME_FSK_AUTODETECT_MIN_FREQ    = "FSK_AUTODETECT_MIN_FREQ";
    public static final String PROPERTY_NAME_FSK_AUTODETECT_MAX_FREQ    = "FSK_AUTODETECT_MAX_FREQ";
    public static final String PROPERTY_NAME_FSK_MIN_MAGNITUDE          = "FSK_MIN_MAGNITUDE";
    public static final String PROPERTY_NAME_FSK_MIN_BIT_SNR            = "FSK_MIN_BIT_SNR";
    public static final String PROPERTY_NAME_FSK_AVOID_TRANSIENTS       = "FSK_AVOID_TRANSIENTS";
    public static final String PROPERTY_NAME_CONFIDENCE_ALGO            = "CONFIDENCE_ALGO";
    public static final String PROPERTY_NAME_FSK_DEBUG                  = "FSK_DEBUG";
    
    /**
     * Returns confidence value [0.0 to 1.0]
     * @param fskPlan
     * @param samples
     * @param frame_nsamples
     * @param try_first_sample
     * @param try_max_nsamples
     * @param try_step_nsamples
     * @param try_confidence_search_limit
     * @param expect_bits_string
     * @return 
     */
    public static FrameInfo findFrame(FskPlan fskPlan, float samples, int frame_nsamples, int try_first_sample, int try_max_nsamples, 
                                      int try_step_nsamples, float try_confidence_search_limit, byte[] expect_bits_string) {
        int     expect_n_bits   = expect_bits_string.length;
        float   samples_per_bit = (float)frame_nsamples / expect_n_bits;

        // try_step_nsamples = 1; // pedantic TEST
        FrameInfo bestFrameInfo = new FrameInfo();

        // Scan the frame positions starting with the one try_first_sample,
        // alternating between a step above that, a step below that, above, below,
        // and so on, until we've scanned the whole try_max_nsamples range.
        FrameInfo currentFrameInfo = new FrameInfo();
        for (int j=0; ; j++) {
            int up = (j % 2) != 0 ? 1 : -1;
            int t = try_first_sample + up * ((j+1) / 2) * try_step_nsamples;

            if (t >= try_max_nsamples) {
                break;
            }

            if (t < 0) {
                continue;
            }

            currentFrameInfo.setConfidence(0.0f);
            currentFrameInfo.setAmpl(0.0f);
            currentFrameInfo.setBits(0);
            currentFrameInfo.setStartPos(t);

            //debug_log("try fsk_frame_analyze at t=%d\n", t);
            analyzeFrame(fskPlan, samples+t, samples_per_bit, expect_n_bits, expect_bits_string, currentFrameInfo);
            if (bestFrameInfo.getConfidence() < currentFrameInfo.getConfidence()) {
                bestFrameInfo.setStartPos(t);
                bestFrameInfo.setConfidence(currentFrameInfo.getConfidence());
                bestFrameInfo.setAmpl(currentFrameInfo.getAmpl());
                bestFrameInfo.setBits(currentFrameInfo.getBits());

                // If we find a frame with confidence > try_confidence_search_limit
                // quit searching.
                if (bestFrameInfo.getConfidence() >= try_confidence_search_limit) {
                    break;
                }
            }
        }

/*        
        #ifdef FSK_DEBUG
        unsigned char bitchar;
        // FIXME? hardcoded chop off framing bits for debug
        // Hmmm... we have now way to distinguish between:
        // 8-bit data with no start/stopbits == 8 bits
        // 5-bit with prevstop+start+stop == 8 bits
        switch ( expect_n_bits ) {
        case 11:	bitchar = ( *bits_outp >> 2 ) & 0xFF;
        break;
        case 8:
        default:
        bitchar = *bits_outp & 0xFF;
        break;
        }
        debug_log("FSK_FRAME bits='");
        for ( j=0; j<expect_n_bits; j++ )
        debug_log("%c", ( *bits_outp >> j ) & 1 ? '1' : '0' );
        debug_log("' datum='%c' (0x%02x) c=%f a=%f t=%d\n",
        isprint(bitchar)||isspace(bitchar) ? bitchar : '.',
        bitchar,
        confidence, best_a, best_t);
        #endif
*/        
        return currentFrameInfo;
    }
    
    public static int detectCarrier(FskPlan fskp, float samples, int nsamples, float min_mag_threshold ) {
        Validate.isTrue(nsamples <= fskp.getFftSize());

        int pa_nchannels = 1;

        // sets sample to fftin
        // bzero(fskp->fftin, (fskp.fftsize * sizeof(float) * pa_nchannels));
        // memcpy(fskp->fftin, samples, nsamples * sizeof(float));
        Arrays.fill(fskp.getFftIn(), 0.0f);
        Arrays.fill(fskp.getFftIn(), nsamples + 1, fskp.getFftIn().length - 1, 0.0f);

        fftwf_execute(fskp->fftplan);

        float magscalar = 1.0f / (nsamples/2.0f);
        float max_mag = 0.0f;
        int   max_mag_band = -1;
        int   i = 1;	/* start detection at the first non-DC band */
        int nbands = fskp.getNBands();

        if (isFskAutodetectMinFreq()) {
            i = Math.round((getFskMinFreq() + fskp.getBandWidth() / 2) / fskp.getBandWidth());
        }

        if (isFskAutodetectMaxFreq()) {
            nbands = Math.round((getFskMaxFreq() + fskp.getBandWidth() / 2) / fskp.getBandWidth());
            if ( nbands > fskp.getNBands()) {
                nbands = fskp.getNBands();
            }
        } 

        for ( ;i<nbands; i++ ) {
            float mag = bandMag(fskp.getFftOut(), i, magscalar);
            if ( mag < min_mag_threshold ) {
                continue;
            }
            if ( max_mag < mag ) {
            max_mag = mag;
            max_mag_band = i;
            }
        }
        
        if ( max_mag_band < 0 ) {
            return -1;
        }
        
        return max_mag_band;        
    }
    
    public void setTonesByBandShift(FskPlan fskp, int b_mark, int b_shift ) {
        Validate.isTrue(b_shift != 0);
        Validate.isTrue(b_mark < fskp.getNBands());
        int b_space = b_mark + b_shift;
        Validate.isTrue(b_space >= 0);
        Validate.isTrue(b_space < fskp.getNBands());
        fskp.setBMark(b_mark);
        fskp.setBSpace(b_space);
        fskp.setFMark(b_mark * fskp.getBandWidth());
        fskp.setFSpace(b_space * fskp.getBandWidth());
    }
    
    public static boolean isUseFft() {
        return BooleanUtils.toBoolean(System.getProperty(PROPERTY_NAME_USE_FFT));
    }
    
    private static void analyzeBit(FskPlan fskp, float samples, int bit_nsamples, BitInfo resultBitInfo) {
        // FIXME: Fast and loose ... don't bzero fftin, just assume its only ever
        // been used for bit_nsamples so the remainder is still zeroed. Sketchy.
        //
        // unsigned int pa_nchannels = 1; // FIXME
        // bzero(fskp->fftin, (fskp->fftsize * sizeof(float) * pa_nchannels));
        Arrays.fill(fskp.getFftIn(), 0, bit_nsamples, samples);
        
        float magscalar = 2.0f / bit_nsamples;
        
        fftwf_execute(fskp->fftplan);
        
        float mag_mark = bandMag(fskp.getFftOut(), fskp.getBMark(), magscalar);
        float mag_space = bandMag(fskp.getFftOut(), fskp.getBSpace(), magscalar);
        // mark==1, space==0
        if (mag_mark > mag_space) {
            resultBitInfo.setBit(1);
            resultBitInfo.setSignalMag(mag_mark);
            resultBitInfo.setNoiseMag(mag_space);
        } else {
            resultBitInfo.setBit(0);
            resultBitInfo.setSignalMag(mag_space);
            resultBitInfo.setNoiseMag(mag_mark);
        }
/*
        debug_log("\t%.2f %.2f %s bit=%u sig=%.2f noise=%.2f\n",
                  mag_mark, 
                  mag_space,
                  mag_mark > mag_space ? "mark " : " space",
                  *bit_outp, 
                  *bit_signal_mag_outp, 
                  *bit_noise_mag_outp);        
*/        
    }
    
    /**
     * Write the result of the frame analyzing to the <code>resultFrameInfo</code>
     * 
     * @param fskp
     * @param samples
     * @param smaples_per_bit
     * @param n_bits
     * @param expect_bits_string
     * @param resultFrameInfo 
     */
    private static float analyzeFrame(FskPlan fskp, float samples, float samples_per_bit, int n_bits, byte[] expect_bits, 
                                     FrameInfo resultFrameInfo) {

        int     bit_nsamples        = Math.round(samples_per_bit + 0.5f);
        BitInfo bits[]              = new BitInfo[32];
        int     bitnum              = 0;
        int	bit_begin_sample;

        // various deprecated noise limiter schemes:
        //#define FSK_MIN_BIT_SNR 1.4
        //#define FSK_MIN_MAGNITUDE 0.10
        //#define FSK_AVOID_TRANSIENTS 0.7

        /* pass #1 - process and check only the "required" (1/0) expect_bits */
        for (bitnum = 0; bitnum < n_bits; ++bitnum) {
            // expect_bits[bitnum] == 'd'
            char c = (char)(((expect_bits[bitnum] & 0x00FF) << 8) + (expect_bits[bitnum] & 0x00FF));
            if (c == 'd') {
                continue;
            }
            
            Validate.isTrue(expect_bits[bitnum] == 1 || expect_bits[bitnum] == 0);
            bit_begin_sample = Math.round(samples_per_bit * bitnum + 0.5f);
            
            //debug_log( " bit# %2u @ %7u: ", bitnum, bit_begin_sample);

            bits[bitnum].setBit(expect_bits[bitnum]);
            analyzeBit(fskp, samples+bit_begin_sample, bit_nsamples, bits[bitnum]);
            if (expect_bits[bitnum] != bits[bitnum].getBit()) {
                resultFrameInfo.setConfidence(0.0f);                
                return 0.0f; /* does not match expected; abort frame analysis. */
            }

            if (isFskMinBitSnr()) {
                float bit_snr = bits[bitnum].getSignalMag() / bits[bitnum].getNoiseMag();
                if (bit_snr < getFskMinBitSnr()) {
                    resultFrameInfo.setConfidence(0.0f);                
                    return 0.0f;
                }
            }
            
            if (isFskMinMagnitude()) {
                // Performance hack: reject frame early if sig mag isn't even half
                // of FSK_MIN_MAGNITUDE
                if ( bits[bitnum].getSignalMag() < getFskMinMagnitude() / 2.0f ) {
                    resultFrameInfo.setConfidence(0.0f);                
                    return 0.0f; // too weak; abort frame analysis
                }
            }
        }

        if (isFskAvoidTransients()) {
            // FIXME: fsk_frame_analyze shouldn't care about start/stop bits,
            // and this really is only correct for "10dd..dd1" format frames anyway:
            // FIXME: this is totally defective, if the checked bits weren't
            // even calculated in pass #1 (e.g. if there are no pass #1 expect bits).
            /* Compare strength of stop bit and start bit, to avoid detecting
            * a transient as a start bit, as often results in a single false
            * character when the mark "leader" tone begins. Require that the
            * diff between start bit and stop bit strength not be "large". */
            float s_mag = bits[1].getSignalMag(); // start bit
            float p_mag = bits[n_bits-1].getSignalMag(); // stop bit
            if (Math.abs(s_mag-p_mag) > (s_mag * getFskAvoidTransients()) ) {
                //debug_log(" avoid transient\n");
                resultFrameInfo.setConfidence(0.0f);                
                return 0.0f;
            }
        }

        /* pass #2 - process only the dontcare ('d') expect_bits */
        for (bitnum = 0; bitnum < n_bits; ++bitnum) {
            // expect_bits[bitnum] == 'd'
            char c = (char)(((expect_bits[bitnum] & 0x00FF) << 8) + (expect_bits[bitnum] & 0x00FF));
            if (c != 'd') {
                continue;
            }

            bit_begin_sample = Math.round(samples_per_bit * bitnum + 0.5f);

            // debug_log( " bit# %2u @ %7u: ", bitnum, bit_begin_sample);
            analyzeBit(fskp, samples+bit_begin_sample, bit_nsamples, bits[bitnum]);

            if (isFskMinBitSnr()) {
                float bit_snr = bits[bitnum].getSignalMag() / bits[bitnum].getNoiseMag();
                if (bit_snr < getFskMinBitSnr()) {
                    resultFrameInfo.setConfidence(0.0f);                
                    return 0.0f;
                }
            }
        }

        float confidence = 0.0f;
        if (isConfidenceAlgo()) {
            int confidenceAlgo = getConfidenceAlgo();

            if (confidenceAlgo == 5 || confidenceAlgo == 6) {
                float total_bit_sig     = 0.0f;
                float total_bit_noise   = 0.0f;
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
                    for (bitnum=0; bitnum < n_bits; ++bitnum) {
                        float avg_bit_sig_other = (total_bit_sig - Math.abs(bits[bitnum].getSignalMag())) / (n_bits - 1);
                        divergence += Math.abs(bits[bitnum].getSignalMag() - avg_bit_sig_other) / avg_bit_sig_other;
                    }
                    divergence *= 2;
                    divergence /= n_bits;
                } // if (confidenceAlgo == 6)
                
                if (isFskDebug()) {
                    float avg_bit_noise = total_bit_noise / n_bits;
                    String logMsg = String.format(" divg=%.3f snr=%.3f avg{bit_sig=%.3f bit_noise=%.3f(%s)}",
                                                  confidenceAlgo == 6 ? divergence : 0.0f,
                                                  snr, avg_bit_sig, avg_bit_noise,
                                                  avg_bit_noise == 0.0 ? "zero" : "non-zero");
                    System.out.println(logMsg);
                } // if (isFskDebug())
                
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
        resultFrameInfo.setBits(0);
        for ( bitnum=0; bitnum<n_bits; bitnum++ ) {
            int bits_outp = resultFrameInfo.getBits() | (bits[bitnum].getBit() << bitnum);
            resultFrameInfo.setBits(bits_outp);
        }
        
        String logMsg = String.format(" frame algo=%u confidence=%f ampl=%f\n", getConfidenceAlgo(), resultFrameInfo.getConfidence(), resultFrameInfo.getAmpl());
        Logger.getLogger(FskPlanUtils.class).finest(logMsg);
        
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
    
    private static boolean isFskDebug() {
        return BooleanUtils.toBoolean(System.getProperty(PROPERTY_NAME_FSK_DEBUG));
    }

    private static float bandMag(Complex[] cplx, int band, float scalar) {
        // float re = cplx[band].;
        // float im = cplx[band][1];
        // float mag = hypotf(re, im) * scalar;
        // return mag;
        
        return (float) (cplx[band].abs()*scalar);
    }

    private FskPlanUtils() {
    }
}

final class BitInfo {
    private int     bit;
    private float   signalMag;
    private float   noiseMag;
    
    public BitInfo() {
        this(0, 0, 0);
    }
    
    public BitInfo(int bit, int signalMag, int noiseMag) {
        this.bit        = bit;
        this.signalMag  = signalMag;
        this.noiseMag   = noiseMag;
    }
    
    public int getBit() {
        return bit;
    }
    
    public void setBit(int bit) {
        this.bit = bit;
    }
    
    public float getSignalMag() {
        return signalMag;
    }
    
    public void setSignalMag(float signalMag) {
        this.signalMag = signalMag;
    }
    
    public float getNoiseMag() {
        return noiseMag;
    }
    
    public void setNoiseMag(float noiseMag) {
        this.noiseMag = noiseMag;
    }
}