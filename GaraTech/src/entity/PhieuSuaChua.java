package entity;

import java.time.LocalDateTime;

public class PhieuSuaChua {
	private String maPhieuSuaChua;
	private LocalDateTime thoiGianDuKien;
	private LocalDateTime thoiGianBatDau;
	private LocalDateTime thoiGianHoanThanh;
	private String ghiChuKyThuat;
	private String trangThai;
	
	public PhieuSuaChua() {
		
	}

	public PhieuSuaChua(String maPhieuSuaChua, LocalDateTime thoiGianDuKien, LocalDateTime thoiGianBatDau,
			LocalDateTime thoiGianHoanThanh, String ghiChuKyThuat, String trangThai) {
		super();
		this.maPhieuSuaChua = maPhieuSuaChua;
		this.thoiGianDuKien = thoiGianDuKien;
		this.thoiGianBatDau = thoiGianBatDau;
		this.thoiGianHoanThanh = thoiGianHoanThanh;
		this.ghiChuKyThuat = ghiChuKyThuat;
		this.trangThai = trangThai;
	}

	public String getMaPhieuSuaChua() {
		return maPhieuSuaChua;
	}

	public void setMaPhieuSuaChua(String maPhieuSuaChua) {
		this.maPhieuSuaChua = maPhieuSuaChua;
	}

	public LocalDateTime getThoiGianDuKien() {
		return thoiGianDuKien;
	}

	public void setThoiGianDuKien(LocalDateTime thoiGianDuKien) {
		this.thoiGianDuKien = thoiGianDuKien;
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

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}
	
	public void taoPhieu() {
		
	}
	
	public void phanCongKTV() {
		
	}
	
	public void capNhatTienDo() {
		
	}
	public void danhGiaChatLuong() {
		
	}
}
