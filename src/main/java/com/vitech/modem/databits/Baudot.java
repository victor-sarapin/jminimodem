package com.vitech.modem.databits;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Baudot 5-bit data databits decoder/encoder
 *
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/databits_baudot.c">source code</a>
 */
public class Baudot implements IDatabits {
    private static Logger logger = Logger.getLogger(Baudot.class.getName());

    private static byte[][]
        baudot_decode_table = new byte[][]{
        // letter, U.S. figs, CCITT No.2 figs (Europe)
        { '_', '^', '^' },	// NUL (underscore and caret marks for debugging)
        { 'E', '3', '3' },
        { 0xA, 0xA, 0xA },	// LF
        { 'A', '-', '-' },
        { ' ', ' ', ' ' },	// SPACE
        { 'S', 0x7, '\'' },	// BELL or apostrophe
        { 'I', '8', '8' },
        { 'U', '7', '7' },

        { 0xD, 0xD, 0xD },	// CR
        { 'D', '$', '^' },	// '$' or ENQ
        { 'R', '4', '4' },
        { 'J', '\'', 0x7 },	// apostrophe or BELL
        { 'N', ',', ',' },
        { 'F', '!', '!' },
        { 'C', ':', ':' },
        { 'K', '(', '(' },

        { 'T', '5', '5' },
        { 'Z', '"', '+' },
        { 'L', ')', ')' },
        { 'W', '2', '2' },
        { 'H', '#', '%' },	// '#' or British pounds symbol	// FIXME
        { 'Y', '6', '6' },
        { 'P', '0', '0' },
        { 'Q', '1', '1' },

        { 'O', '9', '9' },
        { 'B', '?', '?' },
        { 'G', '&', '&' },
        { '%', '%', '%' },	// FIGS (symbol % for debug; won't be printed)
        { 'M', '.', '.' },
        { 'X', '/', '/' },
        { 'V', ';', '=' },
        { '%', '%', '%' },	// LTRS (symbol % for debug; won't be printed)
    };

    private static byte[][]
    baudot_encode_table = new byte[][] {
        // index: ascii char; values: bits, ltrs_or_figs_or_neither_or_both

  /* 0x00 */
    /* NUL */	{ 0x00, 3 },	// NUL
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* BEL */	{ 0x05, 2 },	// BELL (or CCITT2 apostrophe)
    /* BS */	{ 0, 0 },	// non-encodable (FIXME???)
    /* xxx */	{ 0, 0 },	// non-encodable
    /* LF */	{ 0x02, 3 },	// LF
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* 0xD */	{ 0x08, 3 },	// CR
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable

  /* 0x10 */
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable
    /* xxx */	{ 0, 0 },	// non-encodable

  /* 0x20 */
    /*   */	{ 0x04, 3 },	// SPACE
    /* ! */	{ 0x0d, 2 },	//
    /* " */	{ 0x11, 2 },	//
    /* # */	{ 0x14, 2 },	// '#' (or CCITT2 British pounds symbol)
    /* $ */	{ 0x09, 2 },	// '$' (or CCITT2 ENQ)
    /* % */	{ 0, 0 },	// non-encodable
    /* & */	{ 0x1a, 2 },	//
    /* ' */	{ 0x0b, 2 },	// apostrophe (or CCITT2 BELL)
    /* ( */	{ 0x0f, 2 },	//
    /* ) */	{ 0x12, 2 },	//
    /* * */	{ 0, 0 },	// non-encodable
    /* + */	{ 0x12, 2 },	//
    /* , */	{ 0x0c, 2 },	//
    /* - */	{ 0x03, 2 },	//
    /* . */	{ 0x1c, 2 },	//
    /* / */	{ 0x1d, 2 },	//

  /* 0x30 */
    /* 0 */	{ 0x16, 2 },	//
    /* 1 */	{ 0x17, 2 },	//
    /* 2 */	{ 0x13, 2 },	//
    /* 3 */	{ 0x01, 2 },	//
    /* 4 */	{ 0x0a, 2 },	//
    /* 5 */	{ 0x10, 2 },	//
    /* 6 */	{ 0x15, 2 },	//
    /* 7 */	{ 0x07, 2 },	//
    /* 8 */	{ 0x06, 2 },	//
    /* 9 */	{ 0x18, 2 },	//
    /* : */	{ 0x0e, 2 },	//
    /* ; */	{ 0x1e, 2 },	//
    /* < */	{ 0, 0 },	// non-encodable
    /* = */	{ 0, 0 },	// non-encodable
    /* > */	{ 0, 0 },	// non-encodable
    /* ? */	{ 0x19, 2 },	//

  /* 0x40 */
    /* @ */	{ 0, 0 },	// non-encodable
    /* A */	{ 0x03, 1 },	//
    /* B */	{ 0x19, 1 },	//
    /* C */	{ 0x0e, 1 },	//
    /* D */	{ 0x09, 1 },	//
    /* E */	{ 0x01, 1 },	//
    /* F */	{ 0x0d, 1 },	//
    /* G */	{ 0x1a, 1 },	//
    /* H */	{ 0x14, 1 },	//
    /* I */	{ 0x06, 1 },	//
    /* J */	{ 0x0b, 1 },	//
    /* K */	{ 0x0f, 1 },	//
    /* L */	{ 0x12, 1 },	//
    /* M */	{ 0x1c, 1 },	//
    /* N */	{ 0x0c, 1 },	//
    /* O */	{ 0x18, 1 },	//

  /* 0x50 */
    /* P */	{ 0x16, 1 },	//
    /* Q */	{ 0x17, 1 },	//
    /* R */	{ 0x0a, 1 },	//
    /* S */	{ 0x05, 1 },	//
    /* T */	{ 0x10, 1 },	//
    /* U */	{ 0x07, 1 },	//
    /* V */	{ 0x1e, 1 },	//
    /* W */	{ 0x13, 1 },	//
    /* X */	{ 0x1d, 1 },	//
    /* Y */	{ 0x15, 1 },	//
    /* Z */	{ 0x11, 1 },	//
    /* [ */	{ 0, 0 },	// non-encodable
    /* \\ */	{ 0, 0 },	// non-encodable
    /* ] */	{ 0, 0 },	// non-encodable
    /* ^ */	{ 0, 0 },	// non-encodable
    /* _ */	{ 0, 0 },	// non-encodable

    };

