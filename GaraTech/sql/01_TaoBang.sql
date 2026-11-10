-- =====================================================================
-- GaraTech - SQL Server Schema: TÁI TẠO CƠ SỞ DỮ LIỆU GARATECH
-- Áp dụng mô hình Phiếu Tiếp nhận - Sửa chữa duy nhất (PhieuTiepNhanSuaChua)
-- 4 Trạng thái chuẩn: CHO_SUA, DANG_SUA, CHO_LINH_KIEN, HOAN_TAT
-- =====================================================================

USE master;
GO

IF DB_ID(N'GaraTechDB') IS NOT NULL
BEGIN
    ALTER DATABASE GaraTechDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE GaraTechDB;
    PRINT N'-> Đã xóa cơ sở dữ liệu cũ GaraTechDB.';
END;
GO

CREATE DATABASE GaraTechDB;
GO

USE GaraTechDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
GO

PRINT N'-> Đang khởi tạo các bảng trong GaraTechDB...';
GO

-- 1. NHÂN SỰ & TÀI KHOẢN
CREATE TABLE NhanVien (
    maNV         VARCHAR(10)   NOT NULL PRIMARY KEY,
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
GO

CREATE TABLE TaiKhoan (
    tenDangNhap       VARCHAR(50)  NOT NULL PRIMARY KEY,
    matKhau           VARCHAR(200) NOT NULL,
    trangThaiDangNhap NVARCHAR(20) NOT NULL DEFAULT N'Hoạt động',
    maNV              VARCHAR(10)  NOT NULL UNIQUE REFERENCES NhanVien(maNV),
    CONSTRAINT CK_TK_ten CHECK (LEN(tenDangNhap) >= 5 AND tenDangNhap NOT LIKE '% %'),
    CONSTRAINT CK_TK_tt  CHECK (trangThaiDangNhap IN (N'Hoạt động', N'Đang khóa'))
);
GO

-- 2. KHÁCH HÀNG & PHƯƠNG TIỆN
CREATE TABLE KhachHang (
    maKH         VARCHAR(12)   NOT NULL PRIMARY KEY,
    hoTen        NVARCHAR(100) NOT NULL,
    soDienThoai  VARCHAR(10)   NOT NULL UNIQUE,
    diaChi       NVARCHAR(200) NULL,
    email        VARCHAR(100)  NULL,
    diemTichLuy  INT           NOT NULL DEFAULT 0,
    CONSTRAINT CK_KH_sdt  CHECK (LEN(soDienThoai)=10 AND soDienThoai NOT LIKE '%[^0-9]%' AND soDienThoai LIKE '0%'),
    CONSTRAINT CK_KH_diem CHECK (diemTichLuy >= 0)
);
GO

CREATE TABLE Xe (
    bienSo        VARCHAR(12)   NOT NULL PRIMARY KEY,
    maKH          VARCHAR(12)   NOT NULL REFERENCES KhachHang(maKH),
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
GO

-- 3. DANH MỤC DỊCH VỤ, PHỤ TÙNG, NHÀ CUNG CẤP
CREATE TABLE DichVu (
    maDichVu        VARCHAR(10)   NOT NULL PRIMARY KEY,
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
GO

CREATE TABLE PhuTung (
    maPhuTung       VARCHAR(10)   NOT NULL PRIMARY KEY,
    tenPhuTung      NVARCHAR(100) NOT NULL,
    loaiPhuTung     NVARCHAR(50)  NOT NULL,
    donViTinh       NVARCHAR(20)  NOT NULL,
    donGiaNhap      DECIMAL(18,0) NOT NULL,
    donGiaBan       DECIMAL(18,0) NOT NULL,
    soLuongTon      INT           NOT NULL DEFAULT 0,
    tonKhoToiThieu  INT           NOT NULL DEFAULT 0,
    viTriLuuKho     NVARCHAR(50)  NULL,
    thoiGianBaoHanh INT           NOT NULL DEFAULT 0,
    CONSTRAINT CK_PT_gia CHECK (donGiaNhap >= 0 AND donGiaBan >= 0),
    CONSTRAINT CK_PT_ton CHECK (soLuongTon >= 0),
    CONSTRAINT CK_PT_min CHECK (tonKhoToiThieu >= 0),
    CONSTRAINT CK_PT_bh  CHECK (thoiGianBaoHanh >= 0)
);
GO

CREATE TABLE NhaCungCap (
    maNCC        VARCHAR(10)   NOT NULL PRIMARY KEY,
    tenNCC       NVARCHAR(150) NOT NULL,
    soDienThoai  VARCHAR(10)   NULL,
    diaChi       NVARCHAR(200) NULL
);
GO

-- 4. PHIẾU TIẾP NHẬN - SỬA CHỮA THỐNG NHẤT
CREATE TABLE PhieuTiepNhanSuaChua (
    maPhieu              VARCHAR(20)    NOT NULL,
    bienSo               VARCHAR(12)    NOT NULL,
    maNVTiepNhan         VARCHAR(10)    NOT NULL,
    maNVKyThuat          VARCHAR(10)    NULL,
    ngayGioTiepNhan      DATETIME2      NOT NULL CONSTRAINT DF_PTNSC_ngayGioTiepNhan DEFAULT (SYSDATETIME()),
    yeuCauBanDau         NVARCHAR(500)  NOT NULL,
    tinhTrangXe          NVARCHAR(500)  NULL,
    soKmTiepNhan         FLOAT          NOT NULL CONSTRAINT DF_PTNSC_soKm DEFAULT (0),
    mucNhienLieu         FLOAT          NOT NULL CONSTRAINT DF_PTNSC_nhienLieu DEFAULT (0),
    thoiGianHenTra       DATETIME2      NULL,
    thoiGianBatDau       DATETIME2      NULL,
    thoiGianHoanThanh    DATETIME2      NULL,
    ghiChuKyThuat        NVARCHAR(500)  NULL,
    trangThai            VARCHAR(15)    NOT NULL CONSTRAINT DF_PTNSC_trangThai DEFAULT ('CHO_SUA'),

    CONSTRAINT PK_PhieuTiepNhanSuaChua PRIMARY KEY (maPhieu),
    CONSTRAINT CK_PTNSC_TrangThai CHECK (trangThai IN ('CHO_SUA', 'DANG_SUA', 'CHO_LINH_KIEN', 'HOAN_TAT')),
    CONSTRAINT CK_PTNSC_YeuCau CHECK (LEN(LTRIM(RTRIM(yeuCauBanDau))) > 0),
    CONSTRAINT CK_PTNSC_SoKm CHECK (soKmTiepNhan >= 0),
    CONSTRAINT CK_PTNSC_NhienLieu CHECK (mucNhienLieu >= 0),
    CONSTRAINT CK_PTNSC_ThoiGian CHECK (thoiGianHoanThanh IS NULL OR thoiGianBatDau IS NULL OR thoiGianHoanThanh >= thoiGianBatDau),

    CONSTRAINT FK_PTNSC_Xe FOREIGN KEY (bienSo) REFERENCES Xe(bienSo),
    CONSTRAINT FK_PTNSC_NVTiepNhan FOREIGN KEY (maNVTiepNhan) REFERENCES NhanVien(maNV),
    CONSTRAINT FK_PTNSC_NVKyThuat FOREIGN KEY (maNVKyThuat) REFERENCES NhanVien(maNV)
);
GO

CREATE NONCLUSTERED INDEX IX_PTNSC_bienSo ON PhieuTiepNhanSuaChua(bienSo, ngayGioTiepNhan);
CREATE NONCLUSTERED INDEX IX_PTNSC_KTV ON PhieuTiepNhanSuaChua(maNVKyThuat, trangThai);
CREATE NONCLUSTERED INDEX IX_PTNSC_TiepNhan ON PhieuTiepNhanSuaChua(maNVTiepNhan, ngayGioTiepNhan);
CREATE NONCLUSTERED INDEX IX_PTNSC_TrangThai ON PhieuTiepNhanSuaChua(trangThai);
GO

-- 5. CHI TIẾT DỊCH VỤ CỦA PHIẾU
CREATE TABLE ChiTietDichVu (
    maPhieu              VARCHAR(20)    NOT NULL,
    maDichVu             VARCHAR(10)    NOT NULL,
    soLuong              INT            NOT NULL CONSTRAINT DF_ChiTietDichVu_soLuong DEFAULT (1),
    donGiaThucHien       DECIMAL(18,0)  NOT NULL,
    ghiChu               NVARCHAR(300)  NULL,
    thanhTien            AS (CAST(soLuong * donGiaThucHien AS DECIMAL(18,0))) PERSISTED,

    CONSTRAINT PK_ChiTietDichVu PRIMARY KEY (maPhieu, maDichVu),
    CONSTRAINT CK_ChiTietDichVu_SoLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietDichVu_DonGia CHECK (donGiaThucHien >= 0),
    CONSTRAINT FK_ChiTietDichVu_PTNSC FOREIGN KEY (maPhieu) REFERENCES PhieuTiepNhanSuaChua(maPhieu) ON DELETE CASCADE,
    CONSTRAINT FK_ChiTietDichVu_DichVu FOREIGN KEY (maDichVu) REFERENCES DichVu(maDichVu)
);
GO

-- 6. CHI TIẾT LINH KIỆN / PHỤ TÙNG CỦA PHIẾU
CREATE TABLE ChiTietLinhKien (
    maPhieu              VARCHAR(20)    NOT NULL,
    maPhuTung            VARCHAR(10)    NOT NULL,
    soLuong              INT            NOT NULL CONSTRAINT DF_ChiTietLinhKien_soLuong DEFAULT (1),
    donGiaXuat           DECIMAL(18,0)  NOT NULL,
    ghiChu               NVARCHAR(300)  NULL,
    thanhTien            AS (CAST(soLuong * donGiaXuat AS DECIMAL(18,0))) PERSISTED,

    CONSTRAINT PK_ChiTietLinhKien PRIMARY KEY (maPhieu, maPhuTung),
    CONSTRAINT CK_ChiTietLinhKien_SoLuong CHECK (soLuong > 0),
    CONSTRAINT CK_ChiTietLinhKien_DonGia CHECK (donGiaXuat >= 0),
    CONSTRAINT FK_ChiTietLinhKien_PTNSC FOREIGN KEY (maPhieu) REFERENCES PhieuTiepNhanSuaChua(maPhieu) ON DELETE CASCADE,
    CONSTRAINT FK_ChiTietLinhKien_PhuTung FOREIGN KEY (maPhuTung) REFERENCES PhuTung(maPhuTung)
);
GO

-- 7. VIEW TỔNG HỢP CHI PHÍ PHIẾU
CREATE VIEW dbo.vw_PhieuTiepNhanSuaChua_TongTien AS
SELECT 
    p.maPhieu,
    ISNULL(dv.tienDV, 0) AS tongTienDichVu,
    ISNULL(lk.tienLK, 0) AS tongTienLinhKien,
    ISNULL(dv.tienDV, 0) + ISNULL(lk.tienLK, 0) AS tongChiPhi
FROM PhieuTiepNhanSuaChua p
LEFT JOIN (
    SELECT maPhieu, SUM(thanhTien) AS tienDV
    FROM ChiTietDichVu
    GROUP BY maPhieu
) dv ON dv.maPhieu = p.maPhieu
LEFT JOIN (
    SELECT maPhieu, SUM(thanhTien) AS tienLK
    FROM ChiTietLinhKien
    GROUP BY maPhieu
) lk ON lk.maPhieu = p.maPhieu;
GO

-- 8. TRIGGER KIỂM SOÁT LUỒNG TRẠNG THÁI VÀ KHÓA PHIẾU BẤT BIẾN KHI HOÀN TẤT
CREATE TRIGGER trg_PTNSC_KiemTra ON PhieuTiepNhanSuaChua
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- Kiểm tra luồng trạng thái chuyển đổi hợp lệ:
    -- CHO_SUA -> DANG_SUA <-> CHO_LINH_KIEN -> HOAN_TAT
    IF EXISTS (
        SELECT 1 FROM inserted i 
        JOIN deleted d ON d.maPhieu = i.maPhieu
        WHERE i.trangThai <> d.trangThai
          AND NOT (
              (d.trangThai = 'CHO_SUA'       AND i.trangThai IN ('DANG_SUA', 'CHO_LINH_KIEN'))
           OR (d.trangThai = 'DANG_SUA'      AND i.trangThai IN ('CHO_LINH_KIEN', 'HOAN_TAT'))
           OR (d.trangThai = 'CHO_LINH_KIEN' AND i.trangThai = 'DANG_SUA')
          )
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50103, N'Chuyển trạng thái phiếu không hợp lệ (Chờ sửa chữa → Đang sửa chữa ⇄ Chờ linh kiện → Hoàn tất).', 1;
    END;

    -- Khóa phiếu khi đã HOÀN TẤT: Chống gian lận, không cho phép sửa đổi thông tin tiếp nhận
    IF EXISTS (
        SELECT 1 FROM inserted i 
        JOIN deleted d ON d.maPhieu = i.maPhieu
        WHERE d.trangThai = 'HOAN_TAT'
          AND (
              i.bienSo <> d.bienSo 
           OR i.maNVTiepNhan <> d.maNVTiepNhan
           OR ISNULL(i.maNVKyThuat, '') <> ISNULL(d.maNVKyThuat, '')
           OR i.yeuCauBanDau <> d.yeuCauBanDau 
           OR i.ngayGioTiepNhan <> d.ngayGioTiepNhan
           OR i.soKmTiepNhan <> d.soKmTiepNhan
          )
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50104, N'Phiếu đã ở trạng thái HOÀN TẤT - Bị khóa toàn diện, không được chỉnh sửa thông tin.', 1;
    END;
END;
GO

-- 9. TRIGGER KHÓA CHI TIẾT DỊCH VỤ KHI PHIẾU ĐÃ HOÀN TẤT
CREATE TRIGGER trg_CTDV_KiemTra ON ChiTietDichVu
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1 FROM PhieuTiepNhanSuaChua p
        WHERE p.trangThai = 'HOAN_TAT'
          AND p.maPhieu IN (SELECT maPhieu FROM inserted UNION SELECT maPhieu FROM deleted)
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50111, N'Phiếu sửa chữa đã HOÀN TẤT - Không được thêm, sửa hoặc xóa dịch vụ.', 1;
    END;
END;
GO

-- 10. TRIGGER KHÓA LINH KIỆN VÀ ĐỒNG BỘ TỒN KHO TỰ ĐỘNG
CREATE TRIGGER trg_CTLK_KiemTra ON ChiTietLinhKien
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1 FROM PhieuTiepNhanSuaChua p
        WHERE p.trangThai = 'HOAN_TAT'
          AND p.maPhieu IN (SELECT maPhieu FROM inserted UNION SELECT maPhieu FROM deleted)
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50121, N'Phiếu sửa chữa đã HOÀN TẤT - Không được thêm, sửa hoặc xóa linh kiện.', 1;
    END;

    DECLARE @delta TABLE (maPhuTung VARCHAR(10) PRIMARY KEY, delta INT NOT NULL);
    INSERT INTO @delta (maPhuTung, delta)
    SELECT maPhuTung, SUM(delta)
    FROM (
        SELECT maPhuTung, -soLuong AS delta FROM inserted
        UNION ALL
        SELECT maPhuTung,  soLuong AS delta FROM deleted
    ) t
    GROUP BY maPhuTung;

    IF EXISTS (
        SELECT 1 FROM PhuTung pt 
        JOIN @delta d ON pt.maPhuTung = d.maPhuTung
        WHERE pt.soLuongTon + d.delta < 0
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50123, N'Số lượng phụ tùng trong kho không đủ để xuất cho phiếu sửa chữa này.', 1;
    END;

    UPDATE pt
    SET pt.soLuongTon = pt.soLuongTon + d.delta
    FROM PhuTung pt
    JOIN @delta d ON pt.maPhuTung = d.maPhuTung;
END;
GO

-- 11. KHO VÀ XUẤT NHẬP
CREATE TABLE PhieuNhapKho (
    maPhieuNhap VARCHAR(20)   NOT NULL PRIMARY KEY,
    ngayNhap    DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    maNVKho     VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    maNCC       VARCHAR(10)   NOT NULL REFERENCES NhaCungCap(maNCC),
    tongGiaTri  DECIMAL(18,0) NOT NULL DEFAULT 0,
    ghiChu      NVARCHAR(300) NULL,
    CONSTRAINT CK_PNK_gt CHECK (tongGiaTri >= 0)
);
GO

CREATE TABLE ChiTietNhapKho (
    maPhieuNhap VARCHAR(20)   NOT NULL REFERENCES PhieuNhapKho(maPhieuNhap),
    maPhuTung   VARCHAR(10)   NOT NULL REFERENCES PhuTung(maPhuTung),
    soLuongNhap INT           NOT NULL,
    donGiaNhap  DECIMAL(18,0) NOT NULL,
    thanhTien   AS (soLuongNhap * donGiaNhap) PERSISTED,
    PRIMARY KEY (maPhieuNhap, maPhuTung),
    CONSTRAINT CK_CTNK CHECK (soLuongNhap > 0 AND donGiaNhap >= 0)
);
GO

CREATE TABLE PhieuXuatKho (
    maPhieuXK   VARCHAR(20)   NOT NULL PRIMARY KEY,
    maPhieu     VARCHAR(20)   NOT NULL REFERENCES PhieuTiepNhanSuaChua(maPhieu),
    maKTVYeuCau VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    maNVKho     VARCHAR(10)   NULL REFERENCES NhanVien(maNV),
    ngayYeuCau  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    ngayXuat    DATETIME2     NULL,
    lyDoXuat    NVARCHAR(200) NOT NULL,
    lyDoTuChoi  NVARCHAR(200) NULL,
    tongGiaTri  DECIMAL(18,0) NOT NULL DEFAULT 0,
    trangThai   NVARCHAR(20)  NOT NULL DEFAULT N'Chờ xác nhận',
    maPhieuSC   AS maPhieu,
    CONSTRAINT CK_PXK_tt CHECK (trangThai IN (N'Chờ xác nhận', N'Đã xuất kho', N'Đã hủy')),
    CONSTRAINT CK_PXK_gt CHECK (tongGiaTri >= 0)
);
GO

CREATE TABLE ChiTietXuatKho (
    maChiTietXK VARCHAR(12)   NOT NULL PRIMARY KEY,
    maPhieuXK   VARCHAR(20)   NOT NULL REFERENCES PhieuXuatKho(maPhieuXK),
    maPhuTung   VARCHAR(10)   NOT NULL REFERENCES PhuTung(maPhuTung),
    soLuongXuat INT           NOT NULL,
    donGiaXuat  DECIMAL(18,0) NOT NULL,
    thanhTien   AS (soLuongXuat * donGiaXuat) PERSISTED,
    ghiChu      NVARCHAR(200) NULL,
    CONSTRAINT UQ_CTXK UNIQUE (maPhieuXK, maPhuTung),
    CONSTRAINT CK_CTXK CHECK (soLuongXuat > 0 AND donGiaXuat >= 0)
);
GO

-- 11. KHUYẾN MÃI
CREATE TABLE KhuyenMai (
    maKM             VARCHAR(20)   NOT NULL PRIMARY KEY,
    maNV             VARCHAR(10)   NULL REFERENCES NhanVien(maNV),
    tenChuongTrinh   NVARCHAR(150) NOT NULL,
    mucGiamPhanTram  DECIMAL(5,2)  NOT NULL DEFAULT 0,
    ngayBatDau       DATE          NOT NULL,
    ngayKetThuc      DATE          NOT NULL,
    ghiChu           NVARCHAR(255) NULL,
    CONSTRAINT CK_KM_tile CHECK (mucGiamPhanTram >= 0 AND mucGiamPhanTram <= 100),
    CONSTRAINT CK_KM_ngay CHECK (ngayKetThuc >= ngayBatDau)
);
GO

-- 12. HÓA ĐƠN & THANH TOÁN
CREATE TABLE HoaDon (
    maHoaDon          VARCHAR(20)   NOT NULL PRIMARY KEY,
    maPhieu           VARCHAR(20)   NOT NULL UNIQUE REFERENCES PhieuTiepNhanSuaChua(maPhieu),
    maNV              VARCHAR(10)   NOT NULL REFERENCES NhanVien(maNV),
    maKM              VARCHAR(20)   NULL REFERENCES KhuyenMai(maKM),
    ngayLap           DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    tongTienDichVu    DECIMAL(18,0) NOT NULL DEFAULT 0,
    tongTienPhuTung   DECIMAL(18,0) NOT NULL DEFAULT 0,
    chiPhiKhac        DECIMAL(18,0) NOT NULL DEFAULT 0,
    giamGia           DECIMAL(18,0) NOT NULL DEFAULT 0,
    thueVAT           DECIMAL(5,2)  NOT NULL DEFAULT 10,
    tongTienThanhToan DECIMAL(18,0) NOT NULL DEFAULT 0,
    phuongThuc        NVARCHAR(20)  NOT NULL DEFAULT N'Tiền mặt',
    maGiaoDich        VARCHAR(50)   NULL,
    trangThai         NVARCHAR(30)  NOT NULL DEFAULT N'Chưa thanh toán',
    maPhieuSC         AS maPhieu,
    CONSTRAINT CK_HD_tien CHECK (tongTienDichVu>=0 AND tongTienPhuTung>=0 AND chiPhiKhac>=0 AND giamGia>=0 AND tongTienThanhToan>=0),
    CONSTRAINT CK_HD_vat  CHECK (thueVAT BETWEEN 0 AND 100),
    CONSTRAINT CK_HD_giam CHECK (giamGia <= tongTienDichVu + tongTienPhuTung + chiPhiKhac),
    CONSTRAINT CK_HD_pt   CHECK (phuongThuc IN (N'Tiền mặt', N'Chuyển khoản')),
    CONSTRAINT CK_HD_tt   CHECK (trangThai IN (N'Chưa thanh toán', N'Đã thanh toán một phần', N'Đã thanh toán', N'Đã hủy'))
);
GO

CREATE TABLE ChiTietHoaDon (
    maChiTietHD VARCHAR(12)   NOT NULL PRIMARY KEY,
    maHoaDon    VARCHAR(20)   NOT NULL REFERENCES HoaDon(maHoaDon) ON DELETE CASCADE,
    loaiChiTiet VARCHAR(10)   NOT NULL,
    maDichVu    VARCHAR(10)   NULL REFERENCES DichVu(maDichVu),
    maPhuTung   VARCHAR(10)   NULL REFERENCES PhuTung(maPhuTung),
    tenChiTiet  NVARCHAR(200) NOT NULL,
    soLuong     INT           NOT NULL,
    donGia      DECIMAL(18,0) NOT NULL,
    giamGia     DECIMAL(18,0) NOT NULL DEFAULT 0,
    thanhTien   AS (soLuong * donGia - giamGia) PERSISTED,
    CONSTRAINT CK_CTHD_loai CHECK (loaiChiTiet IN ('DichVu','PhuTung')),
    CONSTRAINT CK_CTHD_so   CHECK (soLuong > 0 AND donGia >= 0 AND giamGia >= 0 AND giamGia <= soLuong*donGia),
    CONSTRAINT CK_CTHD_xor  CHECK ((loaiChiTiet='DichVu'  AND maDichVu IS NOT NULL AND maPhuTung IS NULL)
                                OR (loaiChiTiet='PhuTung' AND maPhuTung IS NOT NULL AND maDichVu IS NULL))
);
GO

CREATE TABLE GiaoDichThanhToan (
    maGiaoDich    VARCHAR(20)   NOT NULL PRIMARY KEY,
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

PRINT N'-> Đã hoàn tất tạo cấu trúc bảng GaraTechDB mới thành công!';
GO
