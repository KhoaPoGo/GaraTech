package entity;

public class ChiTietHoaDon {
	private String maChiTietHD;
	private String maHoaDon;
	private String loaiChiTiet; // "DichVu" hoặc "PhuTung"
	private String maDichVu;
	private String maPhuTung;
	private String tenChiTiet;
	private String donViTinh;
	private int soLuong;
	private double donGia;
	private double giamGia;
	private double thanhTien;

	public ChiTietHoaDon() {
		super();
	}

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

	public String getMaHoaDon() {
		return maHoaDon;
	}

	public void setMaHoaDon(String maHoaDon) {
		this.maHoaDon = maHoaDon;
	}

	public String getLoaiChiTiet() {
		return loaiChiTiet;
	}

	public void setLoaiChiTiet(String loaiChiTiet) {
		this.loaiChiTiet = loaiChiTiet;
	}

	public String getMaDichVu() {
		return maDichVu;
	}

	public void setMaDichVu(String maDichVu) {
		this.maDichVu = maDichVu;
	}

	public String getMaPhuTung() {
		return maPhuTung;
	}

	public void setMaPhuTung(String maPhuTung) {
		this.maPhuTung = maPhuTung;
	}

	public String getTenChiTiet() {
		return tenChiTiet;
	}

	public void setTenChiTiet(String tenChiTiet) {
		this.tenChiTiet = tenChiTiet;
	}

	public String getDonViTinh() {
		return donViTinh;
	}

	public void setDonViTinh(String donViTinh) {
		this.donViTinh = donViTinh;
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

	public void tinhThanhTien() {
	    this.thanhTien = Math.max(0.0, soLuong * donGia - giamGia);
	}

	@Override
	public String toString() {
		return "ChiTietHoaDon [maChiTietHD=" + maChiTietHD + ", loaiChiTiet=" + loaiChiTiet + ", tenChiTiet="
				+ tenChiTiet + ", soLuong=" + soLuong + ", donGia=" + donGia + ", giamGia=" + giamGia + ", thanhTien="
				+ thanhTien + "]";
	}
}
