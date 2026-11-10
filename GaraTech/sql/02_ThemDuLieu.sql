-- =====================================================================
-- GaraTech - Dữ liệu mẫu (Seed Data)
-- Mật khẩu tất cả tài khoản: Garatech@123
-- Áp dụng cho mô hình Phiếu Tiếp nhận - Sửa chữa thống nhất
-- =====================================================================

USE GaraTechDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

PRINT N'-> Đang nạp dữ liệu mẫu vào GaraTechDB...';
GO

-- 1. NHÂN VIÊN
INSERT INTO NhanVien(maNV,hoTen,soDienThoai,email,diaChi,vaiTro,heSoLuong,luongCoBan) VALUES
 ('NV26001',N'Nguyễn Văn An','0901000001','an.ql@garatech.vn',N'Q.1, TP.HCM',N'Quản lý',3.00,15000000),
 ('NV26002',N'Trần Thị Bình','0901000002','binh.tn@garatech.vn',N'Q.3, TP.HCM',N'Nhân viên tiếp nhận',2.00,9000000),
 ('NV26003',N'Lê Minh Cường','0901000003','cuong.ktv@garatech.vn',N'Gò Vấp, TP.HCM',N'Kỹ thuật viên',2.50,11000000),
 ('NV26004',N'Phạm Thu Dung','0901000004','dung.kho@garatech.vn',N'Q.7, TP.HCM',N'Nhân viên kho',2.00,9500000);
GO

-- 2. TÀI KHOẢN
INSERT INTO TaiKhoan(tenDangNhap,matKhau,maNV) VALUES
 ('quanly',  '120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26001'),
 ('tiepnhan','120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26002'),
 ('kythuat', '120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26003'),
 ('nvkho',   '120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26004');
GO

-- 3. DANH MỤC DỊCH VỤ
INSERT INTO DichVu(maDichVu,tenDichVu,moTa,donGiaCong,thoiGianUocTinh,capDoBaoDuong) VALUES
 ('DV0001',N'Thay dầu động cơ',N'Xả dầu cũ, thay dầu và lọc dầu',150000,0.5,N'Cấp 1'),
 ('DV0002',N'Thay má phanh trước',N'Tháo, thay má phanh, kiểm tra đĩa',250000,1.0,N'Cấp 2'),
 ('DV0003',N'Bảo dưỡng định kỳ 10.000 km',N'Kiểm tra toàn diện 24 hạng mục',400000,2.0,N'Cấp 3'),
 ('DV0004',N'Vệ sinh kim phun & họng nạp',N'Xúc rửa kim phun bằng dung dịch chuyên dụng',200000,1.0,N'Cấp 2'),
 ('DV0005',N'Cân bằng động 4 bánh xe',N'Kiểm tra và cân chì bánh xe',300000,1.5,N'Cấp 2');
GO

-- 4. PHỤ TÙNG
INSERT INTO PhuTung(maPhuTung,tenPhuTung,loaiPhuTung,donViTinh,donGiaNhap,donGiaBan,soLuongTon,tonKhoToiThieu,viTriLuuKho,thoiGianBaoHanh) VALUES
 ('PT0001',N'Dầu nhớt 5W-30 (1L)',N'Dầu nhớt',N'Lít',120000,180000,40,10,N'Kệ A1',0),
 ('PT0002',N'Lọc dầu động cơ',N'Động cơ',N'Cái',60000,100000,15,5,N'Kệ A2',3),
 ('PT0003',N'Má phanh trước chính hãng',N'Phanh',N'Bộ',450000,650000,10,5,N'Kệ B1',6),
 ('PT0004',N'Bugi Iridium',N'Động cơ',N'Cái',70000,110000,20,8,N'Kệ A3',6),
 ('PT0005',N'Lọc gió điều hòa',N'Điều hòa',N'Cái',90000,150000,8,5,N'Kệ B2',3);
GO

-- 5. NHÀ CUNG CẤP
INSERT INTO NhaCungCap(maNCC,tenNCC,soDienThoai,diaChi) VALUES 
 ('NCC001',N'Công ty Phụ tùng Hòa Phát','0283000111',N'Q.5, TP.HCM'),
 ('NCC002',N'Nhà phân phối Castrol Việt Nam','0283999222',N'Bình Thạnh, TP.HCM');
GO

-- 6. KHÁCH HÀNG & PHƯƠNG TIỆN
INSERT INTO KhachHang(maKH,hoTen,soDienThoai,diaChi) VALUES 
 ('KH-2610-001',N'Võ Văn Em','0912345678',N'Q.10, TP.HCM'),
 ('KH-2610-002',N'Nguyễn Thị Mai','0908765432',N'Q.Phú Nhuận, TP.HCM'),
 ('KH-2610-003',N'Trần Quốc Bảo','0987112233',N'TP.Thủ Đức, TP.HCM');
GO

