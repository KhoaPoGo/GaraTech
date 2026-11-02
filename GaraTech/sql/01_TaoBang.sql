-- GaraTech - SQL Server schema (chay bang SSMS)
IF DB_ID(N'GaraTech') IS NULL CREATE DATABASE GaraTechDB;
GO
USE GaraTechDB;
GO

-- ===== NHAN SU & TAI KHOAN =====
CREATE TABLE NhanVien (
    maNV         VARCHAR(10)   NOT NULL PRIMARY KEY,           -- NV26001
    hoTen        NVARCHAR(100) NOT NULL,
    soDienThoai  VARCHAR(10)   NOT NULL UNIQUE,
    email        VARCHAR(100)  NOT NULL UNIQUE,
    diaChi       NVARCHAR(200) NOT NULL,
    vaiTro       NVARCHAR(30)  NOT NULL,
    heSoLuong    DECIMAL(3,2)  NOT NULL,
    luongCoBan   INT           NOT NULL,
    dangLam      BIT           NOT NULL DEFAULT 1,
    CONSTRAINT CK_NV_sdt    CHECK (LEN(soDienThoai)=10 AND soDienThoai NOT LIKE '%[^0-9]%' AND soDienThoai LIKE '0%'),
    CONSTRAINT CK_NV_vaitro CHECK (vaiTro IN (N'Quản lý', N'Nhân viên tiếp nhận', N'Kỹ thuật viên', N'Nhân viên kho')),
    CONSTRAINT CK_NV_heso   CHECK (heSoLuong BETWEEN 1.0 AND 5.0), 
    CONSTRAINT CK_NV_luong  CHECK (luongCoBan > 0)
);

CREATE TABLE TaiKhoan (
    tenDangNhap       VARCHAR(50)  NOT NULL PRIMARY KEY,
    matKhau           VARCHAR(200) NOT NULL,                    -- hash PBKDF2, KHONG luu mat khau tho
    trangThaiDangNhap NVARCHAR(20) NOT NULL DEFAULT N'Hoạt động',
    maNV              VARCHAR(10)  NOT NULL UNIQUE REFERENCES NhanVien(maNV),
    CONSTRAINT CK_TK_ten CHECK (LEN(tenDangNhap) >= 5 AND tenDangNhap NOT LIKE '% %'),
    CONSTRAINT CK_TK_tt  CHECK (trangThaiDangNhap IN (N'Hoạt động', N'Đang khóa'))
);

-- ===== KHACH HANG & XE =====
CREATE TABLE KhachHang (
    maKH         VARCHAR(12)   NOT NULL PRIMARY KEY,            -- KH-2610-001
    hoTen        NVARCHAR(100) NOT NULL,
    soDienThoai  VARCHAR(10)   NOT NULL UNIQUE,
    diaChi       NVARCHAR(200) NULL,
    email        VARCHAR(100)  NULL,
    diemTichLuy  INT           NOT NULL DEFAULT 0,
    CONSTRAINT CK_KH_sdt  CHECK (LEN(soDienThoai)=10 AND soDienThoai NOT LIKE '%[^0-9]%' AND soDienThoai LIKE '0%'),
    CONSTRAINT CK_KH_diem CHECK (diemTichLuy >= 0)
);

CREATE TABLE Xe (
    bienSo        VARCHAR(12)   NOT NULL PRIMARY KEY,
    maKH          VARCHAR(12)   NOT NULL REFERENCES KhachHang(maKH),   -- NO ACTION = RESTRICT
    hangXe        NVARCHAR(50)  NOT NULL,
    model         NVARCHAR(50)  NOT NULL,
    namSanXuat    INT           NOT NULL,
    mauXe         NVARCHAR(30)  NOT NULL,
    soMay         VARCHAR(20)   NULL,
    soKmHienTai   FLOAT         NOT NULL DEFAULT 0,
    ngayDangKy    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CK_Xe_nam CHECK (namSanXuat >= 1980),
    CONSTRAINT CK_Xe_km  CHECK (soKmHienTai >= 0)
);

