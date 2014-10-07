package com.vitech.modem;

import java.util.logging.Logger;

/**
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/fsk.c">source code</a>
 */
public class FSK {
    class FrameAnalysisInfo {
    }

    class BitAnalysisInfo {
    }

    private Logger log = Logger.getLogger(FSK.class.getName());

    public void findFrame(float confidenceSearchLimit, String expectBitsString) {}
    public int detectCarrier(float minMagnitudeThreshold) { return 0;   }

    private void setTonesByBandshift(int mark, int bandShift) {}
    private BitAnalysisInfo bitAnalyze() {  return null;    }
    private FrameAnalysisInfo frameAnalyze() {  return null;    }
}
