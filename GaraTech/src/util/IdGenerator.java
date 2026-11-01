package util;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Sinh mã theo quy luật trong báo cáo ("Định dạng tự sinh mã").
 *   KH-YYMM-STT      ví dụ KH-2610-001
 *   NV-YY-STT        ví dụ NV-26-004
 *   PTN-YYYYMMDD-STT, PSC-..., HD-..., PNK-..., PXK-... (chứng từ theo ngày)
 *   DV-BD-STT, PT-DC-STT  (danh mục theo nhóm)
 *
 * Cách dùng: prefix(...) tạo phần đầu, next(...) hỏi CSDL số thứ tự kế tiếp.
 * <pre>
 * String ma = IdGenerator.next("KhachHang", "maKH", IdGenerator.khachHang());   // KH-2610-005
 * </pre>
 * Lưu ý: tên bảng / cột bên dưới phải khớp DDL của nhóm.
 */
public final class IdGenerator {
    private static final DateTimeFormatter YYMM = DateTimeFormatter.ofPattern("yyMM");
    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdGenerator() {}

    public static String khachHang() { return "KH-" + LocalDate.now().format(YYMM) + "-"; }
    public static String nhanVien() { return "NV-" + LocalDate.now().format(YY) + "-"; }
    /** chungTu("PTN") -> "PTN-20261031-" ; dùng cho PTN, PSC, HD, PNK, PXK */
    public static String chungTu(String loai) { return loai + "-" + LocalDate.now().format(YYYYMMDD) + "-"; }
    /** danhMuc("DV","BD") -> "DV-BD-" ; danhMuc("PT","DC") -> "PT-DC-" */
    public static String danhMuc(String loai, String nhom) { return loai + "-" + nhom + "-"; }

    /** Tìm mã lớn nhất đang có với prefix rồi +1, đệm 3 chữ số. */
    public static String next(String table, String column, String prefix) throws SQLException {
        try (Connection c = ConnectDB.getConnection()) {
            return next(c, table, column, prefix);
        }
    }

    /** Dùng khi đang ở trong transaction (truyền connection của transaction vào). */
    public static String next(Connection c, String table, String column, String prefix) throws SQLException {
        String sql = "SELECT MAX(" + column + ") FROM " + table + " WHERE " + column + " LIKE ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int seq = 0;
                if (rs.next() && rs.getString(1) != null) {
                    String max = rs.getString(1);
                    seq = Integer.parseInt(max.substring(prefix.length()).replaceAll("\\D", ""));
                }
                return prefix + String.format("%03d", seq + 1);
            }
        }
    }
}
