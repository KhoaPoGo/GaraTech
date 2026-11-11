# Báo cáo phân tích và đối chiếu thiết kế GaraTech

**Ngày rà soát:** 11/11/2026  
**Phạm vi:** Báo cáo Word hiện có, hai script SQL, Java trong `src/`, `README.md` và hình nhúng trong báo cáo Word. Không sửa source và không thực thi SQL.

## 1. Phạm vi tài liệu và giới hạn kết luận

Trong workspace chỉ tìm thấy `docs/Nhom17_REPORT_ApplicationDevelopment_v3.docx`. Không thấy các thư mục/tệp được nêu riêng là `docs/requirements/`, `docs/database/`, `docs/diagrams/`, `docs/ui_design/` hay `docs/NEW_DESIGN.md`. Báo cáo Word có hình nhúng và mô tả màn hình; chúng được dùng làm tài liệu thiết kế hiện có, nhưng không thể khẳng định đó là bộ ảnh thiết kế mới được nhắc đến.

Vì không có ERD/Class Diagram rời và `NEW_DESIGN.md`, phần đối chiếu sơ đồ chỉ dựa trên mô tả/ảnh trong báo cáo Word, DDL thực tế và lớp Java. Những khác biệt dưới đây là giữa các hiện vật có trong workspace; không gán chúng cho tài liệu không truy cập được.

## 2. A. Phân tích nghiệp vụ

### 2.1 Tác nhân và chức năng theo vai trò

| Tác nhân | Nghiệp vụ được báo cáo mô tả | Dấu vết trong ứng dụng hiện tại |
|---|---|---|
| Quản lý | Quản lý tài khoản/phân quyền, dịch vụ, phụ tùng; theo dõi sửa chữa; kiểm tra chất lượng; xem báo cáo/thống kê. | `DASHBOARD`, `DIEU_PHOI`, `KIEM_TRA_CL`, `DICH_VU_NHAN_SU`, `BAO_CAO` có trong `Screen`; panel tương ứng tồn tại. Cần lưu ý việc có panel không tự chứng minh đủ quy tắc/luồng thiết kế. |
| Nhân viên tiếp nhận | Quản lý khách hàng/xe; tiếp nhận, lập phiếu/báo giá; theo dõi; lập hóa đơn, ghi nhận thanh toán và bàn giao. | Có dashboard tiếp nhận, khách hàng/xe, tiếp nhận, hóa đơn. `BAN_GIAO` khai báo nhưng không cấp quyền cho vai trò nào và dùng `PlaceholderPanel`. |
| Kỹ thuật viên | Chỉ xem phiếu được phân công; kiểm tra, cập nhật công việc/kết quả, dịch vụ và phụ tùng; gửi yêu cầu xuất kho. | Có màn công việc KTV, yêu cầu phụ tùng và quyền xem khách hàng/xe, tồn kho. Bộ lọc quyền và quy tắc phân công cần đối chiếu sâu trong service/DAO vì SQL/DAO hiện không đồng nhất ở một số luồng. |
| Nhân viên kho | Quản lý phụ tùng; nhập, duyệt xuất, cập nhật tồn; theo dõi lịch sử nhập/xuất và cảnh báo mức tối thiểu. | Có panel tồn kho, nhập kho, xuất kho. Hai DAO nhập kho trống; vì vậy luồng nhập chưa được triển khai ở tầng dữ liệu dù giao diện có. |

### 2.2 Quy trình nghiệp vụ căn cứ báo cáo Word