INSERT INTO Xe(bienSo,maKH,hangXe,model,namSanXuat,mauXe,soKmHienTai) VALUES 
 ('51A-123.45','KH-2610-001',N'Toyota',N'Vios',2020,N'Trắng',45000),
 ('59C-678.90','KH-2610-002',N'Honda',N'City',2021,N'Đen',28000),
 ('51H-999.88','KH-2610-003',N'Mazda',N'CX-5',2022,N'Đỏ',62000);
GO

-- 7. DỮ LIỆU MẪU: PHIẾU TIẾP NHẬN - SỬA CHỮA (ĐỦ 4 TRẠNG THÁI)
-- Tạm vô hiệu hóa trigger kiểm tra trạng thái khóa khi nạp dữ liệu mẫu hoàn tất ban đầu
ALTER TABLE ChiTietDichVu DISABLE TRIGGER trg_CTDV_KiemTra;
ALTER TABLE ChiTietLinhKien DISABLE TRIGGER trg_CTLK_KiemTra;
GO

-- Phiếu 1: HOAN_TAT (Đã sửa xong và nghiệm thu ngày 08/11/2026)
INSERT INTO PhieuTiepNhanSuaChua (
    maPhieu, bienSo, maNVTiepNhan, maNVKyThuat, ngayGioTiepNhan,
    yeuCauBanDau, tinhTrangXe, soKmTiepNhan, mucNhienLieu,
    thoiGianHenTra, thoiGianBatDau, thoiGianHoanThanh, ghiChuKyThuat, trangThai
) VALUES (
    'PSC-20261108-001', '51A-123.45', 'NV26002', 'NV26003', '2026-11-08 08:30:00',
    N'Xe đến kỳ bảo dưỡng, thay dầu nhớt và kiểm tra má phanh trước.',
    N'Xe trầy nhẹ cản sau bên phụ; máy êm, phanh có tiếng kêu nhẹ.',
    44800, 0.50,
    '2026-11-08 11:30:00', '2026-11-08 09:00:00', '2026-11-08 11:00:00',
    N'Đã thay 3L dầu nhớt 5W-30 và 1 lọc dầu; đã thay má phanh trước. Chạy thử xe đạt chuẩn.',
    'HOAN_TAT'
);

INSERT INTO ChiTietDichVu(maPhieu, maDichVu, soLuong, donGiaThucHien, ghiChu) VALUES
 ('PSC-20261108-001', 'DV0001', 1, 150000, N'Thay dầu máy'),
 ('PSC-20261108-001', 'DV0002', 1, 250000, N'Công thay má phanh trước');

INSERT INTO ChiTietLinhKien(maPhieu, maPhuTung, soLuong, donGiaXuat, ghiChu) VALUES
 ('PSC-20261108-001', 'PT0001', 3, 180000, N'Dầu nhớt 5W-30'),
 ('PSC-20261108-001', 'PT0002', 1, 100000, N'Lọc dầu động cơ');

-- Phiếu 2: DANG_SUA (Đang thực hiện sửa chữa hôm nay)
INSERT INTO PhieuTiepNhanSuaChua (
    maPhieu, bienSo, maNVTiepNhan, maNVKyThuat, ngayGioTiepNhan,
    yeuCauBanDau, tinhTrangXe, soKmTiepNhan, mucNhienLieu,
    thoiGianHenTra, thoiGianBatDau, thoiGianHoanThanh, ghiChuKyThuat, trangThai
) VALUES (
    'PSC-20261110-001', '59C-678.90', 'NV26002', 'NV26003', '2026-11-10 09:00:00',
    N'Bảo dưỡng định kỳ 10.000 km, xe tăng tốc hơi rung.',
    N'Hiện trạng nguyên vẹn, nội thất sạch sẽ.',
    28000, 0.75,
    '2026-11-10 16:00:00', '2026-11-10 09:30:00', NULL,
    N'Đang tiến hành tháo kiểm tra cụm bugi và vệ sinh bướm ga.',
    'DANG_SUA'
);

INSERT INTO ChiTietDichVu(maPhieu, maDichVu, soLuong, donGiaThucHien, ghiChu) VALUES
 ('PSC-20261110-001', 'DV0003', 1, 400000, N'Bảo dưỡng định kỳ 10.000 km');

-- Phiếu 3: CHO_LINH_KIEN (Tạm dừng chờ nhập thêm linh kiện)
INSERT INTO PhieuTiepNhanSuaChua (
    maPhieu, bienSo, maNVTiepNhan, maNVKyThuat, ngayGioTiepNhan,
    yeuCauBanDau, tinhTrangXe, soKmTiepNhan, mucNhienLieu,
    thoiGianHenTra, thoiGianBatDau, thoiGianHoanThanh, ghiChuKyThuat, trangThai
) VALUES (
    'PSC-20261110-002', '51H-999.88', 'NV26002', 'NV26003', '2026-11-10 10:15:00',
    N'Đèn Check Engine sáng, hao xăng, máy giật khi tăng ga.',
    N'Thân vỏ đẹp; lốp mòn đều.',
    62000, 0.25,
    '2026-11-11 15:00:00', '2026-11-10 10:45:00', NULL,
    N'Cần thay 4 Bugi Iridium chuyên dụng cho CX-5. Đang chờ xuất kho bổ sung.',
    'CHO_LINH_KIEN'
);