    private static final int BAUDOT_LTRS = 0x1F;
    private static final int BAUDOT_FIGS = 0x1B;
    private static final int BAUDOT_SPACE = 0x04;


    /*
     * 0 unknown state
     * 1 LTRS state
     * 2 FIGS state
     */
    static int baudot_charset = 0;		// FIXME

    private void baudot_reset()
    {
        baudot_charset = 1;
    }

    private byte baudot_decode(byte[] databits)
    {
        /* Baudot (RTTY) */
        assert( (databits[0] & ~0x1F) == 0 );

        int stuff_char = 1;

        if ( databits[0] == BAUDOT_FIGS ) {
            baudot_charset = 2;
            stuff_char = 0;
        } else if ( databits[0] == BAUDOT_LTRS ) {
            baudot_charset = 1;
            stuff_char = 0;
        } else if ( databits[0] == BAUDOT_SPACE ) {	/* RX un-shift on space */
            baudot_charset = 1;
        }

        int t = ( baudot_charset == 1 ) ? 0 : 1;
            return baudot_decode_table[databits[0]][t];
    }


    private void baudot_skip_warning( byte char_out )
    {
        System.err.printf("W: baudot skipping non-encodable character '%c' 0x%02x", char_out);
    }

    /*
     * Returns the number of 5-bit data words stuffed into *databits_outp (1 or 2)
     */
    private byte[] baudot_encode(byte char_out)
    {
        byte[] databits_outp = new byte[2];

        char_out = (byte) Character.toUpperCase(char_out);

        if( char_out >= 0x60 || char_out < 0 ) {
            baudot_skip_warning(char_out);
            return databits_outp;
        }

        byte ind = char_out;

        int n = 0;

        byte charset_mask = baudot_encode_table[ind][1];

        logger.finest(new MessageFormat("\"I: (baudot_charset==%u)   input character '%c' 0x%02x charset_mask=%u\\n\"").format(new Object[]{baudot_charset, char_out, char_out, charset_mask}));

        if ( (baudot_charset & charset_mask ) == 0 ) {
            if ( charset_mask == 0 ) {
                baudot_skip_warning(char_out);
                assert false;
            }

            if ( baudot_charset == 0 )
                baudot_charset = 1;

            if ( charset_mask != 3 )
                baudot_charset = charset_mask;

            if ( baudot_charset == 1 )
                databits_outp[n++] = BAUDOT_LTRS;
            else if ( baudot_charset == 2 )
                databits_outp[n++] = BAUDOT_FIGS;
            else
                assert false;

            debug_log("I: emit charset select 0x%02X\n", databits_outp[n-1]);
        }

        if ( !( baudot_charset == 1 || baudot_charset == 2 ) ) {
            System.err.printf("E: baudot input character failed '%c' 0x%02x\n", char_out, char_out);
            System.err.printf("E: baudot_charset==%u\n", baudot_charset);
            assert false;
        }

        databits_outp[n++] = baudot_encode_table[ind][0];

    /* TX un-shift on space */
        if ( char_out == ' ' )
            baudot_charset = 1;

        return databits_outp;
    }

    private void debug_log(String s, byte b) {
        logger.finest(new MessageFormat(s).format(new Object[]{b}));
    }

    @Override
    public BitSet encode(byte[] data) {
        byte[] raw = new byte[data.length*2];

        for(int i = 0; i < data.length; i++) {
            byte[] encoded = baudot_encode(data[i]);
            raw[i*2] = encoded[0];
            raw[i*2+1] = encoded[1];
        }

        return BitSet.valueOf(raw);
    }

    @Override
    public byte[] decode(BitSet bits) {
        byte[] raw = bits.toByteArray();
        byte[] result = new byte[raw.length/2 + 1];

        for(int i = 0; i < result.length; i++) {
            result[i] = baudot_decode(Arrays.copyOfRange(raw, i*2, i*2 + 2));
        }

        return result;
    }
}
