package com.minicat.ws;

public abstract class WsConstants {

    //websocket opcode
    public static final byte OPCODE_CONTINUATION = 0x00;
    public static final byte OPCODE_TEXT = 0x01;
    public static final byte OPCODE_BINARY = 0x02;
    public static final byte OPCODE_CLOSE = 0x08;
    public static final byte OPCODE_PING = 0x09;
    public static final byte OPCODE_PONG = 0x0A;

    public static final String WS_SERVER_CONTAINER_ATTRIBUTE = "javax.websocket.server.ServerContainer";

}
