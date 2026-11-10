package dao;

import connectDB.ConnectDB;
import util.IdGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO thao tac voi bang ChiTietXuatKho.
 *
 * Schema duoc su dung:
 * ChiTietXuatKho(
 *     maChiTietXK, maPhieuXK, maPhuTung,
 *     soLuongXuat, donGiaXuat, thanhTien, ghiChu
 * )
 *
 * Luu y nghiep vu:
 * - thanhTien la cot tinh (computed column) trong SQL Server, khong INSERT/UPDATE truc tiep.
 * - donGiaXuat duoc chup tu PhuTung.donGiaBan tai thoi diem them chi tiet.
 * - Chi tiet chi duoc them/sua/xoa khi phieu cha con o trang thai "Cho xac nhan".
 * - Khong tru ton kho tai DAO nay. Ton kho chi duoc tru khi PhieuXuatKhoDao duyet phieu.
 */
public final class ChiTietXuatKhoDao {

    private static final String CHO_XAC_NHAN = "Chờ xác nhận";

    private static final String SELECT_DETAILS = """
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
            INNER JOIN PhuTung pt ON pt.maPhuTung = ct.maPhuTung
            """;

    /** Du lieu chi tiet kem ten phu tung va ton kho de hien thi tren giao dien. */
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
    ) { }

    /** Thong tin phieu cha cua mot dong chi tiet. */
    private record PhieuCha(String maPhieuXK, String trangThai) { }

    private ChiTietXuatKhoDao() { }

    /** Lay tat ca chi tiet xuat kho. */
    public static List<ChiTietXuatKhoRow> findAll() throws SQLException {
        return query(SELECT_DETAILS + " ORDER BY ct.maPhieuXK DESC, pt.tenPhuTung ASC", null);
    }

    /** Lay danh sach chi tiet theo ma phieu xuat kho. */
    public static List<ChiTietXuatKhoRow> findByMaPhieuXK(String maPhieuXK)
            throws SQLException {
        requireText(maPhieuXK, "Mã phiếu xuất kho");
        String sql = SELECT_DETAILS
                + " WHERE ct.maPhieuXK = ? ORDER BY pt.tenPhuTung ASC, ct.maChiTietXK ASC";
        return query(sql, maPhieuXK.trim());
    }

    /** Tim mot chi tiet theo ma chi tiet; tra ve null neu khong tim thay. */
    public static ChiTietXuatKhoRow findByMaChiTietXK(String maChiTietXK)
            throws SQLException {
        requireText(maChiTietXK, "Mã chi tiết xuất kho");
        String sql = SELECT_DETAILS + " WHERE ct.maChiTietXK = ?";
        List<ChiTietXuatKhoRow> rows = query(sql, maChiTietXK.trim());
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Them mot dong chi tiet vao phieu dang cho xac nhan.
     * Don gia duoc lay tu PhuTung.donGiaBan, khong nhan don gia tu giao dien.
     *
     * @return ma chi tiet xuat kho vua tao.
     */
    public static String insert(String maPhieuXK,
                                String maPhuTung,
                                int soLuongXuat,
                                String ghiChu) throws SQLException {
        requireText(maPhieuXK, "Mã phiếu xuất kho");
        requireText(maPhuTung, "Mã phụ tùng");
        validateQuantity(soLuongXuat);
        String note = normalizeNote(ghiChu);

        return ConnectDB.inTransaction(c -> {
            String trangThai = lockAndGetRequestStatus(c, maPhieuXK.trim());
            ensurePendingRequest(trangThai, maPhieuXK);

            if (existsPartInRequest(c, maPhieuXK.trim(), maPhuTung.trim())) {
                throw new SQLException("Phụ tùng này đã có trong phiếu xuất kho.");
            }

            BigDecimal donGiaXuat = findCurrentSellingPrice(c, maPhuTung.trim());
            String maChiTietXK = IdGenerator.next(
                    c, IdGenerator.Kind.CHI_TIET_XUAT_KHO);

            String sqlInsert = """
                    INSERT INTO ChiTietXuatKho
                        (maChiTietXK, maPhieuXK, maPhuTung,
                         soLuongXuat, donGiaXuat, ghiChu)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlInsert)) {
                ps.setString(1, maChiTietXK);
                ps.setString(2, maPhieuXK.trim());
                ps.setString(3, maPhuTung.trim());
                ps.setInt(4, soLuongXuat);
                ps.setBigDecimal(5, donGiaXuat);
                ps.setString(6, note);
                ps.executeUpdate();
            }

            updateRequestTotal(c, maPhieuXK.trim());
            return maChiTietXK;
        });
    }

    /**
     * Cap nhat so luong va ghi chu cua mot dong chi tiet.
     * Khong thay doi ma phu tung va don gia da duoc chot luc tao dong.
     *
     * @return true neu cap nhat duoc, false neu ma chi tiet khong ton tai.
     */
    public static boolean update(String maChiTietXK,
                                 int soLuongXuat,
                                 String ghiChu) throws SQLException {
        requireText(maChiTietXK, "Mã chi tiết xuất kho");
        validateQuantity(soLuongXuat);
        String note = normalizeNote(ghiChu);

        return ConnectDB.inTransaction(c -> {
            PhieuCha phieu = lockRequestByDetail(c, maChiTietXK.trim());
            if (phieu == null) {
                return false;
            }
            ensurePendingRequest(phieu.trangThai(), phieu.maPhieuXK());

            String sqlUpdate = """
                    UPDATE ChiTietXuatKho
                    SET soLuongXuat = ?, ghiChu = ?
                    WHERE maChiTietXK = ?
                    """;
            try (PreparedStatement ps = c.prepareStatement(sqlUpdate)) {
                ps.setInt(1, soLuongXuat);
                ps.setString(2, note);
                ps.setString(3, maChiTietXK.trim());
                if (ps.executeUpdate() != 1) {
                    return false;
                }
            }

            updateRequestTotal(c, phieu.maPhieuXK());
            return true;
        });
    }

    /**
     * Xoa mot dong chi tiet khi phieu van dang cho xac nhan.
     * Khong cho xoa dong cuoi cung; neu khong can xuat nua, hay huy ca phieu.
     *
     * @return true neu xoa duoc, false neu ma chi tiet khong ton tai.
     */
    public static boolean delete(String maChiTietXK) throws SQLException {
        requireText(maChiTietXK, "Mã chi tiết xuất kho");

        return ConnectDB.inTransaction(c -> {
            PhieuCha phieu = lockRequestByDetail(c, maChiTietXK.trim());
            if (phieu == null) {
                return false;
            }
            ensurePendingRequest(phieu.trangThai(), phieu.maPhieuXK());

            int soDong = countDetails(c, phieu.maPhieuXK());
            if (soDong <= 1) {
                throw new SQLException(
                        "Không thể xóa phụ tùng cuối cùng. Hãy hủy phiếu xuất nếu không còn nhu cầu.");
            }

            String sqlDelete = "DELETE FROM ChiTietXuatKho WHERE maChiTietXK = ?";
            try (PreparedStatement ps = c.prepareStatement(sqlDelete)) {
                ps.setString(1, maChiTietXK.trim());
                if (ps.executeUpdate() != 1) {
                    return false;
                }
            }

            updateRequestTotal(c, phieu.maPhieuXK());
            return true;
        });
    }

    // ---------------------------------------------------------------------
    // Cac ham noi bo
    // ---------------------------------------------------------------------

    private static List<ChiTietXuatKhoRow> query(String sql, String parameter)
            throws SQLException {
        List<ChiTietXuatKhoRow> result = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (parameter != null) {
                ps.setString(1, parameter);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    private static ChiTietXuatKhoRow mapRow(ResultSet rs) throws SQLException {
        return new ChiTietXuatKhoRow(
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
        );
    }

    private static String lockAndGetRequestStatus(Connection c, String maPhieuXK)
            throws SQLException {
        String sql = """
                SELECT trangThai
                FROM PhieuXuatKho WITH (UPDLOCK, HOLDLOCK)
                WHERE maPhieuXK = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuXK);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("trangThai") : null;
            }
        }
    }

    private static PhieuCha lockRequestByDetail(Connection c, String maChiTietXK)
            throws SQLException {
        String sql = """
                SELECT px.maPhieuXK, px.trangThai
                FROM ChiTietXuatKho ct WITH (UPDLOCK, HOLDLOCK)
                INNER JOIN PhieuXuatKho px WITH (UPDLOCK, HOLDLOCK)
                    ON px.maPhieuXK = ct.maPhieuXK
                WHERE ct.maChiTietXK = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maChiTietXK);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new PhieuCha(rs.getString("maPhieuXK"), rs.getString("trangThai"))
                        : null;
            }
        }
    }

    private static boolean existsPartInRequest(Connection c,
                                                String maPhieuXK,
                                                String maPhuTung) throws SQLException {
        String sql = """
                SELECT 1
                FROM ChiTietXuatKho WITH (UPDLOCK, HOLDLOCK)
                WHERE maPhieuXK = ? AND maPhuTung = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuXK);
            ps.setString(2, maPhuTung);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static BigDecimal findCurrentSellingPrice(Connection c, String maPhuTung)
            throws SQLException {
        String sql = "SELECT donGiaBan FROM PhuTung WHERE maPhuTung = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhuTung);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Không tìm thấy phụ tùng " + maPhuTung + ".");
                }
                BigDecimal price = rs.getBigDecimal("donGiaBan");
                if (price == null || price.signum() < 0) {
                    throw new SQLException("Đơn giá bán của phụ tùng " + maPhuTung + " không hợp lệ.");
                }
                return price;
            }
        }
    }

    private static int countDetails(Connection c, String maPhieuXK) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ChiTietXuatKho WHERE maPhieuXK = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuXK);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Cap nhat tongGiaTri tu cot thanhTien da duoc SQL Server tinh san. */
    private static void updateRequestTotal(Connection c, String maPhieuXK)
            throws SQLException {
        String sql = """
                UPDATE PhieuXuatKho
                SET tongGiaTri = ISNULL(
                    (SELECT SUM(ct.thanhTien)
                     FROM ChiTietXuatKho ct
                     WHERE ct.maPhieuXK = ?), 0)
                WHERE maPhieuXK = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuXK);
            ps.setString(2, maPhieuXK);
            ps.executeUpdate();
        }
    }

    private static void ensurePendingRequest(String trangThai, String maPhieuXK)
            throws SQLException {
        if (trangThai == null) {
            throw new SQLException("Không tìm thấy phiếu xuất kho " + maPhieuXK + ".");
        }
        if (!CHO_XAC_NHAN.equals(trangThai)) {
            throw new SQLException(
                    "Chỉ được thay đổi chi tiết khi phiếu ở trạng thái '" + CHO_XAC_NHAN + "'.");
        }
    }

    private static void validateQuantity(int soLuongXuat) {
        if (soLuongXuat <= 0) {
            throw new IllegalArgumentException("Số lượng xuất phải lớn hơn 0.");
        }
    }

    private static String normalizeNote(String ghiChu) {
        if (ghiChu == null || ghiChu.isBlank()) {
            return null;
        }
        String note = ghiChu.trim();
        if (note.length() > 200) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 200 ký tự.");
        }
        return note;
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
    }
}
