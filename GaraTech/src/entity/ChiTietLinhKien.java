package entity;

import java.util.Objects;

/**
 * Chi tiết linh kiện / phụ tùng sử dụng trong Phiếu tiếp nhận - sửa chữa.
 * Bảng ChiTietLinhKien (khóa chính: maPhieu, maPhuTung).
 */
public class ChiTietLinhKien {
    private PhieuTiepNhanSuaChua phieuSuaChua;
    private PhuTung phuTung;
    private int soLuong = 1;
    private double donGiaXuat;
    private String ghiChu;

    public ChiTietLinhKien() {
    }

    public ChiTietLinhKien(PhieuTiepNhanSuaChua phieuSuaChua) {
        this.phieuSuaChua = phieuSuaChua;
    }

    public ChiTietLinhKien(PhieuTiepNhanSuaChua phieuSuaChua, PhuTung phuTung, int soLuong, double donGiaXuat, String ghiChu) {
        setPhieuSuaChua(phieuSuaChua);
        setPhuTung(phuTung);
        setSoLuong(soLuong);
        setDonGiaXuat(donGiaXuat);
        setGhiChu(ghiChu);
    }

    public PhieuTiepNhanSuaChua getPhieuSuaChua() {
        return phieuSuaChua;
    }

    public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
        this.phieuSuaChua = phieuSuaChua;
    }

    public PhuTung getPhuTung() {
        return phuTung;
    }

    public void setPhuTung(PhuTung phuTung) {
        if (phuTung == null) throw new IllegalArgumentException("Phụ tùng / linh kiện không được để trống.");
        this.phuTung = phuTung;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng linh kiện phải lớn hơn 0.");
        this.soLuong = soLuong;
    }

    public double getDonGiaXuat() {
        return donGiaXuat;
    }

    public void setDonGiaXuat(double donGiaXuat) {
        if (donGiaXuat < 0) throw new IllegalArgumentException("Đơn giá xuất không được âm.");
        this.donGiaXuat = donGiaXuat;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    /**
     * Tính thành tiền = soLuong * donGiaXuat
     */
    public double tinhThanhTien() {
        return soLuong * donGiaXuat;
    }

    public double getThanhTien() {
        return tinhThanhTien();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChiTietLinhKien that = (ChiTietLinhKien) o;
        return Objects.equals(phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : null,
                              that.phieuSuaChua != null ? that.phieuSuaChua.getMaPhieu() : null) &&
               Objects.equals(phuTung != null ? phuTung.getMaPhuTung() : null,
                              that.phuTung != null ? that.phuTung.getMaPhuTung() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : null,
                            phuTung != null ? phuTung.getMaPhuTung() : null);
    }

    @Override
    public String toString() {
        return "ChiTietLinhKien{" +
                "maPhieu=" + (phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : "null") +
                ", phuTung=" + (phuTung != null ? phuTung.getTenPhuTung() : "null") +
                ", soLuong=" + soLuong +
                ", donGiaXuat=" + donGiaXuat +
                ", thanhTien=" + tinhThanhTien() +
                '}';
    }
}
