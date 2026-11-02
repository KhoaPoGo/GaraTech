package entity;

import java.time.LocalDateTime;

public class PhieuSuaChua {
    private String maPhieuSC;
    private String maPhieuTN;
    private String maKTV;
    private String maNVLap;
    private LocalDateTime thoiGianDuKien;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianHoanThanh;
    private String ghiChuKyThuat;
    private String lyDoHuy;
    private String trangThai;

    public PhieuSuaChua() {
    }

    public PhieuSuaChua(String maPhieuSC, String maPhieuTN, String maKTV, String maNVLap,
                        LocalDateTime thoiGianDuKien, LocalDateTime thoiGianBatDau, LocalDateTime thoiGianHoanThanh,
                        String ghiChuKyThuat, String lyDoHuy, String trangThai) {
        this.maPhieuSC = maPhieuSC;
        this.maPhieuTN = maPhieuTN;
        this.maKTV = maKTV;
        this.maNVLap = maNVLap;
        this.thoiGianDuKien = thoiGianDuKien;
        this.thoiGianBatDau = thoiGianBatDau;
        this.thoiGianHoanThanh = thoiGianHoanThanh;
        this.ghiChuKyThuat = ghiChuKyThuat;
        this.lyDoHuy = lyDoHuy;
        this.trangThai = trangThai;
    }

    // Constructor kế thừa cũ để tương thích
    public PhieuSuaChua(String maPhieuSuaChua, LocalDateTime thoiGianDuKien, LocalDateTime thoiGianBatDau,
                        LocalDateTime thoiGianHoanThanh, String ghiChuKyThuat, String trangThai) {
        this(maPhieuSuaChua, null, null, null, thoiGianDuKien, thoiGianBatDau, thoiGianHoanThanh, ghiChuKyThuat, null, trangThai);
    }

    public String getMaPhieuSC() {
        return maPhieuSC;
    }

    public void setMaPhieuSC(String maPhieuSC) {
        this.maPhieuSC = maPhieuSC;
    }

    // Tương thích tên cũ
    public String getMaPhieuSuaChua() {
        return maPhieuSC;
    }

    public void setMaPhieuSuaChua(String maPhieuSuaChua) {
        this.maPhieuSC = maPhieuSuaChua;
    }

    public String getMaPhieuTN() {
        return maPhieuTN;
    }

    public void setMaPhieuTN(String maPhieuTN) {
        this.maPhieuTN = maPhieuTN;
    }

    public String getMaKTV() {
        return maKTV;
    }

    public void setMaKTV(String maKTV) {
        this.maKTV = maKTV;
    }

    public String getMaNVLap() {
        return maNVLap;
    }

    public void setMaNVLap(String maNVLap) {
        this.maNVLap = maNVLap;
    }

    public LocalDateTime getThoiGianDuKien() {
        return thoiGianDuKien;
    }

    public void setThoiGianDuKien(LocalDateTime thoiGianDuKien) {
        this.thoiGianDuKien = thoiGianDuKien;
    }

    public LocalDateTime getThoiGianBatDau() {
        return thoiGianBatDau;
    }

    public void setThoiGianBatDau(LocalDateTime thoiGianBatDau) {
        this.thoiGianBatDau = thoiGianBatDau;
    }

    public LocalDateTime getThoiGianHoanThanh() {
        return thoiGianHoanThanh;
    }

    public void setThoiGianHoanThanh(LocalDateTime thoiGianHoanThanh) {
        this.thoiGianHoanThanh = thoiGianHoanThanh;
    }

    public String getGhiChuKyThuat() {
        return ghiChuKyThuat;
    }

    public void setGhiChuKyThuat(String ghiChuKyThuat) {
        this.ghiChuKyThuat = ghiChuKyThuat;
    }

    public String getLyDoHuy() {
        return lyDoHuy;
    }

    public void setLyDoHuy(String lyDoHuy) {
        this.lyDoHuy = lyDoHuy;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public void taoPhieu() {
    }

    public void phanCongKTV() {
    }

    public void capNhatTienDo() {
    }

    public void danhGiaChatLuong() {
    }
}
