# GaraTech – Quản lý gara ô tô (Java Swing + JDBC + SQL Server)

## 1. Mở project trong Eclipse (project Java thường, không dùng Maven)
1. Đặt thư mục `GaraTech` ở đường dẫn **không dấu, không khoảng trắng** (ví dụ `D:\dev\GaraTech`).
2. Bỏ 3 file `.jar` vào thư mục `lib/` (xem `lib/README.txt`).
3. Eclipse: **File → Import → General → Existing Projects into Workspace** → chọn thư mục `GaraTech` → Finish.
4. Nếu báo lỗi JRE: chuột phải project → **Properties → Java Build Path → Libraries** → chọn lại JRE là JDK 17.
5. Chạy: chuột phải `src/main/Main.java` → **Run As → Java Application**.

**CSDL:** copy `db.local.properties.example` → `db.local.properties` (không commit) rồi sửa user/password SQL Server.
**Chưa có CSDL:** với `app.demoMode=true` đăng nhập bằng `quanly` / `tiepnhan` / `kythuat` / `kho`, mật khẩu `12345678`.

## 2. Cấu trúc
```
GaraTech
├─ sql/GaraTech.sql          DDL + dữ liệu mẫu (SV4)
├─ docs/
├─ resources/                là Source Folder: db.properties, images/logo.png, icons/
├─ lib/                      flatlaf, flatlaf-fonts-inter, mssql-jdbc (.jar)
└─ src/
   ├─ main/Main.java         điểm khởi động
   ├─ entity/                class thực thể + enum (Role, User, TrangThai*...)
   ├─ connectDB/ConnectDB    kết nối JDBC, inTransaction(...)
   ├─ dao/                   truy vấn JDBC, mỗi bảng 1 Dao  (TaiKhoanDao có sẵn)
   ├─ bus/                   nghiệp vụ / service             (TaiKhoanBus có sẵn)
   ├─ gui/                   LoginFrame, MainFrame, Screen, Navigator
   │  ├─ component/          Theme, BasePanel, UI, StatCard, SidebarPanel...
   │  ├─ dialog/             các cửa sổ popup (thêm/sửa)
   │  └─ panel/              mỗi màn hình 1 panel (DashBoardPanel là bản mẫu)
   └─ util/                  RoundedPanel, Session, Validator, IdGenerator, Fmt...
```
Luồng gọi: `gui → bus → dao → ConnectDB`. Quy tắc nghiệp vụ (chặn xuất vượt tồn, chỉ lập hóa đơn khi phiếu Hoàn thành, chuyển trạng thái...) viết ở **bus**, không viết trong sự kiện nút bấm.

## 3. Thêm một màn hình (3 bước)
1. Tạo `gui/panel/TenManHinhPanel.java` kế thừa `BasePanel` (xem `DashBoardPanel`): nội dung đặt vào `body`, nút góc phải vào `actions`, nạp dữ liệu trong `onShow()`.
2. Trong `gui/Screen.java`, thay `null` ở dòng của màn hình đó bằng `TenManHinhPanel::new`.
3. Xong: màn hình tự hiện trong sidebar đúng vai trò. Chuyển trang từ code: `Navigator.go(Screen.HOA_DON)`.
Mỗi vai trò dùng chung một `MainFrame`; menu tự đổi theo `Session.role()` nên **không cần** tạo riêng 4 Frame cho từng vai trò.

## 4. Quy ước nhóm
| Người | Sở hữu |
|---|---|
| SV1 | `gui/component/*`, `gui/MainFrame`, `Theme`, `ConnectDB`, `Session` + màn Quản lý (Dashboard, Điều phối, Kiểm tra CL, Dịch vụ & nhân sự, Báo cáo) |
| SV2 | Khách hàng & xe, Tiếp nhận xe, Lập phiếu sửa chữa |
| SV3 | Phụ tùng & tồn kho, Nhập kho, Xuất kho |
| SV4 | `sql/GaraTech.sql`, Công việc KTV, Yêu cầu phụ tùng, Hóa đơn, Bàn giao |

- Mỗi bảng trong CSDL: 1 class ở `entity`, 1 Dao ở `dao`, và Bus tương ứng ở `bus` do người sở hữu màn hình đó viết.
- Mỗi người 1 nhánh Git, commit nhỏ, `git pull --rebase` trước khi push, merge vào `develop` giữa ngày và cuối ngày.
- Ghi nhiều bước vào DB (xuất/nhập kho, lập hóa đơn) dùng `ConnectDB.inTransaction(c -> { ... })`.
- Thao tác quan trọng hỏi xác nhận bằng `Msg.confirm(...)`.
- Không đổi tên package/class dùng chung khi chưa báo SV1 (dễ gây xung đột khi merge).

## 5. Cần thống nhất với file SQL (SV4)
- `TaiKhoanDao` giả định bảng `TAI_KHOAN(tenDangNhap, matKhau, maNV)` và `NHAN_VIEN(maNV, hoTen, vaiTro)`. Nếu DDL đặt tên khác, **chỉ sửa `TaiKhoanDao`**.
- Mật khẩu lưu SHA-256 hex IN HOA: `CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', N'12345678'), 2)`.
- `vaiTro` chỉ nhận: `Quản Lý`, `Tiếp Nhận`, `Kỹ Thuật Viên`, `Kho` (khớp enum `entity.Role`).
- Chuỗi trạng thái trong DB phải khớp `label` của các enum `TrangThai*` (ví dụ `Đang sửa chữa`).
