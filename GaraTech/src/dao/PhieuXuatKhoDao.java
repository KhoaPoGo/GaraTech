package dao;

import connectDB.ConnectDB;
import util.IdGenerator;
import util.IdGenerator.Kind;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DAO xử lý yêu cầu xuất phụ tùng của Kỹ thuật viên và nghiệp vụ duyệt xuất kho.
 *
 * Tên bảng/cột được căn cứ theo sql/01_TaoBang.sql:
 *   PhieuXuatKho(maPhieuXK, maPhieuSC, maKTVYeuCau, maNVKho, ngayYeuCau,
 *                ngayXuat, lyDoXuat, lyDoTuChoi, tongGiaTri, trangThai)
 *   ChiTietXuatKho(maChiTietXK, maPhieuXK, maPhuTung, soLuongXuat,
 *                  donGiaXuat, thanhTien, ghiChu)
 *
 * Lưu ý: entity.PhieuXuatKho hiện chưa có đủ các thuộc tính liên kết theo schema
 * (maPhieuSC, maKTVYeuCau, maNVKho, ngayYeuCau, lyDoTuChoi). Vì vậy DAO dùng
 * các record kết quả và tham số nghiệp vụ riêng, không giả định các thuộc tính
 * chưa tồn tại trong Entity.
 */
public class PhieuXuatKhoDao {

    public record PhieuXuatKhoRow(
            String maPhieuXK,
            String maPhieuSC,
            String maKTVYeuCau,
            String tenKTVYeuCau,
            String maNVKho,
            String tenNVKho,
            LocalDateTime ngayYeuCau,
            LocalDateTime ngayXuat,
            String lyDoXuat,
            String lyDoTuChoi,
            BigDecimal tongGiaTri,
            String trangThai
    ) {
    }

    public record ChiTietXuatKhoRow(
            String maChiTietXK,
            String maPhieuXK,
            String maPhuTung,
            String tenPhuTung,
            String donViTinh,
            int soLuongXuat,
            int soLuongTon,
            BigDecimal donGiaXuat,
            BigDecimal thanhTien,
            String ghiChu
    ) {
    }

    /**
     * Một dòng phụ tùng KTV yêu cầu xuất.
     * Giá xuất được DAO chụp từ PhuTung.donGiaBan tại thời điểm lập yêu cầu.
     */
    public record ChiTietYeuCau(
            String maPhuTung,
            int soLuongXuat,
            String ghiChu
    ) {
        public ChiTietYeuCau {
            if (maPhuTung == null || maPhuTung.isBlank()) {
                throw new IllegalArgumentException("Mã phụ tùng không được để trống.");
            }
            if (soLuongXuat <= 0) {
                throw new IllegalArgumentException("Số lượng xuất phải lớn hơn 0.");
            }
            if (ghiChu != null && ghiChu.length() > 200) {
                throw new IllegalArgumentException("Ghi chú chi tiết không được vượt quá 200 ký tự.");
            }
        }
    }

    private record PartQuantity(String maPhuTung, int soLuongXuat) {
    }

    private static final String STATUS_PENDING = "Chờ xác nhận";
    private static final String STATUS_ISSUED = "Đã xuất kho";
    private static final String STATUS_CANCELED = "Đã hủy";

    private static final String SELECT_REQUESTS = """
            SELECT px.maPhieuXK,
                   px.maPhieuSC,
                   px.maKTVYeuCau,
                   ktv.hoTen AS tenKTVYeuCau,
                   px.maNVKho,
                   nvKho.hoTen AS tenNVKho,
                   px.ngayYeuCau,
                   px.ngayXuat,
                   px.lyDoXuat,
                   px.lyDoTuChoi,
                   px.tongGiaTri,
                   px.trangThai
            FROM PhieuXuatKho px
            LEFT JOIN NhanVien ktv ON px.maKTVYeuCau = ktv.maNV
            LEFT JOIN NhanVien nvKho ON px.maNVKho = nvKho.maNV
            """;

    /**
     * Lấy tất cả phiếu xuất, mới gửi gần nhất đứng trước.
     */
    public List<PhieuXuatKhoRow> findAll() throws SQLException {
        return queryRequests("", null);
    }

    /**
     * Lấy các yêu cầu xuất kho do một Kỹ thuật viên gửi.
     */
    public List<PhieuXuatKhoRow> findByTechnician(String maKTV) throws SQLException {
        requireText(maKTV, "Mã kỹ thuật viên");
        return queryRequests(" WHERE px.maKTVYeuCau = ?", maKTV);
    }

    /**
     * Lọc yêu cầu theo trạng thái: Chờ xác nhận, Đã xuất kho hoặc Đã hủy.
     */
    public List<PhieuXuatKhoRow> findByStatus(String trangThai) throws SQLException {
        if (!isValidStatus(trangThai)) {
            throw new IllegalArgumentException("Trạng thái phiếu xuất kho không hợp lệ.");
        }
        return queryRequests(" WHERE px.trangThai = ?", trangThai);
    }