-- ===== DANH MUC =====
CREATE TABLE DichVu (
    maDichVu        VARCHAR(10)   NOT NULL PRIMARY KEY,         -- DV0001
    tenDichVu       NVARCHAR(150) NOT NULL,
    moTa            NVARCHAR(500) NULL,
    donGiaCong      DECIMAL(18,0) NOT NULL,
    thoiGianUocTinh FLOAT         NOT NULL,
    capDoBaoDuong   NVARCHAR(10)  NULL,
    trangThai       NVARCHAR(20)  NOT NULL DEFAULT N'Hoạt động',
    CONSTRAINT CK_DV_gia CHECK (donGiaCong >= 0),
    CONSTRAINT CK_DV_tg  CHECK (thoiGianUocTinh > 0),
    CONSTRAINT CK_DV_cap CHECK (capDoBaoDuong IS NULL OR capDoBaoDuong IN (N'Cấp 1', N'Cấp 2', N'Cấp 3', N'Cấp 4')),
    CONSTRAINT CK_DV_tt  CHECK (trangThai IN (N'Hoạt động', N'Ngừng cung cấp'))
);

CREATE TABLE PhuTung (
    maPhuTung       VARCHAR(10)   NOT NULL PRIMARY KEY,         -- PT0001
    tenPhuTung      NVARCHAR(100) NOT NULL,
    loaiPhuTung     NVARCHAR(50)  NOT NULL,
    donViTinh       NVARCHAR(20)  NOT NULL,
    donGiaNhap      DECIMAL(18,0) NOT NULL,
    donGiaBan       DECIMAL(18,0) NOT NULL,
    soLuongTon      INT           NOT NULL DEFAULT 0,
    tonKhoToiThieu  INT           NOT NULL DEFAULT 0,
    viTriLuuKho     NVARCHAR(50)  NULL,
    thoiGianBaoHanh INT           NOT NULL DEFAULT 0,           -- thang
    CONSTRAINT CK_PT_gia CHECK (donGiaNhap >= 0 AND donGiaBan >= 0),
    CONSTRAINT CK_PT_ton CHECK (soLuongTon >= 0),               -- >= 0 (report ghi > 0 la sai: het hang thi ton = 0)
    CONSTRAINT CK_PT_min CHECK (tonKhoToiThieu >= 0),
    CONSTRAINT CK_PT_bh  CHECK (thoiGianBaoHanh >= 0)
);

CREATE TABLE NhaCungCap (
    maNCC        VARCHAR(10)   NOT NULL PRIMARY KEY,
    tenNCC       NVARCHAR(150) NOT NULL,
    soDienThoai  VARCHAR(10)   NULL,
    diaChi       NVARCHAR(200) NULL
);

-- ===== TIEP NHAN & SUA CHUA =====
CREATE TABLE PhieuTiepNhan (
    maPhieuTN        VARCHAR(20)   NOT NULL PRIMARY KEY,        -- PTN-20261031-001
    bienSo           VARCHAR(12)   NOT NULL REFERENCES Xe(bienSo),
    maKH             VARCHAR(12)   NOT NULL REFERENCES KhachHang(maKH),
    maNV             VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    thoiGianTiepNhan DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    thoiGianHenTra   DATETIME2     NULL,
    tinhTrangBanDau  NVARCHAR(500) NOT NULL,
    yeuCauKhachHang  NVARCHAR(500) NOT NULL,
    mucNhienLieu     FLOAT         NOT NULL DEFAULT 0,
    soKmTiepNhan     FLOAT         NOT NULL DEFAULT 0,
    trangThai        NVARCHAR(30)  NOT NULL DEFAULT N'Chờ khách hàng xác nhận',
    CONSTRAINT CK_PTN_tt  CHECK (trangThai IN (N'Chờ khách hàng xác nhận', N'Chờ khách xác nhận', N'Đã xác nhận', N'Đã hủy')),
    CONSTRAINT CK_PTN_gio CHECK (thoiGianHenTra IS NULL OR thoiGianHenTra > thoiGianTiepNhan),
    CONSTRAINT CK_PTN_nl  CHECK (mucNhienLieu >= 0 AND soKmTiepNhan >= 0)
);

