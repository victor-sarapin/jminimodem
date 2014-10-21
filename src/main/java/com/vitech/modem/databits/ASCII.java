package com.vitech.modem.databits;

import java.util.BitSet;

/**
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/databits_ascii.c">source code</a>
 */
public class ASCII implements IDatabits {

    @Override
    public int encode(byte[] source, byte[] encoded) {
        int encodedCount = 0;

        for (int i = 0; i < source.length; i++) {
            encoded[i] = source[i];
            encodedCount = i + 1;
        }

        return encodedCount;
    }

    @Override
    public int decode(byte[] source, byte[] decoded) {
        int decodedCount = 0;

        for (int i = 0; i < source.length; i++) {
            decoded[i] = (byte) (source[i] & 0xFF);
            decodedCount = i + 1;
        }

        return decodedCount;
    }
}