1. Tiếp nhận tra cứu/tạo khách hàng và xe, ghi yêu cầu.
2. KTV kiểm tra tình trạng xe, xác định dịch vụ/phụ tùng dự kiến.
3. Tiếp nhận lập phiếu và báo giá; phiếu chờ khách duyệt. Khách đồng ý thì xác nhận và phân công; khách từ chối thì hủy kèm lý do.
4. KTV sửa chữa, gửi yêu cầu phụ tùng. Kho kiểm tra và xác nhận cấp phát; chỉ lúc xác nhận xuất mới trừ tồn. Thiếu hàng thì cảnh báo và tạm dừng hạng mục.
5. KTV cập nhật kết quả; quản lý kiểm tra chất lượng theo mô tả màn hình và xác nhận hoàn thành.
6. Tiếp nhận lập hóa đơn từ chi phí thực tế, ghi nhận một hay nhiều lần thanh toán; chỉ bàn giao khi thanh toán đủ.
7. Lưu lịch sử theo xe để tra cứu.

### 2.3 Điều kiện/ràng buộc nghiệp vụ được nêu

- Mỗi lượt xe cần phiếu gắn xe và thông tin yêu cầu; KTV chỉ làm công việc được ghi nhận.
- Phiếu chỉ sang bước tiếp theo khi dữ liệu bắt buộc của bước hiện tại đầy đủ.
- Tồn kho không được âm; yêu cầu xuất không đồng nghĩa xuất thực tế. Trừ tồn ở thời điểm kho xác nhận.
- Tồn đạt hoặc thấp hơn ngưỡng tối thiểu cần cảnh báo.
- Chỉ lập hóa đơn khi sửa chữa hoàn thành; tổng tiền tính từ số lượng và đơn giá; tiếp nhận ghi nhận thanh toán và bàn giao sau khi thu đủ.
- Vai trò giới hạn chức năng; tài khoản riêng, mật khẩu ít nhất 8 ký tự; nhân viên nghỉ việc cần khóa/ngừng tài khoản.
- Mô tả UI bổ sung các điều kiện thiết kế: KCS tick đầy đủ checklist; bàn giao xác nhận trả chìa khóa/giấy tờ; hóa đơn hỗ trợ lịch sử thanh toán một phần; giữ hồ sơ nhân viên thay vì xóa.

## 3. B. Database

### 3.1 Bảng và khóa/quan hệ trong `sql/01_TaoBang.sql`

| Nhóm | Bảng | Khóa chính | Quan hệ FK chính |
|---|---|---|---|
| Nhân sự | `NhanVien`, `TaiKhoan` | `maNV`; `tenDangNhap` | `TaiKhoan.maNV` → `NhanVien.maNV` (unique, một tài khoản/nhân viên tối đa). |
| Khách/xe | `KhachHang`, `Xe` | `maKH`; `bienSo` | `Xe.maKH` → `KhachHang.maKH`. |
| Danh mục | `DichVu`, `PhuTung`, `NhaCungCap` | `maDichVu`, `maPhuTung`, `maNCC` | Không có quan hệ giữa nhà cung cấp và danh mục phụ tùng; quan hệ nhà cung cấp đi qua phiếu nhập. |
| Tiếp nhận/sửa chữa | `PhieuTiepNhanSuaChua` | `maPhieu` | Gắn `Xe`, nhân viên tiếp nhận và KTV; chi tiết dịch vụ/phụ tùng tham chiếu phiếu và danh mục tương ứng. |
| Nhập/xuất kho | `PhieuNhapKho`, `ChiTietNhapKho`, `PhieuXuatKho`, `ChiTietXuatKho` | Mã phiếu; CT nhập dùng khóa ghép `(maPhieuNhap, maPhuTung)`, CT xuất dùng `maChiTietXK` và unique `(maPhieuXK, maPhuTung)`. | Nhập gắn nhân viên kho/NCC và phụ tùng; xuất gắn phiếu sửa chữa, KTV yêu cầu, nhân viên kho duyệt và phụ tùng. |
| Bán hàng | `KhuyenMai`, `HoaDon`, `ChiTietHoaDon`, `GiaoDichThanhToan` | `maKM`, `maHoaDon`, `maChiTietHD`, `maGiaoDich` | Hóa đơn gắn phiếu (unique), NV, khuyến mãi; chi tiết gắn hóa đơn và tùy loại dịch vụ/phụ tùng; giao dịch gắn hóa đơn và NV. |

