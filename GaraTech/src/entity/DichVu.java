package entity;

import java.time.LocalDateTime;

public class DichVu {
	private String maDichVu;
	private String tenDichVu;
	private String moTa;
	private double tienCong;
	private String capDoBaoDuong;
	private String trangThai;

	public DichVu(String maDichVu) {
		super();
		this.maDichVu = maDichVu;
	}

	public DichVu(String maDichVu, String tenDichVu, String moTa, double tienCong, String capDoBaoDuong,
			String trangThai) {
		super();
		this.maDichVu = maDichVu;
		this.tenDichVu = tenDichVu;
		this.moTa = moTa;
		this.tienCong = tienCong;
		this.capDoBaoDuong = capDoBaoDuong;
		this.trangThai = trangThai;
	}

	public String getMaDichVu() {
		return maDichVu;
	}

	public void setMaDichVu(String maDichVu) {
		this.maDichVu = maDichVu;
	}

	public String getTenDichVu() {
		return tenDichVu;
	}

	public void setTenDichVu(String tenDichVu) {
		this.tenDichVu = tenDichVu;
	}

	public String getMoTa() {
		return moTa;
	}

	public void setMoTa(String moTa) {
		this.moTa = moTa;
	}

	public double getTienCong() {
		return tienCong;
	}

	public void setTienCong(double tienCong) {
		this.tienCong = tienCong;
	}

	public String getCapDoBaoDuong() {
		return capDoBaoDuong;
	}

	public void setCapDoBaoDuong(String capDoBaoDuong) {
		this.capDoBaoDuong = capDoBaoDuong;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	@Override
	public String toString() {
		return "DichVu [maDichVu=" + maDichVu + ", tenDichVu=" + tenDichVu + ", moTa=" + moTa + ", tienCong=" + tienCong
				+ ", capDoBaoDuong=" + capDoBaoDuong + ", trangThai=" + trangThai + "]";
	}

}
