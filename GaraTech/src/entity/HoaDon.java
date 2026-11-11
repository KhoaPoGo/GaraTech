package entity;

import enums.PhuongThucThanhToan;
import enums.TrangThaiHoaDon;
import enums.TrangThaiThanhToan;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thực thể Hóa đơn thanh toán dịch vụ & phụ tùng.
 * Sử dụng enum cho phương thức thanh toán (PhuongThucThanhToan)
 * và trạng thái thanh toán (TrangThaiThanhToan: CHUA_THANH_TOAN, DA_THANH_TOAN).
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
    private PhuongThucThanhToan phuongThucTT = PhuongThucThanhToan.TIEN_MAT;
    private TrangThaiThanhToan trangThaiThanhToan = TrangThaiThanhToan.DA_THANH_TOAN;
    private String maGiaoDich; // Mã GD ngân hàng nếu chuyển khoản
    private List<ChiTietHoaDon> dsChiTiet = new ArrayList<>();

    public HoaDon() {
        this.trangThaiThanhToan = TrangThaiThanhToan.CHUA_THANH_TOAN;
    }

    public HoaDon(String maHoaDon) {
        this();
        this.maHoaDon = maHoaDon;
    }

    public HoaDon(String maHoaDon, PhieuTiepNhanSuaChua phieuSuaChua, NhanVien nhanVien, LocalDateTime ngayLap,
                  double tongTienDichVu, double tongTienPhuTung, double chiPhiKhac, double giamGia,
                  double thueVAT, double tongTienThanhToan, PhuongThucThanhToan phuongThucTT, TrangThaiThanhToan trangThaiThanhToan) {
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
        this.phuongThucTT = phuongThucTT != null ? phuongThucTT : PhuongThucThanhToan.TIEN_MAT;
        this.trangThaiThanhToan = trangThaiThanhToan != null ? trangThaiThanhToan : TrangThaiThanhToan.DA_THANH_TOAN;
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

    // ---- Phương thức thanh toán (Enum PhuongThucThanhToan) ----
    public PhuongThucThanhToan getPhuongThucTT() {
        return phuongThucTT;
    }

    public void setPhuongThucTT(PhuongThucThanhToan phuongThucTT) {
        this.phuongThucTT = phuongThucTT != null ? phuongThucTT : PhuongThucThanhToan.TIEN_MAT;
    }

    public void setPhuongThucTT(String pt) {
        this.phuongThucTT = PhuongThucThanhToan.fromString(pt);
    }

    public PhuongThucThanhToan getPhuongThuc() {
        return phuongThucTT;
    }

    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) {
        this.phuongThucTT = phuongThuc != null ? phuongThuc : PhuongThucThanhToan.TIEN_MAT;
    }

    public void setPhuongThuc(String phuongThuc) {
        this.phuongThucTT = PhuongThucThanhToan.fromString(phuongThuc);
    }

    // ---- Trạng thái thanh toán (Enum TrangThaiThanhToan) ----
    public TrangThaiThanhToan getTrangThaiThanhToan() {
        return trangThaiThanhToan;
    }

    public void setTrangThaiThanhToan(TrangThaiThanhToan trangThaiThanhToan) {
        this.trangThaiThanhToan = trangThaiThanhToan != null ? trangThaiThanhToan : TrangThaiThanhToan.DA_THANH_TOAN;
    }

    public void setTrangThaiThanhToan(String s) {
        this.trangThaiThanhToan = TrangThaiThanhToan.fromString(s);
    }

    public TrangThaiThanhToan getTrangThai() {
        return trangThaiThanhToan;
    }

    public void setTrangThai(TrangThaiThanhToan trangThai) {
        this.trangThaiThanhToan = trangThai != null ? trangThai : TrangThaiThanhToan.DA_THANH_TOAN;
    }

    public void setTrangThai(String trangThai) {
        this.trangThaiThanhToan = TrangThaiThanhToan.fromString(trangThai);
    }

    public TrangThaiHoaDon getTrangThaiEnum() {
        return TrangThaiHoaDon.fromLabel(trangThaiThanhToan != null ? trangThaiThanhToan.label() : null);
    }

    public void setTrangThai(TrangThaiHoaDon tt) {
        this.trangThaiThanhToan = tt != null ? TrangThaiThanhToan.fromString(tt.label()) : null;
    }

    public String getMaGiaoDich() {
        return maGiaoDich;
    }

    public void setMaGiaoDich(String maGiaoDich) {
        this.maGiaoDich = maGiaoDich;
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
                + ", tongTienThanhToan=" + tongTienThanhToan + ", phuongThucTT=" + phuongThucTT
                + ", trangThaiThanhToan=" + trangThaiThanhToan + "]";
    }
}
