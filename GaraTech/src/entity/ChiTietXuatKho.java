package entity;

public class ChiTietXuatKho {
	private String maChiTietXK;
	private int soLuongXuat;
	private double donGiaXuat;
	private double thanhTien;
	private String ghiChu;
	
	
	public ChiTietXuatKho() {
		super();
	}

	public ChiTietXuatKho(String maChiTietXK, int soLuongXuat, double donGiaXuat, double thanhTien, String ghiChu) {
		super();
		this.maChiTietXK = maChiTietXK;
		this.soLuongXuat = soLuongXuat;
		this.donGiaXuat = donGiaXuat;
		this.thanhTien = thanhTien;
		this.ghiChu = ghiChu;
	}

	public String getMaChiTietXK() {
		return maChiTietXK;
	}

	public void setMaChiTietXK(String maChiTietXK) {
		this.maChiTietXK = maChiTietXK;
	}

	public int getSoLuongXuat() {
		return soLuongXuat;
	}

	public void setSoLuongXuat(int soLuongXuat) {
		this.soLuongXuat = soLuongXuat;
	}

	public double getDonGiaXuat() {
		return donGiaXuat;
	}

	public void setDonGiaXuat(double donGiaXuat) {
		this.donGiaXuat = donGiaXuat;
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
		return "ChiTietXuatKho [maChiTietXK=" + maChiTietXK + ", soLuongXuat=" + soLuongXuat + ", donGiaXuat="
				+ donGiaXuat + ", thanhTien=" + thanhTien + ", ghiChu=" + ghiChu + "]";
	}
	
	
}
