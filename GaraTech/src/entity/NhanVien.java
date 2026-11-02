package entity;

import enums.Role;

public class NhanVien {
	private String maNV;
	private String hoTen;
	private String soDienThoai;
	private String email;
	private String diaChi;
	private String vaiTro;
	private double heSoLuong;
	private int luongCoBan;
	private boolean dangLam = true;
	
	public NhanVien() {
		super();
	}

	public NhanVien(String maNV, String hoTen, String soDienThoai, String email, String diaChi, String vaiTro,
			double heSoLuong, int luongCoBan) {
		super();
		this.maNV = maNV;
		this.hoTen = hoTen;
		this.soDienThoai = soDienThoai;
		this.email = email;
		this.diaChi = diaChi;
		this.vaiTro = vaiTro;
		this.heSoLuong = heSoLuong;
		this.luongCoBan = luongCoBan;
	}

	public NhanVien(String maNV, String hoTen, String soDienThoai, String email, String diaChi, String vaiTro,
			double heSoLuong, int luongCoBan, boolean dangLam) {
		this(maNV, hoTen, soDienThoai, email, diaChi, vaiTro, heSoLuong, luongCoBan);
		this.dangLam = dangLam;
	}

	public String getMaNV() {
		return maNV;
	}

	public void setMaNV(String maNV) {
		this.maNV = maNV;
	}

	public String getHoTen() {
		return hoTen;
	}

	public void setHoTen(String hoTen) {
		this.hoTen = hoTen;
	}

	public String getSoDienThoai() {
		return soDienThoai;
	}

	public void setSoDienThoai(String soDienThoai) {
		this.soDienThoai = soDienThoai;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDiaChi() {
		return diaChi;
	}

	public void setDiaChi(String diaChi) {
		this.diaChi = diaChi;
	}

	public String getVaiTro() {
		return vaiTro;
	}

	public void setVaiTro(String vaiTro) {
		this.vaiTro = vaiTro;
	}

	public double getHeSoLuong() {
		return heSoLuong;
	}

	public void setHeSoLuong(double heSoLuong) {
		this.heSoLuong = heSoLuong;
	}

	public int getLuongCoBan() {
		return luongCoBan;
	}

	public void setLuongCoBan(int luongCoBan) {
		this.luongCoBan = luongCoBan;
	}

	/** Cột dangLam trong bảng NhanVien: true = đang làm, false = đã nghỉ việc. */
	public boolean isDangLam() {
		return dangLam;
	}

	public void setDangLam(boolean dangLam) {
		this.dangLam = dangLam;
	}

	/** Vai trò dạng enum (từ chuỗi vaiTro đọc ở DB); null nếu chuỗi không hợp lệ. */
	public Role getRole() {
		return Role.fromDb(vaiTro);
	}

	/** Lương theo hệ số = lương cơ bản × hệ số (chỉ để hiển thị tham khảo). */
	public long luongTheoHeSo() {
		return Math.round(luongCoBan * heSoLuong);
	}

	@Override
	public String toString() {
		return "NhanVien [maNV=" + maNV + ", hoTen=" + hoTen + ", soDienThoai=" + soDienThoai + ", email=" + email
				+ ", diaChi=" + diaChi + ", vaiTro=" + vaiTro + ", heSoLuong=" + heSoLuong + ", luongCoBan="
				+ luongCoBan + ", dangLam=" + dangLam + "]";
	}
}
