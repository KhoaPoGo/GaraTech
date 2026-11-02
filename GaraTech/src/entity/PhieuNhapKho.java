package entity;

import java.time.LocalDateTime;

public class PhieuNhapKho {
	private String maPhieuNK;
	private LocalDateTime ngayNhap;
	private NhaCungCap nhaCungCap;
	private double tongGiaTri;
	private String trangThai;
	
	public PhieuNhapKho() {
		super();
	}

	public PhieuNhapKho(String maPhieuNK, LocalDateTime ngayNhap, NhaCungCap nhaCungCap, double tongGiaTri,
			String trangThai) {
		super();
		this.maPhieuNK = maPhieuNK;
		this.ngayNhap = ngayNhap;
		this.nhaCungCap = nhaCungCap;
		this.tongGiaTri = tongGiaTri;
		this.trangThai = trangThai;
	}

	public String getMaPhieuNK() {
		return maPhieuNK;
	}

	public void setMaPhieuNK(String maPhieuNK) {
		this.maPhieuNK = maPhieuNK;
	}

	public LocalDateTime getNgayNhap() {
		return ngayNhap;
	}

	public void setNgayNhap(LocalDateTime ngayNhap) {
		this.ngayNhap = ngayNhap;
	}

	public NhaCungCap getNhaCungCap() {
		return nhaCungCap;
	}

	public void setNhaCungCap(NhaCungCap nhaCungCap) {
		this.nhaCungCap = nhaCungCap;
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
		return "PhieuNhapKho [maPhieuNK=" + maPhieuNK + ", ngayNhap=" + ngayNhap + ", nhaCungCap=" + nhaCungCap
				+ ", tongGiaTri=" + tongGiaTri + ", trangThai=" + trangThai + "]";
	}
	
	
}