DDL có các FK/PK nêu trên, CHECK cho số lượng/giá/trạng thái và cột thành tiền tính toán. View tổng tiền phiếu và trigger kiểm tra chuyển trạng thái, khóa chi tiết sau hoàn tất, trừ tồn cho `ChiTietLinhKien` cũng được định nghĩa. Không có ERD rời nên không xác nhận được cardinality/quan hệ hình vẽ so với DDL.

### 3.2 Mâu thuẫn và thiếu nhất quán dữ liệu

| Vấn đề | Vị trí và nội dung | Tác động |
|---|---|---|
| Hai mô hình trạng thái phiếu không khớp | Báo cáo Word (quy trình và UC) dùng `Chờ khách duyệt`, `Đã xác nhận`, `Đang sửa chữa`, `Chờ kiểm tra`, `Hoàn thành`, `Đã bàn giao`, `Đã hủy`. SQL `PhieuTiepNhanSuaChua.trangThai` chỉ nhận `CHO_SUA`, `DANG_SUA`, `CHO_LINH_KIEN`, `HOAN_TAT`; enum `TrangThaiPhieuSuaChua` trong Java lại chứa bộ trạng thái của báo cáo, còn `TrangThaiPhieu` chứa bộ SQL. | UI/service/DAO có nguy cơ ghi giá trị bị CHECK từ chối hoặc không thể biểu diễn một bước nghiệp vụ; không có duyệt khách, bàn giao, trạng thái chờ KCS trong schema hiện tại. |
| Hai đường ghi nhận phụ tùng cho một sửa chữa | SQL có `ChiTietLinhKien` gắn thẳng phiếu và trigger trừ tồn ngay khi insert; đồng thời có `PhieuXuatKho`/`ChiTietXuatKho` cho yêu cầu và kho duyệt rồi trừ tồn. Không có FK/trigger đồng bộ giữa chi tiết đã duyệt xuất và `ChiTietLinhKien`. | Có thể trừ tồn ở thời điểm không theo yêu cầu, trừ hai lần nếu cả hai đường được dùng, hoặc hóa đơn thiếu phụ tùng vì `HoaDon`/báo cáo chi phí dựa theo đường dữ liệu khác. Cần chốt một luồng dữ liệu chuẩn. |
| Phê duyệt KCS không được lưu đủ | UI/UseCase Word cần checklist, đối chiếu KTV và quản lý, cho phép trả sửa. SQL không có bảng checklist, kết quả KCS, người duyệt hay thời điểm duyệt; trigger cho phép `DANG_SUA → HOAN_TAT`, bỏ qua `CHO_KIEM_TRA`. | Dữ liệu không thể chứng minh việc nghiệm thu; SQL cho phép hoàn tất mà không qua KCS. |
| Báo giá/khách duyệt/hủy chưa được mô hình hóa | Báo cáo cần báo giá, xác nhận khách và lý do hủy phiếu. SQL không có giá/bản báo giá, trạng thái khách duyệt, thời điểm/người xác nhận hay `lyDoHuy` trên phiếu sửa chữa. `PhieuXuatKho.lyDoTuChoi` chỉ là từ chối yêu cầu phụ tùng. | Không lưu được các bước bắt buộc và lịch sử quyết định của khách. |
| Bàn giao không có dữ liệu | Báo cáo yêu cầu xác nhận đã giao, chỉ đủ tiền mới được bàn giao, tick trả chìa khóa/giấy tờ. SQL không có bảng/thuộc tính bàn giao; enum Java có `DA_BAN_GIAO` nhưng đó không phải trạng thái SQL. | Không lưu/kiểm chứng được điều kiện bàn giao hoặc người thực hiện. |
| Lịch sử nhập kho và tồn | Báo cáo cần lịch sử nhập/xuất/người thao tác. DDL có chứng từ nhập/xuất để làm nguồn lịch sử, nhưng `PhieuNhapKhoDao` và `ChiTietNhapKhoDao` hiện rỗng. `NhapKhoPanel` không đủ để xác nhận cập nhật DB. | Nghiệp vụ nhập và lịch sử nhập không có DAO để vận hành qua luồng chuẩn hiện tại. |
| Thanh toán một phần | DDL có `GiaoDichThanhToan`, nhưng `HoaDon.phuongThuc` không chấp nhận `Thẻ` trong khi `GiaoDichThanhToan.phuongThuc` có. DB không có ràng buộc đảm bảo trạng thái hóa đơn/tổng đã thu nhất quán với tổng giao dịch. | Cùng hệ thống chấp nhận phương thức khác nhau ở đầu hóa đơn và dòng thanh toán; trạng thái/thực thu có thể lệch nếu ứng dụng không đồng bộ. |
| Trạng thái/xác nhận xuất kho | DDL biểu diễn từ chối bằng trạng thái `Đã hủy` và cột lý do từ chối. Điều này nhập nhằng giữa hủy bởi KTV và từ chối bởi kho; không có người/thời điểm hủy, lý do hủy riêng. | Khó audit nguyên nhân và tác nhân khi yêu cầu kết thúc. |
| Thông tin khách hàng trên hóa đơn | Hóa đơn gắn phiếu, phiếu gắn xe, xe gắn khách hàng; không có FK khách trực tiếp. Quan hệ gián tiếp vẫn truy vấn được. | Không phải FK bị thiếu nếu chấp nhận đường nối gián tiếp; cần kiểm tra ERD khi có sơ đồ. |

