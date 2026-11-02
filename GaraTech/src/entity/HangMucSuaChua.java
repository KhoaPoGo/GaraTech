package entity;

import java.time.LocalDateTime;

public class HangMucSuaChua {
    private String maHangMuc;
    private String maPhieuSC;
    private String loaiHangMuc; // "DichVu" hoặc "PhuTung"
    private String maDichVu;
    private String maPhuTung;
    private String tenHangMuc;
    private int soLuong;
    private double donGia;
    private double mucGiamGia;
    private double thanhTien;
    private boolean laPhatSinh;
    private LocalDateTime thoiHanBaoHanh;
    private String trangThai; // "Chờ duyệt", "Đã xác nhận", "Từ chối", "Hoàn thành"
    private String ghiChu;

    public HangMucSuaChua() {
    }

    public HangMucSuaChua(String maHangMuc, String maPhieuSC, String loaiHangMuc, String maDichVu, String maPhuTung,
                          String tenHangMuc, int soLuong, double donGia, double mucGiamGia, double thanhTien,
                          boolean laPhatSinh, LocalDateTime thoiHanBaoHanh, String trangThai, String ghiChu) {
        this.maHangMuc = maHangMuc;
        this.maPhieuSC = maPhieuSC;
        this.loaiHangMuc = loaiHangMuc;
        this.maDichVu = maDichVu;
        this.maPhuTung = maPhuTung;
        this.tenHangMuc = tenHangMuc;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.mucGiamGia = mucGiamGia;
        this.thanhTien = thanhTien;
        this.laPhatSinh = laPhatSinh;
        this.thoiHanBaoHanh = thoiHanBaoHanh;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
    }

    // Constructor cũ để tương thích
    public HangMucSuaChua(String maHangMuc, String loaiHangMuc, String tenHangMuc, int soLuong, double donGia,
                          float mucGiamGia, double thanhTien, LocalDateTime thoiHanBaoHanh, String trangThai, String ghiChu) {
        this(maHangMuc, null, loaiHangMuc, null, null, tenHangMuc, soLuong, donGia, mucGiamGia, thanhTien, false, thoiHanBaoHanh, trangThai, ghiChu);
    }

    public String getMaHangMuc() {
        return maHangMuc;
    }

    public void setMaHangMuc(String maHangMuc) {
        this.maHangMuc = maHangMuc;
    }

    public String getMaPhieuSC() {
        return maPhieuSC;
    }

    public void setMaPhieuSC(String maPhieuSC) {
        this.maPhieuSC = maPhieuSC;
    }

    public String getLoaiHangMuc() {
        return loaiHangMuc;
    }

    public void setLoaiHangMuc(String loaiHangMuc) {
        this.loaiHangMuc = loaiHangMuc;
    }

    public String getMaDichVu() {
        return maDichVu;
    }

    public void setMaDichVu(String maDichVu) {
        this.maDichVu = maDichVu;
    }

    public String getMaPhuTung() {
        return maPhuTung;
    }

    public void setMaPhuTung(String maPhuTung) {
        this.maPhuTung = maPhuTung;
    }

    public String getTenHangMuc() {
        return tenHangMuc;
    }

    public void setTenHangMuc(String tenHangMuc) {
        this.tenHangMuc = tenHangMuc;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
        tinhThanhTien();
    }

    public double getDonGia() {
        return donGia;
    }

    public void setDonGia(double donGia) {
        this.donGia = donGia;
        tinhThanhTien();
    }

    public double getMucGiamGia() {
        return mucGiamGia;
    }

    public void setMucGiamGia(double mucGiamGia) {
        this.mucGiamGia = mucGiamGia;
        tinhThanhTien();
    }

    public double getThanhTien() {
        return thanhTien;
    }

    public void setThanhTien(double thanhTien) {
        this.thanhTien = thanhTien;
    }

    public boolean isLaPhatSinh() {
        return laPhatSinh;
    }

    public void setLaPhatSinh(boolean laPhatSinh) {
        this.laPhatSinh = laPhatSinh;
    }

    public LocalDateTime getThoiHanBaoHanh() {
        return thoiHanBaoHanh;
    }

    public void setThoiHanBaoHanh(LocalDateTime thoiHanBaoHanh) {
        this.thoiHanBaoHanh = thoiHanBaoHanh;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public void tinhThanhTien() {
        this.thanhTien = Math.max(0, (this.soLuong * this.donGia) - this.mucGiamGia);
    }

    public void capNhatTienDo() {
    }

    public void danhDauHoanThanh() {
        this.trangThai = "Hoàn thành";
    }
}