INSERT INTO ChiTietDichVu(maPhieu, maDichVu, soLuong, donGiaThucHien, ghiChu) VALUES
 ('PSC-20261110-002', 'DV0004', 1, 200000, N'Vệ sinh kim phun');

-- Phiếu 4: CHO_SUA (Vừa tiếp nhận, chờ KTV chính nhận xe)
INSERT INTO PhieuTiepNhanSuaChua (
    maPhieu, bienSo, maNVTiepNhan, maNVKyThuat, ngayGioTiepNhan,
    yeuCauBanDau, tinhTrangXe, soKmTiepNhan, mucNhienLieu,
    thoiGianHenTra, thoiGianBatDau, thoiGianHoanThanh, ghiChuKyThuat, trangThai
) VALUES (
    'PSC-20261110-003', '51A-123.45', 'NV26002', 'NV26003', '2026-11-10 14:00:00',
    N'Vệ sinh kim phun và cân bằng động bánh xe sau chuyến đi dài.',
    N'Gầm xe dính bùn đất; kính chắn gió có vết ố nhỏ.',
    45000, 0.50,
    '2026-11-10 17:30:00', NULL, NULL,
    NULL,
    'CHO_SUA'
);

INSERT INTO ChiTietDichVu(maPhieu, maDichVu, soLuong, donGiaThucHien, ghiChu) VALUES
 ('PSC-20261110-003', 'DV0004', 1, 200000, N'Vệ sinh kim phun'),
 ('PSC-20261110-003', 'DV0005', 1, 300000, N'Cân bằng động 4 bánh');
GO

-- 8. KHUYẾN MÃI, HÓA ĐƠN & THANH TOÁN
INSERT INTO KhuyenMai (maKM, maNV, tenChuongTrinh, mucGiamPhanTram, ngayBatDau, ngayKetThuc, ghiChu) VALUES
 ('KM001', 'NV26002', N'Ưu đãi bảo dưỡng định kỳ', 10.0, '2026-10-01', '2026-12-31', N'Giảm 10% toàn bộ dịch vụ và linh kiện'),
 ('KM002', 'NV26002', N'Khuyến mãi thay nhớt & lọc dầu', 15.0, '2026-10-15', '2026-11-30', N'Giảm 15% cho phụ tùng nhớt và lọc'),
 ('KM003', 'NV26002', N'Tri ân khách hàng thân thiết', 5.0, '2026-09-01', '2026-12-31', N'Giảm 5% trên tổng hóa đơn thanh toán');
GO

INSERT INTO HoaDon (
    maHoaDon, maPhieu, maNV, maKM, ngayLap,
    tongTienDichVu, tongTienPhuTung, chiPhiKhac, giamGia, thueVAT,
    tongTienThanhToan, phuongThuc, maGiaoDich, trangThai
) VALUES (
    'HD20261108-0001', 'PSC-20261108-001', 'NV26002', NULL, '2026-11-08 11:15:00',
    400000, 640000, 0, 0, 10.00,
    1144000, N'Chuyển khoản', 'VCB98234182', N'Đã thanh toán'
);

INSERT INTO ChiTietHoaDon(maChiTietHD, maHoaDon, loaiChiTiet, maDichVu, maPhuTung, tenChiTiet, soLuong, donGia, giamGia) VALUES
 ('CTHD00001', 'HD20261108-0001', 'DichVu', 'DV0001', NULL, N'Thay dầu động cơ', 1, 150000, 0),
 ('CTHD00002', 'HD20261108-0001', 'DichVu', 'DV0002', NULL, N'Thay má phanh trước', 1, 250000, 0),
 ('CTHD00003', 'HD20261108-0001', 'PhuTung', NULL, 'PT0001', N'Dầu nhớt 5W-30 (1L)', 3, 180000, 0),
 ('CTHD00004', 'HD20261108-0001', 'PhuTung', NULL, 'PT0002', N'Lọc dầu động cơ', 1, 100000, 0);

INSERT INTO GiaoDichThanhToan(maGiaoDich, maHoaDon, maNV, ngayThanhToan, soTien, phuongThuc, trangThai) VALUES
 ('GD-20261108-001', 'HD20261108-0001', 'NV26002', '2026-11-08 11:20:00', 1144000, N'Chuyển khoản', N'Đã thanh toán');
GO

-- Kích hoạt lại các trigger kiểm tra
ALTER TABLE ChiTietDichVu ENABLE TRIGGER trg_CTDV_KiemTra;
ALTER TABLE ChiTietLinhKien ENABLE TRIGGER trg_CTLK_KiemTra;
GO

PRINT N'-> Đã nạp thành công toàn bộ dữ liệu mẫu vào GaraTechDB!';
GO
