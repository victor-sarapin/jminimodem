package com.vitech.modem;

import org.apache.commons.math3.complex.Complex;

public final class FskPlan {
    private final float sample_rate;
    private float f_mark;
    private float f_space;
    private final float filter_bw;
    
    // following field is used by FFT
    private int     fftsize;
    private int     nbands;
    private float   band_width;
    private int     b_mark;
    private int     b_space;
    //fftwf_plan          fftplan;
    private float       fftin[];
    private Complex[]   fftout;

    public FskPlan(float sample_rate, float f_mark, float f_space, float filter_bw, boolean useFft) {
        this.sample_rate    = sample_rate;
        this.f_mark         = f_mark;
        this.f_space        = f_space;
        this.filter_bw      = filter_bw;
        
        if (useFft) {
            preparePlanToUseFft(sample_rate, f_mark, f_space, filter_bw);
        }
    }
    
    public int getFftSize() {
        return fftsize;
    }
    
    public int getNBands() {
        return nbands;
    }
    
    public float getBandWidth() {
        return band_width;
    }
    
    public Complex[] getFftOut() {
        return fftout;
    }
    
    public float[] getFftIn() {
        return fftin;
    }
    
    public void setBMark(int bMark) {
        this.b_mark = bMark;
    }
    
    public int getBMark() {
        return b_mark;
    }
    
    public void setBSpace(int bSpace) {
        this.b_space = bSpace;
    }
    
    public int getBSpace() {
        return b_space;
    }
    
    public void setFMark(float fMark) {
        this.f_mark = fMark;
    }
    
    public void setFSpace(float fSpace) {
        this.f_space = fSpace;
    }
    
    private void preparePlanToUseFft(float sample_rate, float f_mark, float f_space, float filter_bw) {
        band_width = filter_bw;
        float fft_half_bw = band_width / 2.0f;

        fftsize = Math.round((sample_rate + fft_half_bw) / band_width);
        nbands  = fftsize / 2 + 1;
        b_mark  = Math.round((f_mark + fft_half_bw) / band_width);
        b_space = Math.round((f_space + fft_half_bw) / band_width);

        if ( b_mark >= nbands || b_space >= nbands ) {
            String errorMessage = String.format("b_mark=%d or b_space=%d is invalid (nbands=%d)", b_mark, b_space, nbands);
            throw new IllegalArgumentException(errorMessage);
        }

        //debug_log("### b_mark=%u b_space=%u fftsize=%u\n", fskp->b_mark, fskp->b_space, fskp->fftsize);

        // FIXME:
        int pa_nchannels = 1;

        // FIXME check these:
        fftin   = new float[fftsize * pa_nchannels]; // fftwf_malloc(fskp->fftsize * sizeof(float) * pa_nchannels);
        fftout  = new Complex[nbands*pa_nchannels];


//      complex fftw plan, works for N channels:
//        fftplan = fftwf_plan_many_dft_r2c(
//                                /*rank*/1, &fskp->fftsize, /*howmany*/pa_nchannels,
//                                fskp->fftin, NULL, /*istride*/pa_nchannels, /*idist*/1,
//                                fskp->fftout, NULL, /*ostride*/1, /*odist*/fskp->nbands,
//                                FFTW_ESTIMATE);
//        if ( !fskp->fftplan ) {
//            fprintf(stderr, "fftwf_plan_dft_r2c_1d() failed\n");
//            fftwf_free(fskp->fftin);
//            fftwf_free(fskp->fftout);
//            free(fskp);
//            errno = EINVAL;
//            return NULL;
//        }            
    }
}
