package entity;

import enums.TrangThaiPhieu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thực thể gộp duy nhất: Phiếu Tiếp Nhận - Sửa Chữa (PhieuTiepNhanSuaChua).
 * Thay thế cho 2 thực thể rời rạc trước đây (PhieuTiepNhan và PhieuSuaChua).
 *
 * Quy trình trạng thái nghiệp vụ:
 * 1. Tiếp nhận viên lập phiếu ban đầu -> CHO_SUA.
 * 2. Kỹ thuật viên tiếp nhận khám & bắt đầu sửa -> DANG_SUA.
 * 3. Nếu thiếu phụ tùng -> chuyển tạm dừng CHO_LINH_KIEN (có hàng -> DANG_SUA).
 * 4. KTV sửa xong, nghiệm thu -> HOAN_TAT (Phiếu bị KHÓA hoàn toàn, read-only).
 * 5. Lập hóa đơn từ phiếu HOAN_TAT -> DA_THANH_TOAN.
 */
public class PhieuTiepNhanSuaChua {

    private String maPhieu;
    private Xe xe;
    private NhanVien nhanVienTiepNhan;
    private NhanVien nhanVienKyThuat;
    private LocalDateTime ngayGioTiepNhan = LocalDateTime.now();
    private String yeuCauBanDau;
    private String tinhTrangXe;
    private float soKmTiepNhan = 0;
    private float mucNhienLieu = 0;
    private LocalDateTime thoiGianHenTra;
    private LocalDateTime thoiGianBatDau;
    private LocalDateTime thoiGianHoanThanh;
    private String ghiChuKyThuat;
    private TrangThaiPhieu trangThai = TrangThaiPhieu.CHO_SUA;

    private List<ChiTietDichVu> chiTietDichVu = new ArrayList<>();
    private List<ChiTietLinhKien> chiTietLinhKien = new ArrayList<>();

    public PhieuTiepNhanSuaChua() {
    }

    public PhieuTiepNhanSuaChua(String maPhieu) {
        this.maPhieu = maPhieu;
    }

    public PhieuTiepNhanSuaChua(String maPhieu, Xe xe, NhanVien nhanVienTiepNhan, NhanVien nhanVienKyThuat,
                                LocalDateTime ngayGioTiepNhan, String yeuCauBanDau, TrangThaiPhieu trangThai) {
        setMaPhieu(maPhieu);
        setXe(xe);
        setNhanVienTiepNhan(nhanVienTiepNhan);
        setNhanVienKyThuat(nhanVienKyThuat);
        setNgayGioTiepNhan(ngayGioTiepNhan);
        setYeuCauBanDau(yeuCauBanDau);
        setTrangThai(trangThai);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GETTERS & SETTERS (CÓ KIỂM TRA RÀNG BUỘC NGHIỆP VỤ)
    // ─────────────────────────────────────────────────────────────────────────

    public String getMaPhieu() {
        return maPhieu;
    }

    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
    }

    public Xe getXe() {
        return xe;
    }

    public void setXe(Xe xe) {
        if (xe == null) throw new IllegalArgumentException("Phiếu sửa chữa phải gắn với một xe.");
        this.xe = xe;
    }

    public NhanVien getNhanVienTiepNhan() {
        return nhanVienTiepNhan;
    }

    public void setNhanVienTiepNhan(NhanVien nhanVienTiepNhan) {
        if (nhanVienTiepNhan == null) throw new IllegalArgumentException("Nhân viên tiếp nhận không được để trống.");
        this.nhanVienTiepNhan = nhanVienTiepNhan;
    }

    public NhanVien getNhanVienKyThuat() {
        return nhanVienKyThuat;
    }

    public void setNhanVienKyThuat(NhanVien nhanVienKyThuat) {
        if (nhanVienKyThuat != null && nhanVienTiepNhan != null &&
            nhanVienKyThuat.getMaNV() != null && nhanVienKyThuat.getMaNV().equals(nhanVienTiepNhan.getMaNV())) {
            throw new IllegalArgumentException("Kỹ thuật viên chính phụ trách phải khác với nhân viên tiếp nhận.");
        }
        this.nhanVienKyThuat = nhanVienKyThuat;
    }

    public LocalDateTime getNgayGioTiepNhan() {
        return ngayGioTiepNhan;
    }

    public void setNgayGioTiepNhan(LocalDateTime ngayGioTiepNhan) {
        if (ngayGioTiepNhan == null) {
            this.ngayGioTiepNhan = LocalDateTime.now();
        } else {
            this.ngayGioTiepNhan = ngayGioTiepNhan;
        }
    }

    public String getYeuCauBanDau() {
        return yeuCauBanDau;
    }

