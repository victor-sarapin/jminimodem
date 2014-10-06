package com.vitech.modem;

public final class FrameInfo {
    private float confidence;
    private int   bits;
    private float ampl;
    private int   startPos;
    
    public FrameInfo() {
        this(0.0f, 0, 0.0f, 0);
    }
    
    public FrameInfo(float confidence, int bits, float ampl, int startPos) {
        this.confidence = confidence;
        this.bits       = bits;
        this.ampl       = ampl;
        this.startPos   = startPos;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public int getBits() {
        return bits;
    }
    
    public void setBits(int bits) {
        this.bits = bits;
    }
    
    public float getAmpl() {
        return ampl;
    }
    
    public void setAmpl(float ampl) {
        this.ampl = ampl;
    }
    
    public int getStartPos() {
        return startPos;
    }
    
    public void setStartPos(int startPos) {
        this.startPos = startPos;
    }
}
