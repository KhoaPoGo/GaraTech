package entity;

public class ChiTietHoaDon {
	private String maChiTietHD;
	private String loaiChiTiet;
	private String tenChiTiet;
	private int soLuong;
	private double donGia;
	private double giamGia;
	private double thanhTien;

	public ChiTietHoaDon(String maChiTietHD) {
		super();
		this.maChiTietHD = maChiTietHD;
	}

	public ChiTietHoaDon(String maChiTietHD, String loaiChiTiet, String tenChiTiet, int soLuong, double donGia,
			double giamGia, double thanhTien) {
		super();
		this.maChiTietHD = maChiTietHD;
		this.loaiChiTiet = loaiChiTiet;
		this.tenChiTiet = tenChiTiet;
		this.soLuong = soLuong;
		this.donGia = donGia;
		this.giamGia = giamGia;
		this.thanhTien = thanhTien;
	}

	public String getMaChiTietHD() {
		return maChiTietHD;
	}

	public void setMaChiTietHD(String maChiTietHD) {
		this.maChiTietHD = maChiTietHD;
	}

	public String getLoaiChiTiet() {
		return loaiChiTiet;
	}

	public void setLoaiChiTiet(String loaiChiTiet) {
		this.loaiChiTiet = loaiChiTiet;
	}

	public String getTenChiTiet() {
		return tenChiTiet;
	}

	public void setTenChiTiet(String tenChiTiet) {
		this.tenChiTiet = tenChiTiet;
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

	public double getGiamGia() {
		return giamGia;
	}

	public void setGiamGia(double giamGia) {
		this.giamGia = giamGia;
	}

	public double getThanhTien() {
		return thanhTien;
	}

	public void setThanhTien(double thanhTien) {
		this.thanhTien = thanhTien;
	}

	// tinh thanh tien
	public void tinhThanhTien() {
		this.thanhTien = soLuong * donGia;

		// ap dung giam gia

	}

	@Override
	public String toString() {
		return "ChiTietHoaDon [maChiTietHD=" + maChiTietHD + ", loaiChiTiet=" + loaiChiTiet + ", tenChiTiet="
				+ tenChiTiet + ", soLuong=" + soLuong + ", donGia=" + donGia + ", giamGia=" + giamGia + ", thanhTien="
				+ thanhTien + "]";
	}

}
