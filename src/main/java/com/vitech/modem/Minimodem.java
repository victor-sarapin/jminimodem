package com.vitech.modem;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * Created by vic on 10/1/14.
 *
 * @see <a href="https://github.com/kamalmostafa/minimodem/blob/master/src/minimodem.c">source code</a>
 */
public class Minimodem {
    private static final Logger log = Logger.getLogger(Minimodem.class.getName());
    private boolean rxStop = false;
    FloatBuffer inputBuffer     = FloatBuffer.allocate(1024);
    ByteBuffer  outputBuffer    = ByteBuffer.allocate(1024);

    public void demodulate() {

        while(true){
            if(rxStop)
                break;

            //log.finest(String.format("advance=%u\\n", advance));

            if(inputBuffer.position() > inputBuffer.capacity()/2) {
                inputBuffer.compact();
                inputBuffer.flip();

                float[] chunk = new float[32];
                inputBuffer.put(chunk);
                //read data here
                inputBuffer.flip();
            }


        }

    }

    public void modulate() {

    }
}