    public void setYeuCauBanDau(String yeuCauBanDau) {
        if (yeuCauBanDau == null || yeuCauBanDau.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu ban đầu không được để trống.");
        }
        this.yeuCauBanDau = yeuCauBanDau.trim();
    }

    public String getTinhTrangXe() {
        return tinhTrangXe;
    }

    public void setTinhTrangXe(String tinhTrangXe) {
        this.tinhTrangXe = tinhTrangXe;
    }

    public float getSoKmTiepNhan() {
        return soKmTiepNhan;
    }

    public void setSoKmTiepNhan(float soKmTiepNhan) {
        if (soKmTiepNhan < 0) throw new IllegalArgumentException("Số km tiếp nhận không được âm.");
        this.soKmTiepNhan = soKmTiepNhan;
    }

    public float getMucNhienLieu() {
        return mucNhienLieu;
    }

    public void setMucNhienLieu(float mucNhienLieu) {
        if (mucNhienLieu < 0) throw new IllegalArgumentException("Mức nhiên liệu không được âm.");
        this.mucNhienLieu = mucNhienLieu;
    }

    public LocalDateTime getThoiGianHenTra() {
        return thoiGianHenTra;
    }

    public void setThoiGianHenTra(LocalDateTime thoiGianHenTra) {
        this.thoiGianHenTra = thoiGianHenTra;
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

    public TrangThaiPhieu getTrangThai() {
        return trangThai;
    }

    /**
     * Chuyển trạng thái phiếu có kiểm tra logic chuyển tiếp hợp lệ:
     * CHO_SUA -> DANG_SUA <-> CHO_LINH_KIEN -> HOAN_TAT
     */
    public void setTrangThai(TrangThaiPhieu trangThaiMoi) {
        if (trangThaiMoi == null) throw new IllegalArgumentException("Trạng thái phiếu không được để trống.");

        if (this.trangThai == TrangThaiPhieu.HOAN_TAT && trangThaiMoi != TrangThaiPhieu.HOAN_TAT) {
            throw new IllegalStateException("Phiếu đã ở trạng thái HOÀN TẤT - Không thể thay đổi trạng thái nữa.");
        }

        if (this.trangThai != null && this.trangThai != trangThaiMoi) {
            boolean hopLe = false;
            switch (this.trangThai) {
                case CHO_SUA -> hopLe = (trangThaiMoi == TrangThaiPhieu.DANG_SUA || trangThaiMoi == TrangThaiPhieu.CHO_LINH_KIEN);
                case DANG_SUA -> hopLe = (trangThaiMoi == TrangThaiPhieu.CHO_LINH_KIEN || trangThaiMoi == TrangThaiPhieu.HOAN_TAT);
                case CHO_LINH_KIEN -> hopLe = (trangThaiMoi == TrangThaiPhieu.DANG_SUA);
                case HOAN_TAT -> hopLe = false;
            }
            if (!hopLe) {
                throw new IllegalArgumentException(String.format(
                        "Không thể chuyển trạng thái từ '%s' sang '%s'. Quy trình: Chờ sửa → Đang sửa ⇄ Chờ linh kiện → Hoàn tất.",
                        this.trangThai.label(), trangThaiMoi.label()));
            }
        }

        if (trangThaiMoi == TrangThaiPhieu.DANG_SUA && this.nhanVienKyThuat == null) {
            throw new IllegalStateException("Phiếu phải có Kỹ thuật viên chính phụ trách trước khi chuyển sang Đang sửa.");
        }

        if (trangThaiMoi == TrangThaiPhieu.DANG_SUA && this.thoiGianBatDau == null) {
            this.thoiGianBatDau = LocalDateTime.now();
        } else if (trangThaiMoi == TrangThaiPhieu.HOAN_TAT && this.thoiGianHoanThanh == null) {
            this.thoiGianHoanThanh = LocalDateTime.now();
        }

        this.trangThai = trangThaiMoi;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUẢN LÝ DANH SÁCH DỊCH VỤ & LINH KIỆN
    // ─────────────────────────────────────────────────────────────────────────

    public List<ChiTietDichVu> getChiTietDichVu() {
        return Collections.unmodifiableList(chiTietDichVu);
    }

    public void setChiTietDichVu(List<ChiTietDichVu> ds) {
        this.chiTietDichVu = ds != null ? new ArrayList<>(ds) : new ArrayList<>();
    }

    public void themChiTietDichVu(ChiTietDichVu ct) {
        kiemTraChuaHoanTat("thêm dịch vụ");
        if (ct == null) return;
        ct.setPhieuSuaChua(this);
        chiTietDichVu.removeIf(item -> item.getDichVu().getMaDichVu().equals(ct.getDichVu().getMaDichVu()));
        chiTietDichVu.add(ct);
    }

    public void xoaChiTietDichVu(String maDichVu) {
        kiemTraChuaHoanTat("xóa dịch vụ");
        chiTietDichVu.removeIf(item -> item.getDichVu() != null && item.getDichVu().getMaDichVu().equals(maDichVu));
    }

    public List<ChiTietLinhKien> getChiTietLinhKien() {
        return Collections.unmodifiableList(chiTietLinhKien);
    }

    public void setChiTietLinhKien(List<ChiTietLinhKien> ds) {
        this.chiTietLinhKien = ds != null ? new ArrayList<>(ds) : new ArrayList<>();
    }

    public void themChiTietLinhKien(ChiTietLinhKien ct) {
        kiemTraChuaHoanTat("thêm linh kiện");
        if (ct == null) return;
        ct.setPhieuSuaChua(this);
        chiTietLinhKien.removeIf(item -> item.getPhuTung().getMaPhuTung().equals(ct.getPhuTung().getMaPhuTung()));
        chiTietLinhKien.add(ct);
    }

    public void xoaChiTietLinhKien(String maPhuTung) {
        kiemTraChuaHoanTat("xóa linh kiện");
        chiTietLinhKien.removeIf(item -> item.getPhuTung() != null && item.getPhuTung().getMaPhuTung().equals(maPhuTung));
    }

    private void kiemTraChuaHoanTat(String hanhDong) {
        if (this.trangThai == TrangThaiPhieu.HOAN_TAT) {
            throw new IllegalStateException("Phiếu đã HOÀN TẤT - Không được phép " + hanhDong + " để chống gian lận.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÍNH TOÁN CHI PHÍ TỰ ĐỘNG
    // ─────────────────────────────────────────────────────────────────────────

    public double tinhTongTienDichVu() {
        double sum = 0;
        for (ChiTietDichVu ct : chiTietDichVu) {
            sum += ct.tinhThanhTien();
        }
        return sum;
    }

    public double tinhTongTienLinhKien() {
        double sum = 0;
        for (ChiTietLinhKien ct : chiTietLinhKien) {
            sum += ct.tinhThanhTien();
        }
        return sum;
    }

    public double tinhTongChiPhi() {
        return tinhTongTienDichVu() + tinhTongTienLinhKien();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RAW SETTERS (DÙNG CHO DAO MAPPING RESULTSET)
    // ─────────────────────────────────────────────────────────────────────────
    public void setMaPhieuRaw(String maPhieu) { this.maPhieu = maPhieu; }
    public void setXeRaw(Xe xe) { this.xe = xe; }
    public void setNhanVienTiepNhanRaw(NhanVien nv) { this.nhanVienTiepNhan = nv; }
    public void setNhanVienKyThuatRaw(NhanVien nv) { this.nhanVienKyThuat = nv; }
    public void setNgayGioTiepNhanRaw(LocalDateTime dt) { this.ngayGioTiepNhan = dt; }
    public void setYeuCauBanDauRaw(String yc) { this.yeuCauBanDau = yc; }
    public void setTinhTrangXeRaw(String tt) { this.tinhTrangXe = tt; }
    public void setSoKmTiepNhanRaw(float km) { this.soKmTiepNhan = km; }
    public void setMucNhienLieuRaw(float nl) { this.mucNhienLieu = nl; }
    public void setThoiGianHenTraRaw(LocalDateTime dt) { this.thoiGianHenTra = dt; }
    public void setThoiGianBatDauRaw(LocalDateTime dt) { this.thoiGianBatDau = dt; }
    public void setThoiGianHoanThanhRaw(LocalDateTime dt) { this.thoiGianHoanThanh = dt; }
    public void setGhiChuKyThuatRaw(String gc) { this.ghiChuKyThuat = gc; }
    public void setTrangThaiRaw(TrangThaiPhieu tt) { this.trangThai = tt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhieuTiepNhanSuaChua that = (PhieuTiepNhanSuaChua) o;
        return Objects.equals(maPhieu, that.maPhieu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPhieu);
    }

    @Override
    public String toString() {
        return "PhieuTiepNhanSuaChua{" +
                "maPhieu='" + maPhieu + '\'' +
                ", bienSo=" + (xe != null ? xe.getBienSo() : "null") +
                ", KTV=" + (nhanVienKyThuat != null ? nhanVienKyThuat.getHoTen() : "Chưa phân công") +
                ", trangThai=" + trangThai +
                ", tongChiPhi=" + tinhTongChiPhi() +
                '}';
    }
}
