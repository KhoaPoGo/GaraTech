package entity;

import java.time.LocalDateTime;

public class HoaDon {
	private String maHoaDon;
	private LocalDateTime ngayLap;
	private double tongTienDichVu;
	private double tongTienPhuTung;
	private double chiPhiKhac;
	private double giamGia;
	private double thueVAT;
	private double tongTienThanhToan;
	private String trangThai;

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

	@Override
	public String toString() {
		return "DichVu [maHoaDon=" + maHoaDon + ", ngayLap=" + ngayLap + ", tongTienDichVu=" + tongTienDichVu
				+ ", tongTienPhuTung=" + tongTienPhuTung + ", chiPhiKhac=" + chiPhiKhac + ", giamGia=" + giamGia
				+ ", thueVAT=" + thueVAT + ", tongTienThanhToan=" + tongTienThanhToan + ", trangThai=" + trangThai
				+ "]";
	}
}
