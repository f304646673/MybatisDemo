package org.example.model;

public class AllTypeEnum {
    public int getIntInfo() {
        return intInfo;
    }

    public void setIntInfo(int intInfo) {
        this.intInfo = intInfo;
    }

    public byte getByteInfo() {
        return byteInfo;
    }

    public void setByteInfo(byte byteInfo) {
        this.byteInfo = byteInfo;
    }

    public UdEnum getShortInfo() {
        return shortInfo;
    }

    public void setShortInfo(UdEnum shortInfo) {
        this.shortInfo = shortInfo;
    }

    private int intInfo;
    private byte byteInfo;
    private UdEnum shortInfo;
}
