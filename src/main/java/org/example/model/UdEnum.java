package org.example.model;

public enum UdEnum {
    OPEN((short) 0),
    CLOSING((short) 1),
    CLOSED((short) 2);

    private final short code;

    UdEnum(short code) {
        this.code = code;
    }
}
