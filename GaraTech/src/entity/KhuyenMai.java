package entity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Entity KhuyenMai tương ứng bảng SQL (maKM, maNV, tên, phần trăm, ngày bắt đầu/kết thúc, ghi chú). */
public class KhuyenMai {
    private String maKM;
    private String maNVTao;
    private String tenChuongTrinh;
    private double mucGiamPhanTram; // Ví dụ: 10.0 = giảm 10%
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private String ghiChu;

    // Giữ tương thích với màn thanh toán cũ; schema hiện tại không lưu danh sách áp dụng riêng.
    private Set<String> dsDichVuApDung = new HashSet<>();
    private Set<String> dsPhuTungApDung = new HashSet<>();

    public KhuyenMai() {
    }

    public KhuyenMai(String maKM) {
        this.maKM = maKM;
    }

    public KhuyenMai(String maKM, String tenChuongTrinh, double mucGiamPhanTram, LocalDate ngayBatDau, LocalDate ngayKetThuc, String ghiChu) {
        this.maKM = maKM;
        this.tenChuongTrinh = tenChuongTrinh;
        this.mucGiamPhanTram = mucGiamPhanTram;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.ghiChu = ghiChu;
    }

    public String getMaKM() {
        return maKM;
    }

    public void setMaKM(String maKM) {
        this.maKM = maKM;
    }

    public String getMaNVTao() {
        return maNVTao;
    }

    public void setMaNVTao(String maNVTao) {
        this.maNVTao = maNVTao;
    }

    public String getTenChuongTrinh() {
        return tenChuongTrinh;
    }

    public void setTenChuongTrinh(String tenChuongTrinh) {
        this.tenChuongTrinh = tenChuongTrinh;
    }

    public double getMucGiamPhanTram() {
        return mucGiamPhanTram;
    }

    public void setMucGiamPhanTram(double mucGiamPhanTram) {
        this.mucGiamPhanTram = mucGiamPhanTram;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Set<String> getDsDichVuApDung() {
        return Collections.unmodifiableSet(dsDichVuApDung);
    }

    public void setDsDichVuApDung(Set<String> dsDichVuApDung) {
        this.dsDichVuApDung.clear();
        if (dsDichVuApDung != null) this.dsDichVuApDung.addAll(dsDichVuApDung);
    }

    public Set<String> getDsPhuTungApDung() {
        return Collections.unmodifiableSet(dsPhuTungApDung);
    }

    public void setDsPhuTungApDung(Set<String> dsPhuTungApDung) {
        this.dsPhuTungApDung.clear();
        if (dsPhuTungApDung != null) this.dsPhuTungApDung.addAll(dsPhuTungApDung);
    }

    /**
     * Kiểm tra chương trình áp dụng cho tất cả dịch vụ và phụ tùng không.
     */
    public boolean apDungTatCa() {
        return dsDichVuApDung.isEmpty() && dsPhuTungApDung.isEmpty();
    }

    /**
     * Kiểm tra chương trình khuyến mãi có hiệu lực tại ngày chỉ định không.
     */
    public boolean conHieuLuc(LocalDate ngay) {
        if (ngay == null || ngayBatDau == null || ngayKetThuc == null) return false;
        return !ngay.isBefore(ngayBatDau) && !ngay.isAfter(ngayKetThuc);
    }

    public boolean conHieuLuc() {
        return conHieuLuc(LocalDate.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KhuyenMai khuyenMai = (KhuyenMai) o;
        return Objects.equals(maKM, khuyenMai.maKM);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maKM);
    }

    @Override
    public String toString() {
        return maKM + " - " + tenChuongTrinh + " (Giảm " + (int) mucGiamPhanTram + "%)";
    }
}