## 4. C. Kiến trúc source code

### 4.1 Hiện trạng theo tầng

- **GUI:** `gui/` có `LoginFrame`, `MainFrame`, `Screen`, sidebar, panel và dialog. Mỗi panel được tạo qua `Screen` và menu lọc theo `Role`; `MainFrame` kiểm tra quyền lúc chuyển màn.
- **BUS/Service:** Có service cho đăng nhập, nhân sự, dịch vụ, điều phối, khách/xe, tiếp nhận/sửa chữa, hóa đơn và báo cáo. Quy tắc nghiệp vụ được đặt chủ yếu ở BUS theo hướng dẫn README.
- **DAO:** Có DAO cho hầu hết bảng, nhưng `PhieuNhapKhoDao` và `ChiTietNhapKhoDao` rỗng. `PhieuXuatKhoDao` và `ChiTietXuatKhoDao` hiện thực luồng yêu cầu/xác nhận xuất.
- **Entity:** Có entity tương ứng phần lớn bảng SQL. `PhieuXuatKho` được ghi chú là thiếu nhiều trường quan hệ trong schema; cần đồng bộ entity/Class Diagram/DAO. Không thấy `PhieuNhapKho` relation trong bản trích xuất entity, nhưng tồn tại entity cơ bản.
- **Quyền:** `Screen` gán role ở cấp màn hình. Màn `BAN_GIAO` có `EnumSet.noneOf(Role.class)`, nên không vai trò nào thấy được; mặc dù báo cáo giao việc bàn giao cho tiếp nhận.

### 4.2 Đối chiếu class diagram và Java

Không có Class Diagram rời để so tên/thuộc tính/phương thức. Dựa vào DDL và Java:

