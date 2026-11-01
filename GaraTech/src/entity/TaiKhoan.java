package entity;

public class TaiKhoan {
	private String tenDangNhap;
	private String matKhau;
	private String trangThaiDangNhap;
	private NhanVien maNV;
	
	
	public TaiKhoan() {
		super();
	}

	public TaiKhoan(String tenDangNhap, String matKhau, String trangThaiDangNhap, NhanVien maNV) {
		super();
		this.tenDangNhap = tenDangNhap;
		this.matKhau = matKhau;
		this.trangThaiDangNhap = trangThaiDangNhap;
		this.maNV = maNV;
	}

	public String getTenDangNhap() {
		return tenDangNhap;
	}

	public void setTenDangNhap(String tenDangNhap) {
		this.tenDangNhap = tenDangNhap;
	}

	public String getMatKhau() {
		return matKhau;
	}

	public void setMatKhau(String matKhau) {
		this.matKhau = matKhau;
	}

	public String getTrangThaiDangNhap() {
		return trangThaiDangNhap;
	}

	public void setTrangThaiDangNhap(String trangThaiDangNhap) {
		this.trangThaiDangNhap = trangThaiDangNhap;
	}

	public NhanVien getMaNV() {
		return maNV;
	}

	public void setMaNV(NhanVien maNV) {
		this.maNV = maNV;
	}

	@Override
	public String toString() {
		return "TaiKhoan [tenDangNhap=" + tenDangNhap + ", matKhau=" + matKhau + ", trangThaiDangNhap="
				+ trangThaiDangNhap + ", maNV=" + maNV + "]";
	}
	
}
