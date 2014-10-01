package com.vitech.modem.databits;

import java.util.BitSet;

/**
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/databits_binary.c">source code</a>
 */
public class Binary implements IDatabits {
    @Override
    public BitSet encode(byte[] data) {
        assert data[data.length - 1] == '\n';

        BitSet result = new BitSet();

        for(int i = 0 ; i < data.length - 1; i++) {
            result.set(i, data[i] == '1');
        }

        return result;
    }

    @Override
    public byte[] decode(BitSet bits) {
        byte[] result = new byte[bits.size() + 1];

        byte i;
        for(i = 0; i < result.length - 1; i++) {
            int val = ((bits.get(i) ? '1' : '0') >> i & 1) + '0';
            result[i] = (byte) val;
        }

        result[i] = '\n';

        return result;
    }
}
