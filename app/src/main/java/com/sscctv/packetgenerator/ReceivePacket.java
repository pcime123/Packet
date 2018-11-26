package com.sscctv.packetgenerator;

public class ReceivePacket {


    private String num;
    private String time;
    private String source;
    private String dest;
    private String protocol;
    private String length;
    private String data;

    ReceivePacket(String num, String time, String source, String dest, String protocol,String length, String data) {
        this.num = num;
        this.time = time;
        this.source = source;
        this.dest = dest;
        this.protocol = protocol;
        this.length = length;
        this.data = data;
    }

    public String getNum() {
        return num;
    }

    public String getTime() {
        return time;
    }

    public String getSource() {
        return source;
    }

    public String getDest() {
        return dest;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getLength() {
        return length;
    }

    public String getData() {
        return data;
    }


}
