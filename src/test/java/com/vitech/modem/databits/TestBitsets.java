package com.vitech.modem.databits;

import junit.framework.Assert;
import org.junit.Test;

import java.util.BitSet;

/**
 * Created by vic on 10/1/14.
 */
public class TestBitsets {
    private final String testString = "01234567890absdef";

    @Test
    public void ascii8(){
        IDatabits codec = new ASCII();
        testString.equals(new String(codec.decode(codec.encode(testString.getBytes()))));
    }

    @Test
    public void binary(){
        IDatabits codec = new Binary();
        String sample = "010101\n";
        sample.equals(new String(codec.decode(codec.encode(sample.getBytes()))));
    }

    @Test
    public void baudot(){
        IDatabits codec = new Baudot();
        testString.equals(new String(codec.decode(codec.encode(testString.getBytes()))));
    }
}
