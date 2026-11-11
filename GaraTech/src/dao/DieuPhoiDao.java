package dao;

import connectDB.ConnectDB;
import enums.TrangThaiPhieu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Read-only queries for the unified PhieuTiepNhanSuaChua workflow. */
public class DieuPhoiDao {
    public record PhieuCard(String maPhieuSC, TrangThaiPhieu trangThai, String bienSo, String tenXe,
                            String tenKhach, String maKTV, String tenKTV, LocalDateTime duKien,
                            LocalDateTime batDau, String yeuCau, int tongHangMuc, int hangMucXong) {
        public int tienDo() {
            if (trangThai == TrangThaiPhieu.HOAN_TAT) return 100;
            return 0; // SQL schema hiện tại không lưu tiến độ từng hạng mục.
        }
        public boolean quaHan(LocalDateTime now) {
            return duKien != null && duKien.isBefore(now)
                    && (trangThai == TrangThaiPhieu.DANG_SUA || trangThai == TrangThaiPhieu.CHO_LINH_KIEN);
        }
    }

    /** Giữ type cũ để tương thích panel KCS; danh sách KTV không dùng trong điều phối chỉ xem. */
    public record KtvLoad(String maNV, String hoTen, int soPhieuDangGiu) {
        public boolean ranh() { return soPhieuDangGiu == 0; }
    }
    public record HangMuc(String ten, String loai, int soLuong, String trangThai, boolean phatSinh) {}

    private static final String SELECT = """
            SELECT p.maPhieu, p.trangThai, p.bienSo, x.hangXe, x.model,
                   kh.hoTen AS tenKH, p.maNVKyThuat AS maKTV, kt.hoTen AS tenKTV,
                   p.thoiGianHenTra AS thoiGianDuKien, p.thoiGianBatDau,
                   p.yeuCauBanDau AS yeuCau,
                   (SELECT COUNT(*) FROM ChiTietDichVu d WHERE d.maPhieu=p.maPhieu)
                     + (SELECT COUNT(*) FROM ChiTietLinhKien l WHERE l.maPhieu=p.maPhieu) AS tongHM,
                   CASE WHEN p.trangThai='HOAN_TAT' THEN
                     (SELECT COUNT(*) FROM ChiTietDichVu d WHERE d.maPhieu=p.maPhieu)
                     + (SELECT COUNT(*) FROM ChiTietLinhKien l WHERE l.maPhieu=p.maPhieu)
                     ELSE 0 END AS xongHM
            FROM PhieuTiepNhanSuaChua p
            JOIN Xe x ON x.bienSo=p.bienSo
            JOIN KhachHang kh ON kh.maKH=x.maKH
            LEFT JOIN NhanVien kt ON kt.maNV=p.maNVKyThuat
            """;

    private static final String ORDER = " ORDER BY CASE WHEN p.thoiGianHenTra IS NULL THEN 1 ELSE 0 END, p.thoiGianHenTra, p.maPhieu";
    private static LocalDateTime ldt(Timestamp t) { return t == null ? null : t.toLocalDateTime(); }
    private static PhieuCard map(ResultSet rs) throws SQLException {
        return new PhieuCard(rs.getString("maPhieu"), TrangThaiPhieu.fromDb(rs.getString("trangThai")),
                rs.getString("bienSo"), rs.getString("hangXe") + " " + rs.getString("model"), rs.getString("tenKH"),
                rs.getString("maKTV"), rs.getString("tenKTV"), ldt(rs.getTimestamp("thoiGianDuKien")),
                ldt(rs.getTimestamp("thoiGianBatDau")), rs.getString("yeuCau"), rs.getInt("tongHM"), rs.getInt("xongHM"));
    }

    public List<PhieuCard> findByTrangThai(int limit, TrangThaiPhieu... statuses) throws SQLException {
        String sql = SELECT + (statuses.length == 0 ? " WHERE 1=0" : " WHERE p.trangThai IN (" + "?,".repeat(statuses.length - 1) + "?)") + ORDER;
        if (limit > 0) sql = sql.replaceFirst("SELECT ", "SELECT TOP " + limit + " ");
        List<PhieuCard> out = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i=0;i<statuses.length;i++) ps.setString(i+1, statuses[i].name());
            try (ResultSet rs=ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public PhieuCard findById(String id) throws SQLException {
        try (Connection c=ConnectDB.getConnection(); PreparedStatement ps=c.prepareStatement(SELECT+" WHERE p.maPhieu=?")) {
            ps.setString(1,id); try(ResultSet rs=ps.executeQuery()){ return rs.next()?map(rs):null; }
        }
    }

    public List<KtvLoad> findKtvVaTaiTrong() { return List.of(); }
    public boolean laKtvDangLam(String id) { return false; }

    public List<HangMuc> findHangMuc(String id) throws SQLException {
        List<HangMuc> out=new ArrayList<>();
        String sql="SELECT dv.tenDichVu AS ten, N'Dịch vụ' AS loai, d.soLuong FROM ChiTietDichVu d JOIN DichVu dv ON dv.maDichVu=d.maDichVu WHERE d.maPhieu=? "
                +"UNION ALL SELECT pt.tenPhuTung, N'Phụ tùng', l.soLuong FROM ChiTietLinhKien l JOIN PhuTung pt ON pt.maPhuTung=l.maPhuTung WHERE l.maPhieu=?";
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setString(1,id);ps.setString(2,id);try(ResultSet rs=ps.executeQuery()){while(rs.next())out.add(new HangMuc(rs.getString(1),rs.getString(2),rs.getInt(3),"Đã ghi nhận",false));}}
        return out;
    }
    public int demHangMucChuaXong(String id) { return 0; }
    public boolean daXuatKho(String id) throws SQLException {
        String sql="SELECT COUNT(*) FROM PhieuXuatKho WHERE maPhieu=? AND trangThai=N'Đã xuất kho'";
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setString(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()&&rs.getInt(1)>0;}}
    }
    public String findGhiChuKyThuat(String id) throws SQLException {
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement("SELECT ghiChuKyThuat FROM PhieuTiepNhanSuaChua WHERE maPhieu=?")){ps.setString(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getString(1):null;}}
    }

    /** Chuyển trạng thái KCS trên phiếu hợp nhất; điều phối không gọi các cập nhật này. */
    public int hoanThanh(String id,String note) throws SQLException {
        String sql="UPDATE PhieuTiepNhanSuaChua SET trangThai='HOAN_TAT', thoiGianHoanThanh=SYSDATETIME(), ghiChuKyThuat=? WHERE maPhieu=? AND trangThai IN ('DANG_SUA','CHO_LINH_KIEN')";
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setNString(1,note);ps.setString(2,id);return ps.executeUpdate();}
    }
    public int traVeLamLai(String id,String note) throws SQLException {
        String sql="UPDATE PhieuTiepNhanSuaChua SET trangThai='DANG_SUA', ghiChuKyThuat=? WHERE maPhieu=? AND trangThai='CHO_LINH_KIEN'";
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setNString(1,note);ps.setString(2,id);return ps.executeUpdate();}
    }
}
