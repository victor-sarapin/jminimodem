package com.vitech.modem.databits;

import java.util.BitSet;

/**
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/databits_ascii.c">source code</a>
 */
public class ASCII implements IDatabits {
    @Override
    public BitSet encode(byte[] data) {
        return BitSet.valueOf(data);
    }

    @Override
    public byte[] decode(BitSet bits) {
        return bits.toByteArray();
    }
}
