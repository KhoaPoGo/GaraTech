package entity;

import java.time.LocalDateTime;

public class Xe {
	private String bienSo;
	private KhachHang khachHang;
	private String hangXe;
	private String model;
	private int namSanXuat;
	private String mauXe;
	private String soMay;
	private float soKmHienTai;
	private LocalDateTime ngayDangKy;
	
	public Xe() {
		super();
	}

	public Xe(String bienSo, KhachHang khachHang, String hangXe, String model, int namSanXuat, String mauXe,
			String soMay, float soKmHienTai, LocalDateTime ngayDangKy) {
		super();
		this.bienSo = bienSo;
		this.khachHang = khachHang;
		this.hangXe = hangXe;
		this.model = model;
		this.namSanXuat = namSanXuat;
		this.mauXe = mauXe;
		this.soMay = soMay;
		this.soKmHienTai = soKmHienTai;
		this.ngayDangKy = ngayDangKy;
	}

	public String getBienSo() {
		return bienSo;
	}

	public void setBienSo(String bienSo) {
		this.bienSo = bienSo;
	}

	public KhachHang getKhachHang() {
		return khachHang;
	}

	public void setKhachHang(KhachHang khachHang) {
		this.khachHang = khachHang;
	}

	public String getHangXe() {
		return hangXe;
	}

	public void setHangXe(String hangXe) {
		this.hangXe = hangXe;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public int getNamSanXuat() {
		return namSanXuat;
	}

	public void setNamSanXuat(int namSanXuat) {
		this.namSanXuat = namSanXuat;
	}

	public String getMauXe() {
		return mauXe;
	}

	public void setMauXe(String mauXe) {
		this.mauXe = mauXe;
	}

	public String getSoMay() {
		return soMay;
	}

	public void setSoMay(String soMay) {
		this.soMay = soMay;
	}

	public float getSoKmHienTai() {
		return soKmHienTai;
	}

	public void setSoKmHienTai(float soKmHienTai) {
		this.soKmHienTai = soKmHienTai;
	}

	public LocalDateTime getNgayDangKy() {
		return ngayDangKy;
	}

	public void setNgayDangKy(LocalDateTime ngayDangKy) {
		this.ngayDangKy = ngayDangKy;
	}

	@Override
	public String toString() {
		return "Xe [bienSo=" + bienSo + ", khachHang=" + khachHang + ", hangXe=" + hangXe + ", model=" + model
				+ ", namSanXuat=" + namSanXuat + ", mauXe=" + mauXe + ", soMay=" + soMay + ", soKmHienTai="
				+ soKmHienTai + ", ngayDangKy=" + ngayDangKy + "]";
	}
	
}
