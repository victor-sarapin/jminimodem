package com.vitech.modem.databits;

import java.util.BitSet;

/**
 * Caller-ID (USA SDMF/MDMF) databits decoder
 *
 * Not supported so far.
 *
 * @see <a href = "http://melabs.com/resources/callerid.htm">reference</a>
 * @see <a href = "https://github.com/kamalmostafa/minimodem/blob/master/src/databits_callerid.c">source code</a>
 */
public class CallerID implements IDatabits {

    public static final int CID_MSG_MDMF = 0x80;
    public static final int CID_MSG_SDMF = 0x04;

    public static final int CID_DATA_DATETIME = 0x01;
    public static final int  CID_DATA_PHONE = 0x02;
    public static final int  CID_DATA_PHONE_NA = 0x04;
    public static final int  CID_DATA_NAME = 0x07;
    public static final int  CID_DATA_NAME_NA = 0x08;

    public static final String[] cid_datatype_names = {
        "unknown0:",
        "Time:",
        "Phone:",
        "unknown3:",
        "Phone:",
        "unknown5:",
        "unknown6:",
        "Name:",
        "Name:"
    };

    private int cid_msgtype = 0;
    private int cid_ndata = 0;
    private byte[] cid_buf = new byte[256];

    @Override
    public BitSet encode(byte[] data) {
        throw new IllegalStateException("Not implemented!");
    }

    private void decodeCidReset() {
        cid_msgtype = 0;
        cid_ndata = 0;
    }

    @Override
    public byte[] decode(BitSet bits) {
        throw new IllegalStateException("Not implemented!");
    }
}