CREATE TABLE PhieuSuaChua (
    maPhieuSC         VARCHAR(20)   NOT NULL PRIMARY KEY,       -- PSC-20261031-001
    maPhieuTN         VARCHAR(20)   NOT NULL UNIQUE REFERENCES PhieuTiepNhan(maPhieuTN),
    maKTV             VARCHAR(10)   NULL REFERENCES NhanVien(maNV),   -- NULL cho den khi phan cong
    maNVLap           VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    thoiGianDuKien    DATETIME2     NULL,
    thoiGianBatDau    DATETIME2     NULL,
    thoiGianHoanThanh DATETIME2     NULL,
    ghiChuKyThuat     NVARCHAR(500) NULL,
    lyDoHuy           NVARCHAR(300) NULL,
    trangThai         NVARCHAR(30)  NOT NULL DEFAULT N'Chờ khách duyệt',
    -- report co 2 danh sach trang thai khac nhau (quy trinh 2.3 vs class spec); day la ban gop, nhom can chot
    CONSTRAINT CK_PSC_tt  CHECK (trangThai IN (N'Chờ khách duyệt', N'Đã xác nhận', N'Đang sửa chữa',
                                               N'Chờ kiểm tra', N'Hoàn thành', N'Đã bàn giao', N'Đã hủy')),
    CONSTRAINT CK_PSC_gio CHECK (thoiGianHoanThanh IS NULL OR thoiGianHoanThanh >= thoiGianBatDau)
);

CREATE TABLE HangMucSuaChua (
    maHangMuc      VARCHAR(10)   NOT NULL PRIMARY KEY,          -- HM0001
    maPhieuSC      VARCHAR(20)   NOT NULL REFERENCES PhieuSuaChua(maPhieuSC),
    loaiHangMuc    VARCHAR(10)   NOT NULL,
    maDichVu       VARCHAR(10)   NULL REFERENCES DichVu(maDichVu),
    maPhuTung      VARCHAR(10)   NULL REFERENCES PhuTung(maPhuTung),
    tenHangMuc     NVARCHAR(200) NOT NULL,
    soLuong        INT           NOT NULL,
    donGia         DECIMAL(18,0) NOT NULL,
    mucGiamGia     DECIMAL(18,0) NOT NULL DEFAULT 0,
    thanhTien      AS (soLuong * donGia - mucGiamGia) PERSISTED,
    laPhatSinh     BIT           NOT NULL DEFAULT 0,
    thoiHanBaoHanh DATETIME2     NULL,
    trangThai      NVARCHAR(20)  NOT NULL DEFAULT N'Chờ duyệt',
    ghiChu         NVARCHAR(300) NULL,
    CONSTRAINT CK_HM_loai CHECK (loaiHangMuc IN ('DichVu','PhuTung')),
    CONSTRAINT CK_HM_tt   CHECK (trangThai IN (N'Chờ duyệt', N'Đã xác nhận', N'Từ chối', N'Hoàn thành')),
    CONSTRAINT CK_HM_sl   CHECK (soLuong > 0 AND donGia >= 0 AND mucGiamGia >= 0),
    -- moi dong chi la DichVu HOAC PhuTung, khong ca hai
    CONSTRAINT CK_HM_xor  CHECK ((loaiHangMuc='DichVu'  AND maDichVu IS NOT NULL AND maPhuTung IS NULL)
                              OR (loaiHangMuc='PhuTung' AND maPhuTung IS NOT NULL AND maDichVu IS NULL))
);

