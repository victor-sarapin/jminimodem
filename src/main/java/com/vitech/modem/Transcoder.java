package com.vitech.modem;

import java.nio.ByteBuffer;

/**
 * Created by vic on 10/3/14.
 */
public abstract class Transcoder implements Runnable {
    private ByteBuffer  inputBuffer;
    private ByteBuffer  outputBuffer;

    public ByteBuffer getInputBuffer() {
        return inputBuffer;
    }

    public void setInputBuffer(ByteBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
    }

    public ByteBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public void setOutputBuffer(ByteBuffer outputBuffer) {
        this.outputBuffer = outputBuffer;
    }
}