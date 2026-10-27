package entity;

import java.time.LocalDateTime;

public class PhieuXuatKho {
	private String maPhieuXK;
	private LocalDateTime ngayXuat;
	private String lyDoXuat;
	private double tongGiaTri;
	private String trangThai;
	
	public PhieuXuatKho() {
		super();
	}

	public PhieuXuatKho(String maPhieuXK, LocalDateTime ngayXuat, String lyDoXuat, double tongGiaTri,
			String trangThai) {
		super();
		this.maPhieuXK = maPhieuXK;
		this.ngayXuat = ngayXuat;
		this.lyDoXuat = lyDoXuat;
		this.tongGiaTri = tongGiaTri;
		this.trangThai = trangThai;
	}

	public String getMaPhieuXK() {
		return maPhieuXK;
	}

	public void setMaPhieuXK(String maPhieuXK) {
		this.maPhieuXK = maPhieuXK;
	}

	public LocalDateTime getNgayXuat() {
		return ngayXuat;
	}

	public void setNgayXuat(LocalDateTime ngayXuat) {
		this.ngayXuat = ngayXuat;
	}

	public String getLyDoXuat() {
		return lyDoXuat;
	}

	public void setLyDoXuat(String lyDoXuat) {
		this.lyDoXuat = lyDoXuat;
	}

	public double getTongGiaTri() {
		return tongGiaTri;
	}

	public void setTongGiaTri(double tongGiaTri) {
		this.tongGiaTri = tongGiaTri;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	@Override
	public String toString() {
		return "PhieuXuatKho [maPhieuXK=" + maPhieuXK + ", ngayXuat=" + ngayXuat + ", lyDoXuat=" + lyDoXuat
				+ ", tongGiaTri=" + tongGiaTri + ", trangThai=" + trangThai + "]";
	}
	
	
}
