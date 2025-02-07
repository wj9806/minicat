package io.github.wj9806.minicat.ws;

import javax.websocket.RemoteEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RemoteEndpointBase implements RemoteEndpoint {
    @Override
    public void setBatchingAllowed(boolean allowed) throws IOException {

    }

    @Override
    public boolean getBatchingAllowed() {
        return false;
    }

    @Override
    public void flushBatch() throws IOException {

    }

    @Override
    public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

    }

    @Override
    public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

    }
}
