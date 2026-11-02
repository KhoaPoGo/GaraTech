package dao;

import connectDB.ConnectDB;
import entity.DichVu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Truy vấn bảng DichVu. Các màn hình khác (lập phiếu sửa chữa) dùng {@link #findActive()}. */
public class DichVuDao {
    private static final String COLS = "maDichVu, tenDichVu, moTa, donGiaCong, thoiGianUocTinh, capDoBaoDuong, trangThai";

    public static final String HOAT_DONG = "Hoạt động";
    public static final String NGUNG = "Ngừng cung cấp";

    private static DichVu map(ResultSet rs) throws SQLException {
        return new DichVu(rs.getString("maDichVu"), rs.getString("tenDichVu"), rs.getString("moTa"),
                rs.getBigDecimal("donGiaCong").doubleValue(), rs.getDouble("thoiGianUocTinh"),
                rs.getString("capDoBaoDuong"), rs.getString("trangThai"));
    }

    private List<DichVu> query(String where) throws SQLException {
        String sql = "SELECT " + COLS + " FROM DichVu " + where + " ORDER BY maDichVu";
        List<DichVu> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<DichVu> findAll() throws SQLException { return query(""); }

    /** Chỉ các dịch vụ đang cung cấp (dùng khi chọn dịch vụ cho phiếu sửa chữa). */
    public List<DichVu> findActive() throws SQLException { return query("WHERE trangThai = N'Hoạt động'"); }

    /** Có dịch vụ khác (khác mã exceptId) trùng tên không (không phân biệt hoa thường). */
    public boolean existsByName(String ten, String exceptId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM DichVu WHERE LOWER(tenDichVu) = LOWER(?) AND maDichVu <> ?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setNString(1, ten);
            ps.setString(2, exceptId == null ? "" : exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void insert(Connection c, DichVu d) throws SQLException {
        String sql = "INSERT INTO DichVu(maDichVu, tenDichVu, moTa, donGiaCong, thoiGianUocTinh, capDoBaoDuong, trangThai)"
                + " VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, d, false);
            ps.executeUpdate();
        }
    }

    public int update(DichVu d) throws SQLException {
        String sql = "UPDATE DichVu SET tenDichVu=?, moTa=?, donGiaCong=?, thoiGianUocTinh=?, capDoBaoDuong=?"
                + " WHERE maDichVu=?";
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, d, true);
            return ps.executeUpdate();
        }
    }

    public int setTrangThai(String maDichVu, String trangThai) throws SQLException {
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE DichVu SET trangThai=? WHERE maDichVu=?")) {
            ps.setNString(1, trangThai);
            ps.setString(2, maDichVu);
            return ps.executeUpdate();
        }
    }

    /** insert: thứ tự (ma, ten, moTa, gia, tg, cap, tt); update: (ten, moTa, gia, tg, cap, ma) và KHÔNG đổi trạng thái. */
    private static void bind(PreparedStatement ps, DichVu d, boolean forUpdate) throws SQLException {
        int i = 1;
        if (!forUpdate) ps.setString(i++, d.getMaDichVu());
        ps.setNString(i++, d.getTenDichVu());
        if (d.getMoTa() == null || d.getMoTa().isBlank()) ps.setNull(i++, java.sql.Types.NVARCHAR);
        else ps.setNString(i++, d.getMoTa());
        ps.setBigDecimal(i++, java.math.BigDecimal.valueOf(Math.round(d.getTienCong())));
        ps.setDouble(i++, d.getThoiGianUocTinh());
        if (d.getCapDoBaoDuong() == null || d.getCapDoBaoDuong().isBlank()) ps.setNull(i++, java.sql.Types.NVARCHAR);
        else ps.setNString(i++, d.getCapDoBaoDuong());
        if (forUpdate) ps.setString(i, d.getMaDichVu());
        else ps.setNString(i, d.getTrangThai());
    }
}
