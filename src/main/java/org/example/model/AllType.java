package org.example.model;

public class AllType {
    public AllType(int info_int, byte info_tint, short info_sint) {
        this.info_int = info_int;
        this.info_tint = info_tint;
        this.info_sint = info_sint;
    }

    public int getInfo_int() {
        return info_int;
    }

    public void setInfo_int(int info_int) {
        this.info_int = info_int;
    }

    public byte getInfo_tint() {
        return info_tint;
    }

    public void setInfo_tint(byte info_tint) {
        this.info_tint = info_tint;
    }

    public short getInfo_sint() {
        return info_sint;
    }

    public void setInfo_sint(short info_sint) {
        this.info_sint = info_sint;
    }

    private int info_int;

    private byte info_tint;

    private short info_sint;
}