-- ===== KHO =====
CREATE TABLE PhieuNhapKho (
    maPhieuNhap VARCHAR(20)   NOT NULL PRIMARY KEY,             -- PNK-20261031-001
    ngayNhap    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    maNVKho     VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    maNCC       VARCHAR(10)   NOT NULL REFERENCES NhaCungCap(maNCC),
    tongGiaTri  DECIMAL(18,0) NOT NULL DEFAULT 0,
    ghiChu      NVARCHAR(300) NULL,
    CONSTRAINT CK_PNK_gt CHECK (tongGiaTri >= 0)
);

CREATE TABLE ChiTietNhapKho (
    maPhieuNhap VARCHAR(20)   NOT NULL REFERENCES PhieuNhapKho(maPhieuNhap),
    maPhuTung   VARCHAR(10)   NOT NULL REFERENCES PhuTung(maPhuTung),
    soLuongNhap INT           NOT NULL,
    donGiaNhap  DECIMAL(18,0) NOT NULL,
    thanhTien   AS (soLuongNhap * donGiaNhap) PERSISTED,
    PRIMARY KEY (maPhieuNhap, maPhuTung),
    CONSTRAINT CK_CTNK CHECK (soLuongNhap > 0 AND donGiaNhap >= 0)
);

-- KTV gui yeu cau -> phieu 'Cho xac nhan'; kho bam xac nhan -> 'Da xuat kho' + tru ton
CREATE TABLE PhieuXuatKho (
    maPhieuXK   VARCHAR(20)   NOT NULL PRIMARY KEY,             -- PXK-20261031-001
    maPhieuSC   VARCHAR(20)   NOT NULL REFERENCES PhieuSuaChua(maPhieuSC),
    maKTVYeuCau VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    maNVKho     VARCHAR(10)   NULL REFERENCES NhanVien(maNV),   -- dien khi kho xac nhan/tu choi
    ngayYeuCau  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    ngayXuat    DATETIME2     NULL,
    lyDoXuat    NVARCHAR(200) NOT NULL,
    lyDoTuChoi  NVARCHAR(200) NULL,
    tongGiaTri  DECIMAL(18,0) NOT NULL DEFAULT 0,
    trangThai   NVARCHAR(20)  NOT NULL DEFAULT N'Chờ xác nhận',
    CONSTRAINT CK_PXK_tt CHECK (trangThai IN (N'Chờ xác nhận', N'Đã xuất kho', N'Đã hủy')),
    CONSTRAINT CK_PXK_gt CHECK (tongGiaTri >= 0)
);

CREATE TABLE ChiTietXuatKho (
    maChiTietXK VARCHAR(12)   NOT NULL PRIMARY KEY,             -- CTXK00001
    maPhieuXK   VARCHAR(20)   NOT NULL REFERENCES PhieuXuatKho(maPhieuXK),
    maPhuTung   VARCHAR(10)   NOT NULL REFERENCES PhuTung(maPhuTung),
    soLuongXuat INT           NOT NULL,
    donGiaXuat  DECIMAL(18,0) NOT NULL,                         -- chot gia luc lap phieu
    thanhTien   AS (soLuongXuat * donGiaXuat) PERSISTED,
    ghiChu      NVARCHAR(200) NULL,
    CONSTRAINT UQ_CTXK UNIQUE (maPhieuXK, maPhuTung),
    CONSTRAINT CK_CTXK CHECK (soLuongXuat > 0 AND donGiaXuat >= 0)
);

