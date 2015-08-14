package no.dependent_implementation.utils;

import java.io.IOException;
import java.io.OutputStream;

public class ProxyOutputStream extends OutputStream {
    private OutputStream target=null;
    public void write(int b) throws IOException {
        if(target!=null) target.write(b);
    }
    public void write(byte b[]) throws IOException {
        if(target!=null) target.write(b);
    }
    public void write(byte b[], int off, int len) throws IOException {
        if(target!=null) target.write(b,off,len);
    }
    public void flush() throws IOException {
        if(target!=null) target.flush();
    }

    public void close() throws IOException {
        if(target!=null) target.close();
        target=null;
    }

    public void setTarget(OutputStream target){
        this.target=target;
    }
}