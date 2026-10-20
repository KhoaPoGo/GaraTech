package entity;

import java.time.LocalDateTime;

public class GiaoDichThanhToan {
	private String maGiaoDich;
	private LocalDateTime ngayThanhToan;
	private double soTien;
	private String phuongThuc;
	private String soDung;
	private String trangThai;

	public GiaoDichThanhToan(String maGiaoDich) {
		super();
		this.maGiaoDich = maGiaoDich;
	}

	public GiaoDichThanhToan(String maGiaoDich, LocalDateTime ngayThanhToan, double soTien, String phuongThuc,
			String soDung, String trangThai) {
		super();
		this.maGiaoDich = maGiaoDich;
		this.ngayThanhToan = ngayThanhToan;
		this.soTien = soTien;
		this.phuongThuc = phuongThuc;
		this.soDung = soDung;
		this.trangThai = trangThai;
	}

	public String getMaGiaoDich() {
		return maGiaoDich;
	}

	public void setMaGiaoDich(String maGiaoDich) {
		this.maGiaoDich = maGiaoDich;
	}

	public LocalDateTime getNgayThanhToan() {
		return ngayThanhToan;
	}

	public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
		this.ngayThanhToan = ngayThanhToan;
	}

	public double getSoTien() {
		return soTien;
	}

	public void setSoTien(double soTien) {
		this.soTien = soTien;
	}

	public String getPhuongThuc() {
		return phuongThuc;
	}

	public void setPhuongThuc(String phuongThuc) {
		this.phuongThuc = phuongThuc;
	}

	public String getSoDung() {
		return soDung;
	}

	public void setSoDung(String soDung) {
		this.soDung = soDung;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	@Override
	public String toString() {
		return "GiaoDichThanhToan [maGiaoDich=" + maGiaoDich + ", ngayThanhToan=" + ngayThanhToan + ", soTien=" + soTien
				+ ", phuongThuc=" + phuongThuc + ", soDung=" + soDung + ", trangThai=" + trangThai + "]";
	}

}
