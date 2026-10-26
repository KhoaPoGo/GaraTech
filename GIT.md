
## 1. Quy tắc bắt buộc

1. **Không commit hoặc push trực tiếp vào `main` và `develop`.**
2. Mỗi chức năng được làm trên một nhánh riêng có dạng `feature/<ten>/<chuc-nang>` và được tạo từ `develop` mới nhất.
3. Hoàn thành chức năng thì push nhánh lên GitHub và tạo **Pull Request (PR) vào `develop`**.
4. Anh Ba Đen Đẹp Trai số 1 num bờ oăn sẽ review code, yêu cầu chỉnh sửa nếu cần, kiểm tra chương trình chạy được rồi mới merge.
5. Commit message phải nói rõ thay đổi, không dùng các nội dung chung chung như `update`, `123`, `fix bug` hoặc `blablabloble`.
6. Không tự ý sửa module hoặc file do thành viên khác phụ trách. Nếu cần sửa file dùng chung, hãy báo trong nhóm trước.

## 2. Cấu trúc nhánh

```text
main
└── develop                           # code đã được review để phát triển chung
    ├── feature/dat/login             # Dat: đăng nhập
    ├── feature/dat/employee          # Dat: nhân viên/phân quyền
    ├── feature/dat/statistics        # Dat: thống kê
    ├── feature/tv2/...........       # TV2: ..........
    ├── feature/tv2/............      # TV2: ..........
    ├── feature/tv3/....       .      # TV3: ...........

```

## 3. Quy trình

### Bước 1: Lấy dự án lần đầu
```bash
git clone https://github.com/KhoaPoGo/GaraTech.git
git fetch origin
git switch develop
```

Nếu đã clone dự án rồi thì bỏ qua bước này

### Bước 2: Cập nhật `develop` và tạo nhánh chức năng

Luôn chạy các lệnh sau trước khi bắt đầu một chức năng mới:

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/tv2/appointment
```

Thay `feature/tv2/appointment` bằng đúng nhánh được phân công. Mỗi người chỉ chạy **một** lệnh phù hợp, ví dụ:

```bash
# Khoa làm hóa đơn
git switch -c feature/Khoa/invoice-payment
```

Mỗi lần chỉ tạo và làm việc trên **một** nhánh phù hợp với chức năng đang thực hiện

### Bước 3: Kiểm tra và commit thay đổi

Sau khi code xong một phần chạy được (ví dụ):

```bash
git status
git add src/entity/HoaDon.java
git add src/dao/HoaDonDAO.java
git commit -m "feat: add invoice management"
```

Nên `git add` đúng các file thuộc chức năng thay vì add toàn bộ dự án

Các tiền tố commit thường dùng:

| Tiền tố | Dùng khi | Ví dụ |
|---|---|---|
| `feat` | Thêm chức năng mới | `feat: add appointment management` |
| `fix` | Sửa lỗi | `fix: prevent duplicate spa appointments` |
| `refactor` | Sắp xếp lại code nhưng không đổi chức năng | `refactor: simplify invoice calculation` |
| `style` | Chỉnh giao diện hoặc định dạng code | `style: update sales panel layout` |
| `docs` | Sửa tài liệu | `docs: add Git workflow guide` |
| `chore` | Cấu hình, thư viện hoặc công việc phụ | `chore: configure SQL Server driver` |

### Bước 4: Cập nhật code mới của nhóm trước khi push

```bash
git fetch origin
git merge origin/develop
```

Nếu Git báo conflict:

1. Mở các file có conflict và trao đổi với người phụ trách file nếu cần.
2. Giữ lại nội dung đúng, xóa các dấu `<<<<<<<`, `=======`, `>>>>>>>`.
3. Kiểm tra chương trình rồi hoàn tất merge:

```bash
git add src/duong-dan/file-da-sua.java
git commit
```

Nếu chưa biết xử lý conflict, không xóa file hoặc reset code. Hãy gửi ảnh lỗi và tên file lên nhóm để cùng xử lý.

### Bước 5: Đẩy nhánh lên GitHub

Lần push đầu tiên của nhánh:

```bash
git push -u origin feature/tv2/appointment
```

Những lần sau trên cùng nhánh chỉ cần:

```bash
git push
```

Nhớ thay tên nhánh trong ví dụ bằng nhánh đang làm. Có thể kiểm tra nhánh hiện tại bằng:

```bash
git branch --show-current
```

### Bước 6: Tạo Pull Request

1. Mở repository `GaraTech` trên GitHub.
2. Chọn **Compare & pull request**.
3. Chọn `base: develop` và `compare: feature/<ten>/<chuc-nang>`
4. Tiêu đề ghi rõ chức năng, ví dụ: `feat: add appointment management`
5. Trong mô tả, ghi phần đã làm, cách kiểm tra và phần còn thiếu nếu có
6. Gửi PR và chờ review, không tự merge PR của mình

Ví dụ mô tả:

```text
## Đã làm
- Thêm entity và DAO cho lịch hẹn
- Thêm kiểm tra trùng lịch

## Cách kiểm tra
- Tạo lịch hẹn mới
- Thử tạo hai lịch trùng thời gian

## Còn thiếu
- Chưa làm chức năng gửi thông báo
```
### Bước 7: Sau khi được merge

```bash
git switch develop
git pull --ff-only origin develop
git branch -d feature/tv2/appointment
```

Sau đó tạo nhánh mới từ `develop` để làm chức năng tiếp theo. Không dùng lại nhánh cũ đã merge.

## 5. Các lệnh cứu anh em sẽ cần 

Xem mình đang ở nhánh nào và đã sửa file gì:

```bash
git branch --show-current
git status
```

Bỏ các file vừa `git add` khỏi vùng chờ commit nhưng vẫn giữ code:

```bash
git restore --staged .
```

Hủy một lần merge đang bị conflict và quay lại trạng thái trước khi merge:

```bash
git merge --abort
```

Xem các commit gần nhất:

```bash
git log --oneline -10
```