package entity;

import java.time.LocalDateTime;

public class HangMucSuaChua {
	private String maHangMuc;
	private String loaiHangMuc;
	private String tenHangMuc;
	private int soLuong;
	private double donGia;
	private float mucGiamGia;
	private double thanhTien;
	private LocalDateTime thoiGianBaoHanh;
	private String trangThai;
	private String ghiChu;
	public HangMucSuaChua() {
		
	}
	public HangMucSuaChua(String maHangMuc, String loaiHangMuc, String tenHangMuc, int soLuong, double donGia,
			float mucGiamGia, double thanhTien, LocalDateTime thoiGianBaoHanh, String trangThai, String ghiChu) {
		super();
		this.maHangMuc = maHangMuc;
		this.loaiHangMuc = loaiHangMuc;
		this.tenHangMuc = tenHangMuc;
		this.soLuong = soLuong;
		this.donGia = donGia;
		this.mucGiamGia = mucGiamGia;
		this.thanhTien = thanhTien;
		this.thoiGianBaoHanh = thoiGianBaoHanh;
		this.trangThai = trangThai;
		this.ghiChu = ghiChu;
	}
	public String getMaHangMuc() {
		return maHangMuc;
	}
	public void setMaHangMuc(String maHangMuc) {
		this.maHangMuc = maHangMuc;
	}
	public String getLoaiHangMuc() {
		return loaiHangMuc;
	}
	public void setLoaiHangMuc(String loaiHangMuc) {
		this.loaiHangMuc = loaiHangMuc;
	}
	public String getTenHangMuc() {
		return tenHangMuc;
	}
	public void setTenHangMuc(String tenHangMuc) {
		this.tenHangMuc = tenHangMuc;
	}
	public int getSoLuong() {
		return soLuong;
	}
	public void setSoLuong(int soLuong) {
		this.soLuong = soLuong;
	}
	public double getDonGia() {
		return donGia;
	}
	public void setDonGia(double donGia) {
		this.donGia = donGia;
	}
	public float getMucGiamGia() {
		return mucGiamGia;
	}
	public void setMucGiamGia(float mucGiamGia) {
		this.mucGiamGia = mucGiamGia;
	}
	public double getThanhTien() {
		return thanhTien;
	}
	public void setThanhTien(double thanhTien) {
		this.thanhTien = thanhTien;
	}
	public LocalDateTime getThoiGianBaoHanh() {
		return thoiGianBaoHanh;
	}
	public void setThoiGianBaoHanh(LocalDateTime thoiGianBaoHanh) {
		this.thoiGianBaoHanh = thoiGianBaoHanh;
	}
	public String getTrangThai() {
		return trangThai;
	}
	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}
	public String getGhiChu() {
		return ghiChu;
	}
	public void setGhiChu(String ghiChu) {
		this.ghiChu = ghiChu;
	}
	
	public void capNhatTienDo() {
		
	}
	
	public void danhDauHoanThanh() {
		
	}
}
