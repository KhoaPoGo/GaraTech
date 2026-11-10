package entity;

import enums.TrangThaiHoaDon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoaDon {
	private String maHoaDon;
	private String maPhieu;
	private String maNV;
	private PhieuTiepNhanSuaChua phieuSuaChua;
	private NhanVien nhanVien;
	private LocalDateTime ngayLap;
	private double tongTienDichVu;
	private double tongTienPhuTung;
	private double chiPhiKhac;
	private double giamGia;
	private double thueVAT = 10.0;
	private double tongTienThanhToan;
	private String trangThai;
	private String phuongThucTT; // "Tiền mặt" hoặc "Chuyển khoản"
	private String maGiaoDich;  // Mã giao dịch ngân hàng nếu chuyển khoản
	private LocalDateTime thoiGianThanhToan;
	private String maKM;
	private KhuyenMai khuyenMai;
	private List<ChiTietHoaDon> dsChiTiet = new ArrayList<>();
	private List<GiaoDichThanhToan> dsGiaoDich = new ArrayList<>();

	public HoaDon() {
		super();
	}

	public HoaDon(String maHoaDon) {
		super();
		this.maHoaDon = maHoaDon;
	}

	public HoaDon(String maHoaDon, LocalDateTime ngayLap, double tongTienDichVu, double tongTienPhuTung,
			double chiPhiKhac, double giamGia, double thueVAT, double tongTienThanhToan, String trangThai) {
		super();
		this.maHoaDon = maHoaDon;
		this.ngayLap = ngayLap;
		this.tongTienDichVu = tongTienDichVu;
		this.tongTienPhuTung = tongTienPhuTung;
		this.chiPhiKhac = chiPhiKhac;
		this.giamGia = giamGia;
		this.thueVAT = thueVAT;
		this.tongTienThanhToan = tongTienThanhToan;
		this.trangThai = trangThai;
	}

	public String getMaHoaDon() {
		return maHoaDon;
	}

	public void setMaHoaDon(String maHoaDon) {
		this.maHoaDon = maHoaDon;
	}

	public String getMaPhieu() {
		return maPhieu != null ? maPhieu : (phieuSuaChua != null ? phieuSuaChua.getMaPhieu() : null);
	}

	public void setMaPhieu(String maPhieu) {
		this.maPhieu = maPhieu;
	}

	public String getMaNV() {
		return maNV != null ? maNV : (nhanVien != null ? nhanVien.getMaNV() : null);
	}

	public void setMaNV(String maNV) {
		this.maNV = maNV;
	}

	public PhieuTiepNhanSuaChua getPhieuSuaChua() {
		return phieuSuaChua;
	}

	public void setPhieuSuaChua(PhieuTiepNhanSuaChua phieuSuaChua) {
		this.phieuSuaChua = phieuSuaChua;
		if (phieuSuaChua != null) {
			this.maPhieu = phieuSuaChua.getMaPhieu();
		}
	}

	public NhanVien getNhanVien() {
		return nhanVien;
	}

	public void setNhanVien(NhanVien nhanVien) {
		this.nhanVien = nhanVien;
		if (nhanVien != null) {
			this.maNV = nhanVien.getMaNV();
		}
	}

	public LocalDateTime getNgayLap() {
		return ngayLap;
	}

	public void setNgayLap(LocalDateTime ngayLap) {
		this.ngayLap = ngayLap;
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

	public String getPhuongThucTT() {
		return phuongThucTT;
	}

	public void setPhuongThucTT(String phuongThucTT) {
		this.phuongThucTT = phuongThucTT;
	}

	public String getPhuongThuc() {
		return phuongThucTT;
	}

	public void setPhuongThuc(String phuongThuc) {
		this.phuongThucTT = phuongThuc;
	}

	public String getMaGiaoDich() {
		return maGiaoDich;
	}

	public void setMaGiaoDich(String maGiaoDich) {
		this.maGiaoDich = maGiaoDich;
	}

	public LocalDateTime getThoiGianThanhToan() {
		return thoiGianThanhToan;
	}

	public void setThoiGianThanhToan(LocalDateTime thoiGianThanhToan) {
		this.thoiGianThanhToan = thoiGianThanhToan;
	}

	public String getMaKM() {
		return maKM != null ? maKM : (khuyenMai != null ? khuyenMai.getMaKM() : null);
	}

	public void setMaKM(String maKM) {
		this.maKM = maKM;
	}

	public KhuyenMai getKhuyenMai() {
		return khuyenMai;
	}

	public void setKhuyenMai(KhuyenMai khuyenMai) {
		this.khuyenMai = khuyenMai;
		if (khuyenMai != null) {
			this.maKM = khuyenMai.getMaKM();
		}
	}

	public List<ChiTietHoaDon> getDsChiTiet() {
		return dsChiTiet;
	}

	public void setDsChiTiet(List<ChiTietHoaDon> dsChiTiet) {
		this.dsChiTiet = dsChiTiet != null ? dsChiTiet : new ArrayList<>();
	}

	public List<GiaoDichThanhToan> getDsGiaoDich() {
		return dsGiaoDich;
	}

	public void setDsGiaoDich(List<GiaoDichThanhToan> dsGiaoDich) {
		this.dsGiaoDich = dsGiaoDich != null ? dsGiaoDich : new ArrayList<>();
	}

	@Override
	public String toString() {
		return "HoaDon [maHoaDon=" + maHoaDon + ", maPhieu=" + getMaPhieu() + ", ngayLap=" + ngayLap
				+ ", tongTienDichVu=" + tongTienDichVu + ", tongTienPhuTung=" + tongTienPhuTung
				+ ", chiPhiKhac=" + chiPhiKhac + ", giamGia=" + giamGia + ", thueVAT=" + thueVAT
				+ ", tongTienThanhToan=" + tongTienThanhToan + ", trangThai=" + trangThai + "]";
	}
}
