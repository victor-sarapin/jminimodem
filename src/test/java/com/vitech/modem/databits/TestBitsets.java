package com.vitech.modem.databits;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Created by vic on 10/1/14.
 */
public class TestBitsets {
    private final String testString = "01234567890 absdef";

    @Test
    public void ascii8(){
        IDatabits codec = new ASCII();
        byte[]  encodedBytes = new byte[1024];
        byte[]  decodedBytes = new byte[1024];
        int encoded = codec.encode(testString.getBytes(), encodedBytes);
        encodedBytes = Arrays.copyOfRange(encodedBytes, 0, encoded);
        int decoded = codec.decode(encodedBytes, decodedBytes);
        Assert.assertEquals(testString, new String(decodedBytes, 0, decoded));
    }

    @Test
    public void binary(){
        IDatabits codec = new Binary();
        String sample = "010101\n";
        byte[]  encodedBytes = new byte[1024];
        byte[]  decodedBytes = new byte[1024];
        int encoded = codec.encode(testString.getBytes(), encodedBytes);
        encodedBytes = Arrays.copyOfRange(encodedBytes, 0, encoded);
        int decoded = codec.decode(encodedBytes, decodedBytes);
        Assert.assertEquals(testString, new String(decodedBytes, 0, decoded));
    }

    @Test
    public void baudot(){
        IDatabits codec = new Baudot();
        byte[]  encodedBytes = new byte[1024];
        byte[]  decodedBytes = new byte[1024];
        int encoded = codec.encode(testString.getBytes(), encodedBytes);
        encodedBytes = Arrays.copyOfRange(encodedBytes, 0, encoded);
        int decoded = codec.decode(encodedBytes, decodedBytes);
        Assert.assertEquals(testString, new String(decodedBytes, 0, decoded));
    }
}
