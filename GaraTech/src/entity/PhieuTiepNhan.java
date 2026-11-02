package entity;

import java.time.LocalDateTime;

public class PhieuTiepNhan {
    private String maPhieuTN;
    private Xe xe;
    private KhachHang khachHang;
    private NhanVien nhanVien;
    private LocalDateTime thoiGianTiepNhan;
    private LocalDateTime thoiGianHenTra;
    private String tinhTrangBanDau;
    private String yeuCauKhachHang;
    private float mucNhienLieu;
    private float soKmTiepNhan;
    private String trangThai;

    public PhieuTiepNhan() {
    }

    public PhieuTiepNhan(String maPhieuTN, Xe xe, KhachHang khachHang, NhanVien nhanVien,
                         LocalDateTime thoiGianTiepNhan, LocalDateTime thoiGianHenTra,
                         String tinhTrangBanDau, String yeuCauKhachHang,
                         float mucNhienLieu, float soKmTiepNhan, String trangThai) {
        this.maPhieuTN = maPhieuTN;
        this.xe = xe;
        this.khachHang = khachHang;
        this.nhanVien = nhanVien;
        this.thoiGianTiepNhan = thoiGianTiepNhan;
        this.thoiGianHenTra = thoiGianHenTra;
        this.tinhTrangBanDau = tinhTrangBanDau;
        this.yeuCauKhachHang = yeuCauKhachHang;
        this.mucNhienLieu = mucNhienLieu;
        this.soKmTiepNhan = soKmTiepNhan;
        this.trangThai = trangThai;
    }

    public String getMaPhieuTN() {
        return maPhieuTN;
    }

    public void setMaPhieuTN(String maPhieuTN) {
        this.maPhieuTN = maPhieuTN;
    }

    public Xe getXe() {
        return xe;
    }

    public void setXe(Xe xe) {
        this.xe = xe;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    public LocalDateTime getThoiGianTiepNhan() {
        return thoiGianTiepNhan;
    }

    public void setThoiGianTiepNhan(LocalDateTime thoiGianTiepNhan) {
        this.thoiGianTiepNhan = thoiGianTiepNhan;
    }

    public LocalDateTime getThoiGianHenTra() {
        return thoiGianHenTra;
    }

    public void setThoiGianHenTra(LocalDateTime thoiGianHenTra) {
        this.thoiGianHenTra = thoiGianHenTra;
    }

    public String getTinhTrangBanDau() {
        return tinhTrangBanDau;
    }

    public void setTinhTrangBanDau(String tinhTrangBanDau) {
        this.tinhTrangBanDau = tinhTrangBanDau;
    }

    public String getYeuCauKhachHang() {
        return yeuCauKhachHang;
    }

    public void setYeuCauKhachHang(String yeuCauKhachHang) {
        this.yeuCauKhachHang = yeuCauKhachHang;
    }

    public float getMucNhienLieu() {
        return mucNhienLieu;
    }

    public void setMucNhienLieu(float mucNhienLieu) {
        this.mucNhienLieu = mucNhienLieu;
    }

    public float getSoKmTiepNhan() {
        return soKmTiepNhan;
    }

    public void setSoKmTiepNhan(float soKmTiepNhan) {
        this.soKmTiepNhan = soKmTiepNhan;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public void capNhatTrangThai(String trangThaiMoi) {
        this.trangThai = trangThaiMoi;
    }
}
