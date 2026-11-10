package entity;

import java.util.Objects;

/**
 * Chi tiết dịch vụ thi công của một Phiếu tiếp nhận - sửa chữa.
 * Bảng ChiTietDichVu (khóa chính: maPhieu, maDichVu).
 */
public class ChiTietDichVu {
    private PhieuTiepNhanSuaChua phieuSuaChua;
    private DichVu dichVu;
    private int soLuong = 1;
    private double donGiaThucHien;
    private String ghiChu;

    public ChiTietDichVu() {
    }

    public ChiTietDichVu(PhieuTiepNhanSuaChua phieuSuaChua) {
        this.phieuSuaChua = phieuSuaChua;
    }

    public ChiTietDichVu(PhieuTiepNhanSuaChua phieuSuaChua, DichVu dichVu, int soLuong, double donGiaThucHien, String ghiChu) {
        setPhieuSuaChua(phieuSuaChua);
        setDichVu(dichVu);
        setSoLuong(soLuong);
        setDonGiaThucHien(donGiaThucHien);
        setGhiChu(ghiChu);
    }

    public PhieuTiepNhanSuaChua getPhieuSuaChua() {
        return phieuSuaChua;
    }

    public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
        this.phieuSuaChua = phieuSuaChua;
    }

    public DichVu getDichVu() {
        return dichVu;
    }

    public void setDichVu(DichVu dichVu) {
        if (dichVu == null) throw new IllegalArgumentException("Dịch vụ không được để trống.");
        this.dichVu = dichVu;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        if (soLuong <= 0) throw new IllegalArgumentException("Số lượng dịch vụ phải lớn hơn 0.");
        this.soLuong = soLuong;
    }

    public double getDonGiaThucHien() {
        return donGiaThucHien;
    }

    public void setDonGiaThucHien(double donGiaThucHien) {
        if (donGiaThucHien < 0) throw new IllegalArgumentException("Đơn giá thực hiện không được âm.");
        this.donGiaThucHien = donGiaThucHien;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    /**
     * Tính thành tiền = soLuong * donGiaThucHien
     */
    public double tinhThanhTien() {
        return soLuong * donGiaThucHien;
    }

    public double getThanhTien() {
        return tinhThanhTien();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChiTietDichVu that = (ChiTietDichVu) o;
        return Objects.equals(phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : null,
                              that.phieuSuaChua != null ? that.phieuSuaChua.getMaPhieu() : null) &&
               Objects.equals(dichVu != null ? dichVu.getMaDichVu() : null,
                              that.dichVu != null ? that.dichVu.getMaDichVu() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : null,
                            dichVu != null ? dichVu.getMaDichVu() : null);
    }

    @Override
    public String toString() {
        return "ChiTietDichVu{" +
                "maPhieu=" + (phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : "null") +
                ", dichVu=" + (dichVu != null ? dichVu.getTenDichVu() : "null") +
                ", soLuong=" + soLuong +
                ", donGiaThucHien=" + donGiaThucHien +
                ", thanhTien=" + tinhThanhTien() +
                '}';
    }
}
