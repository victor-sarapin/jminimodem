package com.vitech.modem;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by vic on 10/2/14.
 */
public interface ITranscoder {
    public InputStream getInputStream();
    public void setInputStream(InputStream stream);

    public OutputStream getOutputStream();
    public void setOutputStream(OutputStream stream);
}
