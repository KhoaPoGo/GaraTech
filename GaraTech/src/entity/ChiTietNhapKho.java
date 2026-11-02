package entity;

public class ChiTietNhapKho {
	private String maChiTietNK;
	private int soLuongNhap;
	private double donGiaNhap;
	private double thanhTien;
	private String ghiChu;
	
	public ChiTietNhapKho() {
		super();
	}

	public ChiTietNhapKho(String maChiTietNK, int soLuongNhap, double donGiaNhap, double thanhTien, String ghiChu) {
		super();
		this.maChiTietNK = maChiTietNK;
		this.soLuongNhap = soLuongNhap;
		this.donGiaNhap = donGiaNhap;
		this.thanhTien = thanhTien;
		this.ghiChu = ghiChu;
	}

	public String getMaChiTietNK() {
		return maChiTietNK;
	}

	public void setMaChiTietNK(String maChiTietNK) {
		this.maChiTietNK = maChiTietNK;
	}

	public int getSoLuongNhap() {
		return soLuongNhap;
	}

	public void setSoLuongNhap(int soLuongNhap) {
		this.soLuongNhap = soLuongNhap;
	}

	public double getDonGiaNhap() {
		return donGiaNhap;
	}

	public void setDonGiaNhap(double donGiaNhap) {
		this.donGiaNhap = donGiaNhap;
	}

	public double getThanhTien() {
		return thanhTien;
	}

	public void setThanhTien(double thanhTien) {
		this.thanhTien = thanhTien;
	}

	public String getGhiChu() {
		return ghiChu;
	}

	public void setGhiChu(String ghiChu) {
		this.ghiChu = ghiChu;
	}

	@Override
	public String toString() {
		return "ChiTietNhapKho [maChiTietNK=" + maChiTietNK + ", soLuongNhap=" + soLuongNhap + ", donGiaNhap="
				+ donGiaNhap + ", thanhTien=" + thanhTien + ", ghiChu=" + ghiChu + "]";
	}
	
	
}
