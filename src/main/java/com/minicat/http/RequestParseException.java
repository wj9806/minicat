package com.minicat.http;

public class RequestParseException extends RuntimeException {

    public RequestParseException(String message) {
        super(message);
    }

    public RequestParseException() {
    }

    public RequestParseException(Throwable cause) {
        super(cause);
    }
}