| Đối tượng | Trạng thái triển khai | Việc cần xử lý trong migration |
|---|---|---|
| `PhieuTiepNhanSuaChua` + chi tiết DV/linh kiện | Entity, DAO, BUS và panel có; phục vụ mô hình thống nhất theo SQL. | Chọn bộ trạng thái chuẩn duy nhất, rồi cập nhật entity/enum/DAO/BUS/UI cùng schema. |
| Phiếu nhập và chi tiết nhập | Entity/DAO class có tên nhưng hai DAO rỗng. | Bổ sung thao tác transaction, cập nhật tồn và truy vấn lịch sử theo yêu cầu báo cáo (sau khi thống nhất thiết kế). |
| Phiếu xuất và chi tiết xuất | GUI/DAO có; entity ghi chú thiếu một số thuộc tính của bảng; DAO có phần cũ truy vấn bảng `PhieuSuaChua`/cột `maKTV`, trong khi SQL hiện tại dùng `PhieuTiepNhanSuaChua`/`maNVKyThuat`. | Soát toàn bộ truy vấn DAO xuất, thay phụ thuộc schema cũ và đồng nhất entity/DTO. Tránh chạy các truy vấn cũ với DDL hiện tại. |
| KCS, bàn giao | Panel KCS có; không có entity/DAO/bảng chuyên biệt cho checklist và bàn giao; panel bàn giao chưa có. | Xác định mô hình dữ liệu và use case đúng theo tài liệu mới khi các thiết kế vắng mặt được cung cấp. |
| Báo giá/duyệt khách/hủy phiếu | Có enum `TrangThaiPhieuSuaChua`, nhưng SQL thống nhất không lưu toàn bộ trạng thái/dữ liệu cần thiết. | Đồng bộ state machine và thuộc tính giữa Class Diagram, schema và service. |

### 4.3 Mâu thuẫn source/SQL và tài liệu dự án

- `README.md` còn mô tả SQL/Bus theo bản cũ, ghi demo passwords `12345678`, SHA-256 hex và `TaiKhoanBus`; nhưng source/seed hiện dùng `TaiKhoanService`, PBKDF2 salted và mật khẩu mẫu `Garatech@123`. Đây là sai lệch tài liệu triển khai.
- README nói mỗi bảng một DAO/Bus tương ứng; thực tế không có Bus nhập kho riêng và hai DAO nhập kho rỗng. Một số nghiệp vụ được gộp trong `PhieuTiepNhanSuaChuaBus`/`PhieuXuatKhoDao`.
- `PhieuXuatKhoDao` có đoạn kiểm tra KTV dựa `PhieuSuaChua(maPhieuSC, maKTV)`; DDL hiện không tạo bảng đó. DAO cũng lưu `maPhieuSC` qua cột tính toán từ `maPhieu`, trong khi một câu INSERT đang truyền cột tính toán `maPhieuSC` (SQL Server không cho ghi trực tiếp computed column). Phần này không tương thích DDL và cần rà soát trước vận hành.
- `Session.java` còn TODO stub được phát hiện; cần đọc/đối chiếu thêm để xác nhận các thao tác session thực sự đã hoàn tất trước khi đánh giá luồng xác thực đầy đủ.

## 5. D. Giao diện và phân quyền

### 5.1 Màn hình Word so với `Screen`/panel hiện tại

