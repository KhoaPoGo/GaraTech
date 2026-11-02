package entity;

public class KhachHang {
	private String maKH;
	private String hoTen;
	private String soDienThoai;
	private String diaChi;
	private String email;
	private int diemTichLuy;
	
	
	public KhachHang() {
		super();
	}

	public KhachHang(String maKH, String hoTen, String soDienThoai, String diaChi, String email, int diemTichLuy) {
		super();
		this.maKH = maKH;
		this.hoTen = hoTen;
		this.soDienThoai = soDienThoai;
		this.diaChi = diaChi;
		this.email = email;
		this.diemTichLuy = diemTichLuy;
	}

	public String getMaKH() {
		return maKH;
	}

	public void setMaKH(String maKH) {
		this.maKH = maKH;
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

	public String getDiaChi() {
		return diaChi;
	}

	public void setDiaChi(String diaChi) {
		this.diaChi = diaChi;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getDiemTichLuy() {
		return diemTichLuy;
	}

	public void setDiemTichLuy(int diemTichLuy) {
		this.diemTichLuy = diemTichLuy;
	}

	@Override
	public String toString() {
		return "KhachHang [maKH=" + maKH + ", hoTen=" + hoTen + ", soDienThoai=" + soDienThoai + ", diaChi=" + diaChi
				+ ", email=" + email + ", diemTichLuy=" + diemTichLuy + "]";
	}
}
