package com.minicat.http;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RequestInputStream extends ServletInputStream {
    private final InputStream inputStream;
    private boolean finished;
    private ReadListener readListener;

    public RequestInputStream(byte[] data) {
        this.inputStream = new ByteArrayInputStream(data);
        this.finished = false;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isReady() {
        try {
            return inputStream.available() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        this.readListener = readListener;
        try {
            if (!isFinished() && isReady()) {
                readListener.onDataAvailable();
            }
        } catch (IOException e) {
            readListener.onError(e);
        }
    }

    @Override
    public int read() throws IOException {
        int data = inputStream.read();
        if (data == -1) {
            finished = true;
            if (readListener != null) {
                readListener.onAllDataRead();
            }
        }
        return data;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = inputStream.read(b, off, len);
        if (result == -1) {
            finished = true;
            if (readListener != null) {
                readListener.onAllDataRead();
            }
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        finished = true;
    }
}