| Màn hình/nhóm theo báo cáo Word | Hiện trạng source | Đánh giá |
|---|---|---|
| Đăng nhập, màn hình chính | `LoginFrame`, `MainFrame`, `SidebarPanel` có. | Có khung cơ bản; báo cáo Word là căn cứ giao diện, ảnh thiết kế rời không có. |
| Khách hàng/xe và lịch sử | `KhachHangXePanel`, form, danh sách và `LichSuXePanel` có. | Có cấu phần; quyền KTV xem là phù hợp mô tả, cần xác nhận mọi thao tác ghi bị chặn theo role ở cả BUS. |
| Tiếp nhận, kiểm tra, phiếu/báo giá | `TiepNhanXePanel`, modal lập/sửa phiếu có. | Luồng tiếp nhận có nhưng mô hình báo giá/chờ khách duyệt của báo cáo không biểu diễn trong SQL hiện tại. |
| Phân công/tiến độ | `DieuPhoiPanel` có. | Có màn hình quản lý; state flow hiện lệch giữa report, enum và SQL. |
| Công việc KTV/yêu cầu phụ tùng | `CongViecKTVPanel`, `YeuCauPhuTungPanel` có. | Có màn; kiểm tra DAO xuất kho cho thấy phụ thuộc schema cũ. |
| Kiểm tra chất lượng | `KiemTraChatLuongPanel` có thao tác duyệt hoàn thành/trả làm lại. | Cần đối chiếu checklist bắt buộc của report; DDL không lưu checklist và trigger bỏ qua bước chờ kiểm tra. |
| Phụ tùng/tồn kho và lịch sử | `PhuTungTonKhoPanel` có. | Có danh sách; DDL có ngưỡng tối thiểu, nhưng không có bằng chứng xác nhận lịch sử nhập/xuất hiển thị chính xác do DAO nhập rỗng. |
| Nhập kho | `NhapKhoPanel` có. | Chưa hoàn thiện triển khai dữ liệu: DAO header/detail rỗng. |
| Xuất kho | `XuatKhoPanel` có; DAO header/detail chứa luồng xác nhận transaction. | Đúng ý tưởng duyệt mới trừ tồn tại riêng luồng xuất; xung đột với trigger trừ tồn khi ghi `ChiTietLinhKien`. |
| Hóa đơn/thanh toán | `HoaDonPanel`, dialog thanh toán/in hóa đơn có. | Có; cần đối chiếu điều kiện phiếu hoàn tất và thanh toán nhiều lần với schema/state đang chọn. |
| Bàn giao | `Screen.BAN_GIAO` có nhưng factory null, `PlaceholderPanel`; không role nào được phép mở. | Thiếu màn và thiếu phân quyền; chưa có ghi nhận bàn giao. |
| Nhân sự/phân quyền | `DichVuNhanSuPanel` có. Quyền khai báo cố định theo role trong `Screen`. | Có quản lý nhân sự/tài khoản; phần quyền là cấu hình theo vai trò, không phải bảng quyền động. Cần xác nhận có khớp thiết kế Word “bảng phân quyền trực quan” hay chỉ trình bày tĩnh. |
| Báo cáo/thống kê | `BaoCaoPanel` có. | Có panel/service/DAO; hình chi tiết trong bộ tài liệu thiết kế rời không có để kiểm chứng bố cục và KPI. |

## 6. E. Đánh giá tổng thể và bảng migration

