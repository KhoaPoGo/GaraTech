package entity;

public class PhuTung {
	private String maPhuTung;
	private String tenPhuTung;
	private String loaiPhuTung;
	private String donViTinh;
	private Double donGiaNhap;
	private Double donGiaBan;
	private int soLuongTon;
	private int tonKhoToiThieu;
	private String viTriLuuKho;
	private int thoiGianBaoHanh;
	
	public PhuTung() {
		
	}

	public PhuTung(String maPhuTung, String tenPhuTung, String loaiPhuTung, String donViTinh, Double donGiaNhap,
			Double donGiaBan, int soLuongTon, int tonKhoToiThieu, String viTriLuuKho, int thoiGianBaoHanh) {
		super();
		this.maPhuTung = maPhuTung;
		this.tenPhuTung = tenPhuTung;
		this.loaiPhuTung = loaiPhuTung;
		this.donViTinh = donViTinh;
		this.donGiaNhap = donGiaNhap;
		this.donGiaBan = donGiaBan;
		this.soLuongTon = soLuongTon;
		this.tonKhoToiThieu = tonKhoToiThieu;
		this.viTriLuuKho = viTriLuuKho;
		this.thoiGianBaoHanh = thoiGianBaoHanh;
	}

	public PhuTung(String maPhuTung, String tenPhuTung, String loaiPhuTung, String donViTinh, Double donGiaNhap,
			Double donGiaBan, String viTriLuuKho, int thoiGianBaoHanh) {
		this(maPhuTung, tenPhuTung, loaiPhuTung, donViTinh, donGiaNhap, donGiaBan, 0, 0, viTriLuuKho, thoiGianBaoHanh);
	}

	public String getMaPhuTung() {
		return maPhuTung;
	}

	public void setMaPhuTung(String maPhuTung) {
		this.maPhuTung = maPhuTung;
	}

	public String getTenPhuTung() {
		return tenPhuTung;
	}

	public void setTenPhuTung(String tenPhuTung) {
		this.tenPhuTung = tenPhuTung;
	}

	public String getLoaiPhuTung() {
		return loaiPhuTung;
	}

	public void setLoaiPhuTung(String loaiPhuTung) {
		this.loaiPhuTung = loaiPhuTung;
	}

	public String getDonViTinh() {
		return donViTinh;
	}

	public void setDonViTinh(String donViTinh) {
		this.donViTinh = donViTinh;
	}

	public Double getDonGiaNhap() {
		return donGiaNhap;
	}

	public void setDonGiaNhap(Double donGiaNhap) {
		this.donGiaNhap = donGiaNhap;
	}

	public Double getDonGiaBan() {
		return donGiaBan;
	}

	public void setDonGiaBan(Double donGiaBan) {
		this.donGiaBan = donGiaBan;
	}

	public int getSoLuongTon() {
		return soLuongTon;
	}

	public void setSoLuongTon(int soLuongTon) {
		this.soLuongTon = soLuongTon;
	}

	public int getTonKhoToiThieu() {
		return tonKhoToiThieu;
	}

	public void setTonKhoToiThieu(int tonKhoToiThieu) {
		this.tonKhoToiThieu = tonKhoToiThieu;
	}

	public String getViTriLuuKho() {
		return viTriLuuKho;
	}

	public void setViTriLuuKho(String viTriLuuKho) {
		this.viTriLuuKho = viTriLuuKho;
	}

	public int getThoiGianBaoHanh() {
		return thoiGianBaoHanh;
	}

	public void setThoiGianBaoHanh(int thoiGianBaoHanh) {
		this.thoiGianBaoHanh = thoiGianBaoHanh;
	}
	
	public void capNhatTonKho(int soLuongThayDoi) {
		this.soLuongTon += soLuongThayDoi;
	}
	
	public boolean kiemTraCanhBaoMoi() {
		return this.soLuongTon <= this.tonKhoToiThieu;
	}
}