    /**
     * Tìm một phiếu xuất kho theo mã.
     * Trả về null nếu không tìm thấy.
     */
    public PhieuXuatKhoRow findById(String maPhieuXK) throws SQLException {
        requireText(maPhieuXK, "Mã phiếu xuất kho");
        List<PhieuXuatKhoRow> rows =
                queryRequests(" WHERE px.maPhieuXK = ?", maPhieuXK);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Lấy chi tiết phụ tùng của một phiếu xuất kho.
     */
    public List<ChiTietXuatKhoRow> findDetailsByRequest(String maPhieuXK) throws SQLException {
        requireText(maPhieuXK, "Mã phiếu xuất kho");

        String sql = """
                SELECT ct.maChiTietXK,
                       ct.maPhieuXK,
                       ct.maPhuTung,
                       pt.tenPhuTung,
                       pt.donViTinh,
                       ct.soLuongXuat,
                       pt.soLuongTon,
                       ct.donGiaXuat,
                       ct.thanhTien,
                       ct.ghiChu
                FROM ChiTietXuatKho ct
                JOIN PhuTung pt ON ct.maPhuTung = pt.maPhuTung
                WHERE ct.maPhieuXK = ?
                ORDER BY pt.tenPhuTung
                """;

        List<ChiTietXuatKhoRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuXK);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ChiTietXuatKhoRow(
                            rs.getString("maChiTietXK"),
                            rs.getString("maPhieuXK"),
                            rs.getString("maPhuTung"),
                            rs.getString("tenPhuTung"),
                            rs.getString("donViTinh"),
                            rs.getInt("soLuongXuat"),
                            rs.getInt("soLuongTon"),
                            rs.getBigDecimal("donGiaXuat"),
                            rs.getBigDecimal("thanhTien"),
                            rs.getString("ghiChu")
                    ));
                }
            }
        }
        return result;
    }

    /**
     * KTV gửi yêu cầu xuất phụ tùng.
     *
     * Hàm này:
     * 1. Kiểm tra KTV được phân công đúng phiếu sửa chữa.
     * 2. Tạo PhieuXuatKho ở trạng thái "Chờ xác nhận".
     * 3. Tạo các ChiTietXuatKho trong cùng transaction.
     * 4. Chụp donGiaBan hiện tại của phụ tùng vào donGiaXuat.
     *
     * Không trừ tồn kho tại bước gửi yêu cầu. Tồn kho chỉ được trừ khi kho duyệt.
     *
     * @return mã phiếu xuất kho vừa tạo.
     */
    public String insertRequest(
            String maPhieuSC,
            String maKTVYeuCau,
            String lyDoXuat,
            List<ChiTietYeuCau> chiTietYeuCaus
    ) throws SQLException {

        requireText(maPhieuSC, "Mã phiếu sửa chữa");
        requireText(maKTVYeuCau, "Mã kỹ thuật viên");
        requireText(lyDoXuat, "Lý do xuất");

        if (lyDoXuat.length() > 200) {
            throw new IllegalArgumentException("Lý do xuất không được vượt quá 200 ký tự.");
        }
        if (chiTietYeuCaus == null || chiTietYeuCaus.isEmpty()) {
            throw new IllegalArgumentException("Phiếu xuất phải có ít nhất một phụ tùng.");
        }

        Set<String> partIds = new HashSet<>();
        for (ChiTietYeuCau item : chiTietYeuCaus) {
            if (item == null) {
                throw new IllegalArgumentException("Chi tiết phiếu xuất không được null.");
            }
            if (!partIds.add(item.maPhuTung())) {
                throw new IllegalArgumentException(
                        "Một phụ tùng chỉ được xuất hiện một lần trong cùng phiếu: " + item.maPhuTung());
            }
        }

        return ConnectDB.inTransaction(c -> {
            // Bảo đảm người gửi là KTV đang được phân công cho phiếu tiếp nhận - sửa chữa này.
            String sqlCheckTechnician =
                    "SELECT maNVKyThuat AS maKTV FROM PhieuTiepNhanSuaChua WHERE maPhieu = ?";
            try (PreparedStatement ps = c.prepareStatement(sqlCheckTechnician)) {
                ps.setString(1, maPhieuSC);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Không tìm thấy phiếu sửa chữa: " + maPhieuSC);
                    }
                    String maKTVDuocPhanCong = rs.getString("maKTV");
                    if (maKTVDuocPhanCong == null
                            || !maKTVDuocPhanCong.equalsIgnoreCase(maKTVYeuCau)) {
                        throw new SQLException(
                                "Kỹ thuật viên không được phân công cho phiếu sửa chữa này.");
                    }
                }
            }

            String maPhieuXK = IdGenerator.next(c, Kind.PHIEU_XUAT_KHO);

            String sqlHeader = """
                    INSERT INTO PhieuXuatKho
                        (maPhieuXK, maPhieuSC, maKTVYeuCau, lyDoXuat, tongGiaTri, trangThai)
                    VALUES (?, ?, ?, ?, 0, ?)
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlHeader)) {
                ps.setString(1, maPhieuXK);
                ps.setString(2, maPhieuSC);
                ps.setString(3, maKTVYeuCau);
                ps.setString(4, lyDoXuat.trim());
                ps.setString(5, STATUS_PENDING);
                ps.executeUpdate();
            }

            String sqlGetPartPrice =
                    "SELECT donGiaBan FROM PhuTung WHERE maPhuTung = ?";
            String sqlInsertDetail = """
                    INSERT INTO ChiTietXuatKho
                        (maChiTietXK, maPhieuXK, maPhuTung, soLuongXuat, donGiaXuat, ghiChu)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement psPrice = c.prepareStatement(sqlGetPartPrice);
                 PreparedStatement psDetail = c.prepareStatement(sqlInsertDetail)) {

                for (ChiTietYeuCau item : chiTietYeuCaus) {
                    psPrice.setString(1, item.maPhuTung());

                    BigDecimal donGiaXuat;
                    try (ResultSet rs = psPrice.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Không tìm thấy phụ tùng: " + item.maPhuTung());
                        }
                        donGiaXuat = rs.getBigDecimal("donGiaBan");
                    }

                    String maChiTietXK = IdGenerator.next(c, Kind.CHI_TIET_XUAT_KHO);
                    psDetail.setString(1, maChiTietXK);
                    psDetail.setString(2, maPhieuXK);
                    psDetail.setString(3, item.maPhuTung());
                    psDetail.setInt(4, item.soLuongXuat());
                    psDetail.setBigDecimal(5, donGiaXuat);
                    psDetail.setString(6, item.ghiChu());
                    psDetail.addBatch();
                }

                psDetail.executeBatch();
            }

            // thanhTien là cột computed trong SQL Server.
            String sqlUpdateTotal = """
                    UPDATE PhieuXuatKho
                    SET tongGiaTri = (
                        SELECT ISNULL(SUM(ct.thanhTien), 0)
                        FROM ChiTietXuatKho ct
                        WHERE ct.maPhieuXK = ?
                    )
                    WHERE maPhieuXK = ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlUpdateTotal)) {
                ps.setString(1, maPhieuXK);
                ps.setString(2, maPhieuXK);
                ps.executeUpdate();
            }

            return maPhieuXK;
        });
    }

    /**
     * Kho xác nhận xuất hàng.
     *
     * Việc kiểm tra trạng thái, trừ số lượng tồn từng phụ tùng và cập nhật phiếu
     * được thực hiện trong cùng một transaction. Nếu một phụ tùng không đủ tồn,
     * toàn bộ thao tác bị rollback, không có phụ tùng nào bị trừ dở dang.
     */
    public boolean approveExport(String maPhieuXK, String maNVKho) throws SQLException {
        requireText(maPhieuXK, "Mã phiếu xuất kho");
        requireText(maNVKho, "Mã nhân viên kho");

        return ConnectDB.inTransaction(c -> {
            // Khóa phiếu trong lúc duyệt để tránh hai nhân viên kho duyệt cùng một phiếu.
            String currentStatus;
            String sqlLockHeader =
                    "SELECT trangThai FROM PhieuXuatKho WITH (UPDLOCK, HOLDLOCK) WHERE maPhieuXK = ?";
            try (PreparedStatement ps = c.prepareStatement(sqlLockHeader)) {
                ps.setString(1, maPhieuXK);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Không tìm thấy phiếu xuất kho: " + maPhieuXK);
                    }
                    currentStatus = rs.getString("trangThai");
                }
            }

            if (!STATUS_PENDING.equals(currentStatus)) {
                throw new SQLException(
                        "Chỉ được duyệt phiếu ở trạng thái '" + STATUS_PENDING + "'.");
            }

            List<PartQuantity> details = new ArrayList<>();
            String sqlDetails = """
                    SELECT maPhuTung, soLuongXuat
                    FROM ChiTietXuatKho WITH (UPDLOCK, HOLDLOCK)
                    WHERE maPhieuXK = ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlDetails)) {
                ps.setString(1, maPhieuXK);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        details.add(new PartQuantity(
                                rs.getString("maPhuTung"),
                                rs.getInt("soLuongXuat")
                        ));
                    }
                }
            }

            if (details.isEmpty()) {
                throw new SQLException("Phiếu xuất kho chưa có chi tiết phụ tùng.");
            }

            String sqlDecreaseStock = """
                    UPDATE PhuTung
                    SET soLuongTon = soLuongTon - ?
                    WHERE maPhuTung = ? AND soLuongTon >= ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlDecreaseStock)) {
                for (PartQuantity item : details) {
                    ps.setInt(1, item.soLuongXuat());
                    ps.setString(2, item.maPhuTung());
                    ps.setInt(3, item.soLuongXuat());

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                                "Phụ tùng " + item.maPhuTung()
                                + " không tồn tại hoặc không đủ số lượng tồn kho.");
                    }
                }
            }

            String sqlUpdateHeader = """
                    UPDATE PhieuXuatKho
                    SET trangThai = ?,
                        maNVKho = ?,
                        ngayXuat = SYSDATETIME(),
                        tongGiaTri = (
                            SELECT ISNULL(SUM(ct.thanhTien), 0)
                            FROM ChiTietXuatKho ct
                            WHERE ct.maPhieuXK = ?
                        )
                    WHERE maPhieuXK = ? AND trangThai = ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlUpdateHeader)) {
                ps.setString(1, STATUS_ISSUED);
                ps.setString(2, maNVKho);
                ps.setString(3, maPhieuXK);
                ps.setString(4, maPhieuXK);
                ps.setString(5, STATUS_PENDING);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Không thể cập nhật trạng thái phiếu xuất kho.");
                }
            }

            return true;
        });
    }

    /**
     * Nhân viên kho từ chối yêu cầu.
     * Theo schema hiện tại, trạng thái từ chối được lưu là "Đã hủy" và lý do
     * được ghi vào cột lyDoTuChoi.
     */
    public boolean rejectRequest(
            String maPhieuXK,
            String maNVKho,
            String lyDoTuChoi
    ) throws SQLException {
        requireText(maPhieuXK, "Mã phiếu xuất kho");
        requireText(maNVKho, "Mã nhân viên kho");

        if (lyDoTuChoi != null && lyDoTuChoi.length() > 200) {
            throw new IllegalArgumentException("Lý do từ chối không được vượt quá 200 ký tự.");
        }

        String sql = """
                UPDATE PhieuXuatKho
                SET trangThai = ?,
                    maNVKho = ?,
                    lyDoTuChoi = ?
                WHERE maPhieuXK = ? AND trangThai = ?
                """;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, STATUS_CANCELED);
            ps.setString(2, maNVKho);
            ps.setString(3, lyDoTuChoi);
            ps.setString(4, maPhieuXK);
            ps.setString(5, STATUS_PENDING);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * KTV tự hủy yêu cầu của mình khi phiếu vẫn đang chờ kho xác nhận.
     */
    public boolean cancelRequestByTechnician(
            String maPhieuXK,
            String maKTVYeuCau
    ) throws SQLException {
        requireText(maPhieuXK, "Mã phiếu xuất kho");
        requireText(maKTVYeuCau, "Mã kỹ thuật viên");

        String sql = """
                UPDATE PhieuXuatKho
                SET trangThai = ?
                WHERE maPhieuXK = ?
                  AND maKTVYeuCau = ?
                  AND trangThai = ?
                """;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, STATUS_CANCELED);
            ps.setString(2, maPhieuXK);
            ps.setString(3, maKTVYeuCau);
            ps.setString(4, STATUS_PENDING);
            return ps.executeUpdate() == 1;
        }
    }

    private List<PhieuXuatKhoRow> queryRequests(
            String condition,
            String parameter
    ) throws SQLException {
        String sql = SELECT_REQUESTS + condition + " ORDER BY px.ngayYeuCau DESC";
        List<PhieuXuatKhoRow> result = new ArrayList<>();

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (parameter != null) {
                ps.setString(1, parameter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ngayYeuCau = rs.getTimestamp("ngayYeuCau");
                    Timestamp ngayXuat = rs.getTimestamp("ngayXuat");

                    result.add(new PhieuXuatKhoRow(
                            rs.getString("maPhieuXK"),
                            rs.getString("maPhieuSC"),
                            rs.getString("maKTVYeuCau"),
                            rs.getString("tenKTVYeuCau"),
                            rs.getString("maNVKho"),
                            rs.getString("tenNVKho"),
                            ngayYeuCau != null ? ngayYeuCau.toLocalDateTime() : null,
                            ngayXuat != null ? ngayXuat.toLocalDateTime() : null,
                            rs.getString("lyDoXuat"),
                            rs.getString("lyDoTuChoi"),
                            rs.getBigDecimal("tongGiaTri"),
                            rs.getString("trangThai")
                    ));
                }
            }
        }
        return result;
    }

    private static boolean isValidStatus(String trangThai) {
        return STATUS_PENDING.equals(trangThai)
                || STATUS_ISSUED.equals(trangThai)
                || STATUS_CANCELED.equals(trangThai);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
    }
}
