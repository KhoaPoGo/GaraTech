package entity;

import enums.TrangThaiHoaDon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thực thể Hóa đơn thanh toán dịch vụ & phụ tùng.
 * Đã chuẩn hóa loại bỏ các thuộc tính dư thừa:
 * - maPhieu, maNV, maKM (truy xuất trực tiếp qua đối tượng phieuSuaChua, nhanVien, khuyenMai).
 * - thoiGianThanhToan (sử dụng thống nhất ngayLap).
 * - dsGiaoDich (thanh toán dứt điểm lưu trực tiếp qua phuongThuc và maGiaoDich trên HoaDon).
 * - Chuẩn hóa phuongThucTT thành phuongThuc khớp 100% với CSDL SQL Server.
 */
public class HoaDon {
    private String maHoaDon;
    private PhieuTiepNhanSuaChua phieuSuaChua;
    private NhanVien nhanVien;
    private KhuyenMai khuyenMai;
    private LocalDateTime ngayLap = LocalDateTime.now();
    private double tongTienDichVu;
    private double tongTienPhuTung;
    private double chiPhiKhac;
    private double giamGia;
    private double thueVAT = 10.0;
    private double tongTienThanhToan;
    private String phuongThuc = "Tiền mặt"; // "Tiền mặt" hoặc "Chuyển khoản"
    private String maGiaoDich;               // Mã giao dịch ngân hàng nếu chuyển khoản
    private String trangThai = TrangThaiHoaDon.DA_THANH_TOAN.label();
    private List<ChiTietHoaDon> dsChiTiet = new ArrayList<>();

    public HoaDon() {
        super();
    }

