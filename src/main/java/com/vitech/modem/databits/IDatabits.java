package com.vitech.modem.databits;

import java.util.BitSet;

/**
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/databits.h">source code</a>
 */
public interface IDatabits {
    public int encode(byte[] source, byte[] encoded);
    public int decode(byte[] source, byte[] decoded);
}
