package com.vitech.modem.databits;

import java.util.BitSet;

/**
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/databits_binary.c">source code</a>
 */
public class Binary implements IDatabits {
    @Override
    public int encode(byte[] source, byte[] encoded) {
        throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public int decode(byte[] source, byte[] decoded) {
        throw new IllegalStateException("Not implemented yet");
    }
}