    public HoaDon(String maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public HoaDon(String maHoaDon, PhieuTiepNhanSuaChua phieuSuaChua, NhanVien nhanVien, LocalDateTime ngayLap,
                  double tongTienDichVu, double tongTienPhuTung, double chiPhiKhac, double giamGia,
                  double thueVAT, double tongTienThanhToan, String phuongThuc, String trangThai) {
        this.maHoaDon = maHoaDon;
        this.phieuSuaChua = phieuSuaChua;
        this.nhanVien = nhanVien;
        this.ngayLap = ngayLap != null ? ngayLap : LocalDateTime.now();
        this.tongTienDichVu = tongTienDichVu;
        this.tongTienPhuTung = tongTienPhuTung;
        this.chiPhiKhac = chiPhiKhac;
        this.giamGia = giamGia;
        this.thueVAT = thueVAT;
        this.tongTienThanhToan = tongTienThanhToan;
        this.phuongThuc = phuongThuc != null ? phuongThuc : "Tiền mặt";
        this.trangThai = trangThai != null ? trangThai : TrangThaiHoaDon.DA_THANH_TOAN.label();
    }

    // ---- Getters / Setters ----

    public String getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(String maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public PhieuTiepNhanSuaChua getPhieuSuaChua() {
        return phieuSuaChua;
    }

    public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
        this.phieuSuaChua = phieuSuaChua;
    }

    // Convenience delegates cho maPhieu
    public String getMaPhieu() {
        return phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : null;
    }

    public void setMaPhieu(String maPhieu) {
        if (maPhieu == null) {
            this.phieuSuaChua = null;
        } else if (this.phieuSuaChua == null) {
            this.phieuSuaChua = new PhieuTiepNhanSuaChua(maPhieu);
        } else {
            this.phieuSuaChua.setMaPhieuRaw(maPhieu);
        }
    }

    public NhanVien getNhanVien() {
        return nhanVien;
    }

    public void setNhanVien(NhanVien nhanVien) {
        this.nhanVien = nhanVien;
    }

    // Convenience delegates cho maNV
    public String getMaNV() {
        return nhanVien != null ? nhanVien.getMaNV() : null;
    }

    public void setMaNV(String maNV) {
        if (maNV == null) {
            this.nhanVien = null;
        } else if (this.nhanVien == null) {
            NhanVien nv = new NhanVien();
            nv.setMaNV(maNV);
            this.nhanVien = nv;
        } else {
            this.nhanVien.setMaNV(maNV);
        }
    }

    public KhuyenMai getKhuyenMai() {
        return khuyenMai;
    }

    public void setKhuyenMai(KhuyenMai khuyenMai) {
        this.khuyenMai = khuyenMai;
    }

    // Convenience delegates cho maKM
    public String getMaKM() {
        return khuyenMai != null ? khuyenMai.getMaKM() : null;
    }

    public void setMaKM(String maKM) {
        if (maKM == null || maKM.isBlank()) {
            this.khuyenMai = null;
        } else if (this.khuyenMai == null) {
            this.khuyenMai = new KhuyenMai(maKM);
        } else {
            this.khuyenMai.setMaKM(maKM);
        }
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        this.ngayLap = ngayLap;
    }

    // Compatibility alias: thoiGianThanhToan đồng nhất với ngayLap
    public LocalDateTime getThoiGianThanhToan() {
        return ngayLap;
    }

    public void setThoiGianThanhToan(LocalDateTime dt) {
        if (dt != null) this.ngayLap = dt;
    }

    public double getTongTienDichVu() {
        return tongTienDichVu;
    }

    public void setTongTienDichVu(double tongTienDichVu) {
        this.tongTienDichVu = tongTienDichVu;
    }

    public double getTongTienPhuTung() {
        return tongTienPhuTung;
    }

    public void setTongTienPhuTung(double tongTienPhuTung) {
        this.tongTienPhuTung = tongTienPhuTung;
    }

    public double getChiPhiKhac() {
        return chiPhiKhac;
    }

    public void setChiPhiKhac(double chiPhiKhac) {
        this.chiPhiKhac = chiPhiKhac;
    }

    public double getGiamGia() {
        return giamGia;
    }

    public void setGiamGia(double giamGia) {
        this.giamGia = giamGia;
    }

    public double getThueVAT() {
        return thueVAT;
    }

    public void setThueVAT(double thueVAT) {
        this.thueVAT = thueVAT;
    }

    public double getTongTienThanhToan() {
        return tongTienThanhToan;
    }

    public void setTongTienThanhToan(double tongTienThanhToan) {
        this.tongTienThanhToan = tongTienThanhToan;
    }

    public String getPhuongThuc() {
        return phuongThuc;
    }

    public void setPhuongThuc(String phuongThuc) {
        this.phuongThuc = phuongThuc != null ? phuongThuc : "Tiền mặt";
    }

    // Compatibility alias cho phuongThucTT
    public String getPhuongThucTT() {
        return phuongThuc;
    }

    public void setPhuongThucTT(String phuongThucTT) {
        this.phuongThuc = phuongThucTT != null ? phuongThucTT : "Tiền mặt";
    }

    public String getMaGiaoDich() {
        return maGiaoDich;
    }

    public void setMaGiaoDich(String maGiaoDich) {
        this.maGiaoDich = maGiaoDich;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public TrangThaiHoaDon getTrangThaiEnum() {
        return TrangThaiHoaDon.fromLabel(trangThai);
    }

    public void setTrangThai(TrangThaiHoaDon tt) {
        this.trangThai = tt != null ? tt.label() : null;
    }

    public List<ChiTietHoaDon> getDsChiTiet() {
        return dsChiTiet;
    }

    public void setDsChiTiet(List<ChiTietHoaDon> dsChiTiet) {
        this.dsChiTiet = dsChiTiet != null ? dsChiTiet : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HoaDon hoaDon)) return false;
        return Objects.equals(maHoaDon, hoaDon.maHoaDon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maHoaDon);
    }

    @Override
    public String toString() {
        return "HoaDon [maHoaDon=" + maHoaDon + ", maPhieu=" + getMaPhieu() + ", ngayLap=" + ngayLap
                + ", tongTienDichVu=" + tongTienDichVu + ", tongTienPhuTung=" + tongTienPhuTung
                + ", chiPhiKhac=" + chiPhiKhac + ", giamGia=" + giamGia + ", thueVAT=" + thueVAT
                + ", tongTienThanhToan=" + tongTienThanhToan + ", phuongThuc=" + phuongThuc
                + ", trangThai=" + trangThai + "]";
    }
}
