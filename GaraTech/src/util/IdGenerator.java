package util;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Sinh mã khóa chính đúng định dạng trong sql/01_TaoBang.sql.
 *
 * <pre>
 *   KhachHang        KH-2611-001        PhieuTiepNhan    PTN-20261101-001
 *   NhanVien         NV26001            PhieuSuaChua     PSC-20261101-001
 *   DichVu           DV0001             PhieuNhapKho     PNK-20261101-001
 *   PhuTung          PT0001             PhieuXuatKho     PXK-20261101-001
 *   NhaCungCap       NCC001             GiaoDichThanhToan GD-20261101-001
 *   HangMucSuaChua   HM0001             HoaDon           HD20261101-0001
 *   ChiTietXuatKho   CTXK00001          ChiTietHoaDon    CTHD00001
 * </pre>
 *
 * Cách dùng:
 * <pre>
 * String ma = IdGenerator.next(IdGenerator.Kind.PHIEU_TIEP_NHAN);                 // tự mở kết nối
 * String ma = IdGenerator.next(c, IdGenerator.Kind.HOA_DON);                       // trong transaction (truyền c)
 * </pre>
 * Hàm tìm mã lớn nhất đang có cùng tiền tố rồi +1, nên gọi NGAY TRƯỚC khi insert (trong cùng transaction nếu có).
 */
public final class IdGenerator {
    private static final DateTimeFormatter YY = DateTimeFormatter.ofPattern("yy");
    private static final DateTimeFormatter YYMM = DateTimeFormatter.ofPattern("yyMM");
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Mỗi loại mã: bảng, cột khóa chính, số chữ số của số thứ tự, và cách tạo tiền tố theo ngày. */
    public enum Kind {
        KHACH_HANG("KhachHang", "maKH", 3, d -> "KH-" + d.format(YYMM) + "-"),
        NHAN_VIEN("NhanVien", "maNV", 3, d -> "NV" + d.format(YY)),
        DICH_VU("DichVu", "maDichVu", 4, d -> "DV"),
        PHU_TUNG("PhuTung", "maPhuTung", 4, d -> "PT"),
        NHA_CUNG_CAP("NhaCungCap", "maNCC", 3, d -> "NCC"),
        PHIEU_TIEP_NHAN("PhieuTiepNhan", "maPhieuTN", 3, d -> "PTN-" + d.format(YYYYMMDD) + "-"),
        PHIEU_SUA_CHUA("PhieuSuaChua", "maPhieuSC", 3, d -> "PSC-" + d.format(YYYYMMDD) + "-"),
        PHIEU_TIEP_NHAN_SUA_CHUA("PhieuTiepNhanSuaChua", "maPhieu", 3, d -> "PSC-" + d.format(YYYYMMDD) + "-"),
        HANG_MUC("HangMucSuaChua", "maHangMuc", 4, d -> "HM"),
        PHIEU_NHAP_KHO("PhieuNhapKho", "maPhieuNhap", 3, d -> "PNK-" + d.format(YYYYMMDD) + "-"),
        PHIEU_XUAT_KHO("PhieuXuatKho", "maPhieuXK", 3, d -> "PXK-" + d.format(YYYYMMDD) + "-"),
        CHI_TIET_XUAT_KHO("ChiTietXuatKho", "maChiTietXK", 5, d -> "CTXK"),
        HOA_DON("HoaDon", "maHoaDon", 4, d -> "HD" + d.format(YYYYMMDD) + "-"),
        CHI_TIET_HOA_DON("ChiTietHoaDon", "maChiTietHD", 5, d -> "CTHD"),
        GIAO_DICH("GiaoDichThanhToan", "maGiaoDich", 3, d -> "GD-" + d.format(YYYYMMDD) + "-");

        final String table;
        final String column;
        final int width;
        private final Function<LocalDate, String> prefix;

        Kind(String table, String column, int width, Function<LocalDate, String> prefix) {
            this.table = table;
            this.column = column;
            this.width = width;
            this.prefix = prefix;
        }

        /** Tiền tố của mã cho một ngày, ví dụ PHIEU_TIEP_NHAN.prefix(2026-11-01) = "PTN-20261101-". */
        public String prefix(LocalDate day) { return prefix.apply(day); }
    }

    private IdGenerator() {}

    /** Mã kế tiếp của loại này (tự mở và đóng kết nối). */
    public static String next(Kind kind) throws SQLException {
        try (Connection c = ConnectDB.getConnection()) {
            return next(c, kind);
        }
    }

    /** Dùng khi đang ở trong transaction (truyền connection của transaction vào). */
    public static String next(Connection c, Kind kind) throws SQLException {
        String prefix = kind.prefix(LocalDate.now());
        String sql = "SELECT MAX(" + kind.column + ") FROM " + kind.table + " WHERE " + kind.column + " LIKE ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                String max = rs.next() ? rs.getString(1) : null;
                return nextFrom(prefix, max, kind.width);
            }
        }
    }

    /**
     * Ghép mã kế tiếp từ tiền tố và mã lớn nhất hiện có (null nếu chưa có mã nào với tiền tố đó).
     * nextFrom("PT", "PT0004", 4) = "PT0005";  nextFrom("HD20261101-", null, 4) = "HD20261101-0001".
     */
    public static String nextFrom(String prefix, String currentMax, int width) {
        int seq = 0;
        if (currentMax != null && currentMax.length() > prefix.length()) {
            String digits = currentMax.substring(prefix.length()).replaceAll("\\D", "");
            if (!digits.isEmpty()) seq = Integer.parseInt(digits);
        }
        return prefix + String.format("%0" + width + "d", seq + 1);
    }
}