-- ===== HOA DON & THANH TOAN =====
CREATE TABLE HoaDon (
    maHoaDon          VARCHAR(20)   NOT NULL PRIMARY KEY,       -- HD20261031-0001
    maPhieuSC         VARCHAR(20)   NOT NULL UNIQUE REFERENCES PhieuSuaChua(maPhieuSC),  -- 1 phieu = toi da 1 hoa don
    maNV              VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    ngayLap           DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    tongTienDichVu    DECIMAL(18,0) NOT NULL DEFAULT 0,
    tongTienPhuTung   DECIMAL(18,0) NOT NULL DEFAULT 0,
    chiPhiKhac        DECIMAL(18,0) NOT NULL DEFAULT 0,
    giamGia           DECIMAL(18,0) NOT NULL DEFAULT 0,
    thueVAT           DECIMAL(5,2)  NOT NULL DEFAULT 10,
    tongTienThanhToan DECIMAL(18,0) NOT NULL DEFAULT 0,
    trangThai         NVARCHAR(30)  NOT NULL DEFAULT N'Chưa thanh toán',
    CONSTRAINT CK_HD_tien CHECK (tongTienDichVu>=0 AND tongTienPhuTung>=0 AND chiPhiKhac>=0 AND giamGia>=0 AND tongTienThanhToan>=0),
    CONSTRAINT CK_HD_vat  CHECK (thueVAT BETWEEN 0 AND 100),
    CONSTRAINT CK_HD_giam CHECK (giamGia <= tongTienDichVu + tongTienPhuTung + chiPhiKhac),
    CONSTRAINT CK_HD_tt   CHECK (trangThai IN (N'Chưa thanh toán', N'Đã thanh toán một phần', N'Đã thanh toán', N'Đã hủy'))
);

CREATE TABLE ChiTietHoaDon (
    maChiTietHD VARCHAR(12)   NOT NULL PRIMARY KEY,             -- CTHD00001
    maHoaDon    VARCHAR(20)   NOT NULL REFERENCES HoaDon(maHoaDon) ON DELETE CASCADE,
    loaiChiTiet VARCHAR(10)   NOT NULL,
    maDichVu    VARCHAR(10)   NULL REFERENCES DichVu(maDichVu),
    maPhuTung   VARCHAR(10)   NULL REFERENCES PhuTung(maPhuTung),
    tenChiTiet  NVARCHAR(200) NOT NULL,                         -- sao chep ten luc lap
    soLuong     INT           NOT NULL,
    donGia      DECIMAL(18,0) NOT NULL,                         -- chot gia
    giamGia     DECIMAL(18,0) NOT NULL DEFAULT 0,
    thanhTien   AS (soLuong * donGia - giamGia) PERSISTED,
    CONSTRAINT CK_CTHD_loai CHECK (loaiChiTiet IN ('DichVu','PhuTung')),
    CONSTRAINT CK_CTHD_so   CHECK (soLuong > 0 AND donGia >= 0 AND giamGia >= 0 AND giamGia <= soLuong*donGia),
    CONSTRAINT CK_CTHD_xor  CHECK ((loaiChiTiet='DichVu'  AND maDichVu IS NOT NULL AND maPhuTung IS NULL)
                                OR (loaiChiTiet='PhuTung' AND maPhuTung IS NOT NULL AND maDichVu IS NULL))
);

CREATE TABLE GiaoDichThanhToan (
    maGiaoDich    VARCHAR(20)   NOT NULL PRIMARY KEY,           -- GD-20261031-001
    maHoaDon      VARCHAR(20)   NOT NULL REFERENCES HoaDon(maHoaDon) ON DELETE CASCADE,
    maNV          VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    ngayThanhToan DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    soTien        DECIMAL(18,0) NOT NULL,
    phuongThuc    NVARCHAR(20)  NOT NULL,
    trangThai     NVARCHAR(20)  NOT NULL DEFAULT N'Chờ thanh toán',
    CONSTRAINT CK_GD_tien CHECK (soTien >= 0),
    CONSTRAINT CK_GD_pt   CHECK (phuongThuc IN (N'Tiền mặt', N'Chuyển khoản', N'Thẻ')),
    CONSTRAINT CK_GD_tt   CHECK (trangThai IN (N'Chờ thanh toán', N'Đã thanh toán', N'Hủy'))
);
GO