| Yêu cầu thiết kế/kinh doanh | Trạng thái hiện tại | Mức đối chiếu | Hướng xử lý cần thiết |
|---|---|---|---|
| Bốn vai trò và menu theo vai trò | Có `Role`, lọc `Screen`, menu chung `MainFrame`. | Phù hợp khung | Soát quyền ở từng hành động/service, không chỉ ẩn menu. |
| Quản lý khách hàng, xe, lịch sử | Có panel/entity/DAO/BUS. | Có cấu phần | Đối chiếu trường và hành vi với Class Diagram/ảnh thiết kế khi nhận được. |
| Phiếu, báo giá và khách duyệt/hủy | UI/service có quy trình phiếu một phần; DDL chỉ có 4 trạng thái khác. | Chưa phù hợp | Chọn state machine thiết kế chuẩn; bổ sung dữ liệu báo giá, duyệt/hủy nếu thiết kế cuối giữ quy trình Word. |
| Điều phối, công việc KTV, tiến độ | Có panel và service; 2 bộ enum trạng thái. | Một phần | Đồng nhất enum/schema/trigger/UI; xác nhận điều kiện phân công. |
| KCS checklist và trả làm lại | Panel có; SQL không lưu checklist, state trigger không bắt buộc qua KCS. | Chưa đầy đủ | Bổ sung mô hình lưu nghiệm thu và quy tắc chuyển trạng thái theo thiết kế đã chốt. |
| Yêu cầu phụ tùng, kho duyệt mới trừ tồn | Có luồng phiếu xuất DAO và transaction; SQL còn trigger trừ tồn trên chi tiết linh kiện. | Mâu thuẫn nghiêm trọng | Chỉ giữ một đường ghi nhận sử dụng/tồn kho và nối xuất đã duyệt với chi phí phiếu/hóa đơn. |
| Nhập kho, tồn trước/sau, lịch sử | Panel có; DAO nhập rỗng. | Chưa triển khai dữ liệu | Hoàn thiện tầng DAO/service và ghi nhận tồn/lịch sử khi thiết kế chốt. |
| Cảnh báo tồn tối thiểu | Cột mức tối thiểu có trong DDL; panel tồn kho có. | Có nền tảng | Xác minh điều kiện hiển thị cảnh báo và ngữ nghĩa “<= ngưỡng”. |
| Hóa đơn chỉ sau hoàn tất, thanh toán nhiều lần | Panel/dialog/bảng giao dịch có; state phiếu lệch; phương thức hóa đơn và giao dịch lệch. | Một phần/mâu thuẫn | Đồng bộ state và phương thức; quy tắc tổng đã thu, còn nợ, trạng thái thanh toán. |
| Chỉ bàn giao khi đã thu đủ; checklist giao xe | Chưa có màn hoạt động hoặc dữ liệu bàn giao; role không được cấp. | Thiếu | Tạo luồng bàn giao và ghi nhận xác nhận theo thiết kế chốt. |
| Quản lý dịch vụ, nhân sự, tài khoản | Có panel/service/DAO và trạng thái nhân viên/tài khoản. | Có phần lớn cấu phần | Làm rõ phân quyền cố định theo role so với yêu cầu hiển thị bảng phân quyền. |
| Báo cáo thực thu/còn chờ, cơ cấu dịch vụ/phụ tùng | Panel/service/DAO tồn tại; DB có invoice/payment/detail. | Cần xác minh | Đối chiếu truy vấn và ảnh thiết kế khi tài liệu UI mới có mặt. |
| ERD/Class Diagram/NEW_DESIGN | Không có các tệp chỉ định trong workspace. | Không thể kết luận | Bổ sung đúng tài liệu phiên bản mới để hoàn tất đối chiếu sơ đồ, trường lớp và hình ảnh. |

### Ưu tiên cần giải quyết trước khi migration code

1. Khôi phục đủ tài liệu thiết kế mới (đặc biệt `NEW_DESIGN.md`, ERD/Class Diagram và ảnh giao diện), rồi xác định bản nào là chuẩn nếu khác báo cáo Word.
2. Chốt một mô hình phiếu duy nhất: trạng thái, báo giá/khách duyệt, KCS, hủy, hoàn tất và bàn giao.
3. Chốt một luồng phụ tùng duy nhất giữa yêu cầu xuất, chi tiết phiếu sửa chữa, tồn kho và hóa đơn; loại bỏ khả năng trừ tồn hai lần.
4. Đối chiếu và sửa thiết kế DAO/entity với DDL hiện hành, ưu tiên `PhieuXuatKhoDao` và luồng nhập kho.
5. Hoàn thiện dữ liệu/role/màn bàn giao; bảo đảm hóa đơn chỉ sau hoàn tất và bàn giao chỉ sau thu đủ.
6. Cập nhật README cho đúng seed, mật khẩu, mã hóa và cấu trúc source hiện hành.

**Kết luận:** Source có nền GUI và nhiều màn nghiệp vụ cho cả bốn vai trò, nhưng chưa thể xem là đồng nhất với quy trình báo cáo. Các điểm chặn lớn nhất là hai state machine song song, hai đường trừ tồn phụ tùng, DAO nhập kho chưa có xử lý, DAO xuất kho còn tham chiếu schema cũ, và chức năng bàn giao chưa hoạt động. Việc xác nhận đầy đủ thiết kế mới còn phụ thuộc các tài liệu rời hiện không có trong workspace.
