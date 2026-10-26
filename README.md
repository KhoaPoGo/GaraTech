# GaraTech - Hệ Thống Quản Lý Garage Oto

## Yêu cầu

- **Ngôn ngữ:** Java
- **Môi trường:** **JDK 17**
- **Giao diện:** Java Swing
- **CSDL:** Microsoft SQL Server (JDBC)

## Cấu trúc cây thư mục (Kiến trúc 3 lớp)

```
└── 📁GaraTech
    └── 📁sql
        ├── GaraTech.sql                        # DDL: Tạo bảng + PK/FK
    └── 📁docs
    └── 📁resources
        └── 📁icons                                     # Icon nút bấm, logo
        └── 📁images                                    # Ảnh
    └── 📁src
        └── 📁bus                               # --- TẦNG 2: BUSINESS LOGIC / SERVICE ---
        └── 📁connectDB
            ├── ConnectDB.java                  # Kết nối với dbdb
        └── 📁dao                               # --- TẦNG 3: DATA ACCESS OBJECT (JDBC) ---
            ├── ChiTietHoaDonDao.java
            ├── ChiTietNhapKhoDao.java
            ├── ChiTietXuatKhoDao.java
            ├── DichVuDao.java
            ├── GiaoDichThanhToanDao.java
            ├── HangMucSuaChuaDao.java
            ├── HoaDonDao.java
            ├── KhachHangDao.java
            ├── NhaCungCapDao.java
            ├── NhanVienDao.java
            ├── PhieuNhapKhoDao.java
            ├── PhieuSuaChuaDao.java
            ├── PhieuTiepNhanDao.java  
            ├── PhieuXuatKhoDao.java  
            ├── PhuTungDao.java
            ├── TaiKhoanDao.java
            ├── XeDao.java
        └── 📁entity
            ├── ChiTietHoaDon.java
            ├── ChiTietNhapKho.java
            ├── ChiTietXuatKho.java
            ├── DichVu.java
            ├── GiaoDichThanhToan.java
            ├── HangMucSuaChua.java
            ├── HoaDon.java
            ├── KhachHang.java
            ├── NhaCungCap.java
            ├── NhanVien.java
            ├── PhieuNhapKho.java
            ├── PhieuSuaChua.java
            ├── PhieuTiepNhan.java
            ├── PhieuXuatKho.java
            ├── PhuTung.java
            ├── TaiKhoan.java
            ├── Xe.java
        └── 📁gui                               # --- TẦNG 1: PRESENTATION ---
            └── 📁component                     
            └── 📁dialog                        #Mấy cửa số pop-up hiện lên để thêm
            └── 📁panel
                ├── DashBoardLabel.java
                ├── DieuPhoiLabel.java
                ├── QuanLyDichVuLabel.java
                ├── QuanLyNhanSuLabel.java
                ├── ThongKeLabel.java
                ├── TiepNhanLabel.java
            ├── LoginFrame.java
            ├── KyThuatVienFrame.java
            ├── NhanVienKhoFrame.java
            ├── NhanVienQuanLyFrame.java
            ├── KyThuatTiepNhanFrame.java
        └── 📁main
            ├── Main.java
        └── 📁util
            ├── RoundedPannel.java              #Bo trmàuòn các pannel, truyền vào là màu và độ bo tròn
    ├── .classpath
    ├── .gitignore
    ├── .project
    ├── README.md
```
