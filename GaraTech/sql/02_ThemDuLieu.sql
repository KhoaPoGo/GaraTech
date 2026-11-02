-- Du lieu mau. Mat khau tat ca tai khoan: Garatech@123
USE GaraTechDB;
GO
INSERT INTO NhanVien(maNV,hoTen,soDienThoai,email,diaChi,vaiTro,heSoLuong,luongCoBan) VALUES
 ('NV26001',N'Nguyễn Văn An','0901000001','an.ql@garatech.vn',N'Q.1, TP.HCM',N'Quản lý',3.00,15000000),
 ('NV26002',N'Trần Thị Bình','0901000002','binh.tn@garatech.vn',N'Q.3, TP.HCM',N'Nhân viên tiếp nhận',2.00,9000000),
 ('NV26003',N'Lê Minh Cường','0901000003','cuong.ktv@garatech.vn',N'Gò Vấp, TP.HCM',N'Kỹ thuật viên',2.50,11000000),
 ('NV26004',N'Phạm Thu Dung','0901000004','dung.kho@garatech.vn',N'Q.7, TP.HCM',N'Nhân viên kho',2.00,9500000);

INSERT INTO TaiKhoan(tenDangNhap,matKhau,maNV) VALUES
 ('quanly',  '120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26001'),
 ('tiepnhan','120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26002'),
 ('kythuat', '120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26003'),
 ('nvkho',   '120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpwwHgF594JkWxfWHliAxXedeT7S5CsLtQlAr1ycs=','NV26004');

INSERT INTO DichVu(maDichVu,tenDichVu,moTa,donGiaCong,thoiGianUocTinh,capDoBaoDuong) VALUES
 ('DV0001',N'Thay dầu động cơ',N'Xả dầu cũ, thay dầu và lọc dầu',150000,0.5,N'Cấp 1'),
 ('DV0002',N'Thay má phanh trước',N'Tháo, thay má phanh, kiểm tra đĩa',250000,1.0,N'Cấp 2'),
 ('DV0003',N'Bảo dưỡng định kỳ 10.000 km',N'Kiểm tra tổng quát',400000,2.0,N'Cấp 3');

INSERT INTO PhuTung(maPhuTung,tenPhuTung,loaiPhuTung,donViTinh,donGiaNhap,donGiaBan,soLuongTon,tonKhoToiThieu,viTriLuuKho,thoiGianBaoHanh) VALUES
 ('PT0001',N'Dầu nhớt 5W-30 (1L)',N'Dầu nhớt',N'Lít',120000,180000,40,10,N'Kệ A1',0),
 ('PT0002',N'Lọc dầu',N'Động cơ',N'Cái',60000,100000,15,5,N'Kệ A2',3),
 ('PT0003',N'Má phanh trước',N'Phanh',N'Bộ',450000,650000,6,5,N'Kệ B1',6),
 ('PT0004',N'Bugi',N'Động cơ',N'Cái',70000,110000,3,8,N'Kệ A3',6);   -- ton thap de test canh bao

INSERT INTO NhaCungCap(maNCC,tenNCC,soDienThoai,diaChi) VALUES ('NCC001',N'Công ty Phụ tùng Hòa Phát','0283000111',N'Q.5, TP.HCM');

INSERT INTO KhachHang(maKH,hoTen,soDienThoai,diaChi) VALUES ('KH-2610-001',N'Võ Văn Em','0912345678',N'Q.10, TP.HCM');
INSERT INTO Xe(bienSo,maKH,hangXe,model,namSanXuat,mauXe,soKmHienTai) VALUES ('51A-123.45','KH-2610-001',N'Toyota',N'Vios',2020,N'Trắng',45000);
GO

UPDATE PhuTung SET donGiaNhap = 120000 WHERE maPhuTung = 'PT0001';
