package entity;

import java.time.LocalDateTime;

public class PhieuTiepNhan {
	private String maPhieuTN;
	private LocalDateTime thoiGianTiepNhan;
	private LocalDateTime thoiGianHienTra;
	private String tinhTrangBanDau;
	private float mucNhienLieu;
	private float soKMTiepNhan;
	private KhachHang khachHang;
	private NhanVien nhanVien;
	private String trangThai;
	
	public PhieuTiepNhan() {
		
	}
	
	public PhieuTiepNhan(String maPhieuTN, LocalDateTime thoiGianTiepNhan, LocalDateTime thoiGianHienTra,
			String tinhTrangBanDau, float mucNhienLieu, float soKMTiepNhan, KhachHang khachHang, NhanVien nhanVien,
			String trangThai) {
		super();
		this.maPhieuTN = maPhieuTN;
		this.thoiGianTiepNhan = thoiGianTiepNhan;
		this.thoiGianHienTra = thoiGianHienTra;
		this.tinhTrangBanDau = tinhTrangBanDau;
		this.mucNhienLieu = mucNhienLieu;
		this.soKMTiepNhan = soKMTiepNhan;
		this.khachHang = khachHang;
		this.nhanVien = nhanVien;
		this.trangThai = trangThai;
	}

	public String getMaPhieuTN() {
		return maPhieuTN;
	}

	public void setMaPhieuTN(String maPhieuTN) {
		this.maPhieuTN = maPhieuTN;
	}

	public LocalDateTime getThoiGianTiepNhan() {
		return thoiGianTiepNhan;
	}

	public void setThoiGianTiepNhan(LocalDateTime thoiGianTiepNhan) {
		this.thoiGianTiepNhan = thoiGianTiepNhan;
	}

	public LocalDateTime getThoiGianHienTra() {
		return thoiGianHienTra;
	}

	public void setThoiGianHienTra(LocalDateTime thoiGianHienTra) {
		this.thoiGianHienTra = thoiGianHienTra;
	}

	public String getTinhTrangBanDau() {
		return tinhTrangBanDau;
	}

	public void setTinhTrangBanDau(String tinhTrangBanDau) {
		this.tinhTrangBanDau = tinhTrangBanDau;
	}

	public float getMucNhienLieu() {
		return mucNhienLieu;
	}

	public void setMucNhienLieu(float mucNhienLieu) {
		this.mucNhienLieu = mucNhienLieu;
	}

	public float getSoKMTiepNhan() {
		return soKMTiepNhan;
	}

	public void setSoKMTiepNhan(float soKMTiepNhan) {
		this.soKMTiepNhan = soKMTiepNhan;
	}

	public KhachHang getKhachHang() {
		return khachHang;
	}

	public void setKhachHang(KhachHang khachHang) {
		this.khachHang = khachHang;
	}

	public NhanVien getNhanVien() {
		return nhanVien;
	}

	public void setNhanVien(NhanVien nhanVien) {
		this.nhanVien = nhanVien;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	public void taoPhieu() {
		
	}
	
	public void capNhatTrangThai(String trangThaiMoi) {
		this.trangThai=trangThaiMoi;
	}

}
