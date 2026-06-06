package com.nhahang.quan.ly.mon.an.dto;

public class ThongKeHomNayDTO {

    private long doanhThu;
    private int soDon;

    public ThongKeHomNayDTO(long doanhThu, int soDon) {
        this.doanhThu = doanhThu;
        this.soDon = soDon;
    }

    public long getDoanhThu() {
        return doanhThu;
    }

    public void setDoanhThu(long doanhThu) {
        this.doanhThu = doanhThu;
    }

    public int getSoDon() {
        return soDon;
    }

    public void setSoDon(int soDon) {
        this.soDon = soDon;
    }
}