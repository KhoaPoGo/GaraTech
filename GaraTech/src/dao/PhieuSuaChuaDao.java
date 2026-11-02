package dao;

import connectDB.ConnectDB;
import entity.HangMucSuaChua;
import entity.PhieuSuaChua;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PhieuSuaChuaDao {

    public record PhieuSuaChuaRow(
            String maPhieuSC,
            String maPhieuTN,
            String bienSo,
            String thongTinXe,
            String maKH,
            String tenKH,
            String sdtKH,
            String maNVLap,
            String tenNVLap,
            String maKTV,
            String tenKTV,
            LocalDateTime thoiGianDuKien,
            LocalDateTime thoiGianBatDau,
            LocalDateTime thoiGianHoanThanh,
            String ghiChuKyThuat,
            String lyDoHuy,
            String trangThai,
            double tongTien,
            int tongHangMuc,
            int hangMucXong
    ) {
    }

    private static final String SELECT_BASE = """
            SELECT p.maPhieuSC, p.maPhieuTN, p.maKTV, p.maNVLap,
                   p.thoiGianDuKien, p.thoiGianBatDau, p.thoiGianHoanThanh,
                   p.ghiChuKyThuat, p.lyDoHuy, p.trangThai,
                   tn.bienSo, x.hangXe, x.model, x.namSanXuat, x.mauXe,
                   kh.maKH, kh.hoTen AS tenKH, kh.soDienThoai AS sdtKH,
                   nvLap.hoTen AS tenNVLap,
                   ktv.hoTen AS tenKTV,
                   ISNULL((SELECT SUM(h.thanhTien) FROM HangMucSuaChua h WHERE h.maPhieuSC = p.maPhieuSC AND h.trangThai <> N'Từ chối'), 0) AS tongTien,
                   (SELECT COUNT(*) FROM HangMucSuaChua h WHERE h.maPhieuSC = p.maPhieuSC AND h.trangThai <> N'Từ chối') AS tongHangMuc,
                   (SELECT COUNT(*) FROM HangMucSuaChua h WHERE h.maPhieuSC = p.maPhieuSC AND h.trangThai = N'Hoàn thành') AS hangMucXong
            FROM PhieuSuaChua p
            JOIN PhieuTiepNhan tn ON p.maPhieuTN = tn.maPhieuTN
            JOIN Xe x ON tn.bienSo = x.bienSo
            JOIN KhachHang kh ON tn.maKH = kh.maKH
            JOIN NhanVien nvLap ON p.maNVLap = nvLap.maNV
            LEFT JOIN NhanVien ktv ON p.maKTV = ktv.maNV
            """;

    private static PhieuSuaChuaRow mapRow(ResultSet rs) throws SQLException {
        Timestamp tsDk = rs.getTimestamp("thoiGianDuKien");
        Timestamp tsBd = rs.getTimestamp("thoiGianBatDau");
        Timestamp tsHt = rs.getTimestamp("thoiGianHoanThanh");

        String hangXe = rs.getString("hangXe");
        String model = rs.getString("model");
        int nam = rs.getInt("namSanXuat");
        String mau = rs.getString("mauXe");
        String thongTinXe = String.format("%s %s %d · %s", hangXe, model, nam, mau);

        return new PhieuSuaChuaRow(
                rs.getString("maPhieuSC"),
                rs.getString("maPhieuTN"),
                rs.getString("bienSo"),
                thongTinXe,
                rs.getString("maKH"),
                rs.getString("tenKH"),
                rs.getString("sdtKH"),
                rs.getString("maNVLap"),
                rs.getString("tenNVLap"),
                rs.getString("maKTV"),
                rs.getString("tenKTV"),
                tsDk != null ? tsDk.toLocalDateTime() : null,
                tsBd != null ? tsBd.toLocalDateTime() : null,
                tsHt != null ? tsHt.toLocalDateTime() : null,
                rs.getString("ghiChuKyThuat"),
                rs.getString("lyDoHuy"),
                rs.getString("trangThai"),
                rs.getDouble("tongTien"),
                rs.getInt("tongHangMuc"),
                rs.getInt("hangMucXong")
        );
    }

    /**
     * Tạo mới Phiếu sửa chữa cùng toàn bộ các hạng mục trong 1 Transaction an toàn.
     */
    public String insertWithItems(PhieuSuaChua psc, List<HangMucSuaChua> hangMucs) throws SQLException {
        String sqlPsc = """
                INSERT INTO PhieuSuaChua(maPhieuSC, maPhieuTN, maKTV, maNVLap, thoiGianDuKien,
                                         thoiGianBatDau, thoiGianHoanThanh, ghiChuKyThuat, lyDoHuy, trangThai)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String sqlHm = """
                INSERT INTO HangMucSuaChua(maHangMuc, maPhieuSC, loaiHangMuc, maDichVu, maPhuTung,
                                           tenHangMuc, soLuong, donGia, mucGiamGia, laPhatSinh,
                                           thoiHanBaoHanh, trangThai, ghiChu)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = ConnectDB.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Tạo mã phiếu sửa chữa
                String maPSC = IdGenerator.next(c, IdGenerator.Kind.PHIEU_SUA_CHUA);
                psc.setMaPhieuSC(maPSC);

                try (PreparedStatement psPsc = c.prepareStatement(sqlPsc)) {
                    psPsc.setString(1, maPSC);
                    psPsc.setString(2, psc.getMaPhieuTN());
                    psPsc.setString(3, psc.getMaKTV());
                    psPsc.setString(4, psc.getMaNVLap());
                    psPsc.setTimestamp(5, psc.getThoiGianDuKien() != null ? Timestamp.valueOf(psc.getThoiGianDuKien()) : null);
                    psPsc.setTimestamp(6, psc.getThoiGianBatDau() != null ? Timestamp.valueOf(psc.getThoiGianBatDau()) : null);
                    psPsc.setTimestamp(7, psc.getThoiGianHoanThanh() != null ? Timestamp.valueOf(psc.getThoiGianHoanThanh()) : null);
                    psPsc.setString(8, psc.getGhiChuKyThuat());
                    psPsc.setString(9, psc.getLyDoHuy());
                    psPsc.setString(10, psc.getTrangThai() != null ? psc.getTrangThai() : "Chờ khách duyệt");
                    psPsc.executeUpdate();
                }

                // Thêm các hạng mục
                if (hangMucs != null && !hangMucs.isEmpty()) {
                    try (PreparedStatement psHm = c.prepareStatement(sqlHm)) {
                        for (HangMucSuaChua hm : hangMucs) {
                            String maHM = IdGenerator.next(c, IdGenerator.Kind.HANG_MUC);
                            hm.setMaHangMuc(maHM);
                            hm.setMaPhieuSC(maPSC);

                            psHm.setString(1, maHM);
                            psHm.setString(2, maPSC);
                            psHm.setString(3, hm.getLoaiHangMuc());
                            psHm.setString(4, hm.getMaDichVu());
                            psHm.setString(5, hm.getMaPhuTung());
                            psHm.setString(6, hm.getTenHangMuc());
                            psHm.setInt(7, hm.getSoLuong());
                            psHm.setDouble(8, hm.getDonGia());
                            psHm.setDouble(9, hm.getMucGiamGia());
                            psHm.setBoolean(10, hm.isLaPhatSinh());
                            psHm.setTimestamp(11, hm.getThoiHanBaoHanh() != null ? Timestamp.valueOf(hm.getThoiHanBaoHanh()) : null);
                            psHm.setString(12, hm.getTrangThai() != null ? hm.getTrangThai() : "Chờ duyệt");
                            psHm.setString(13, hm.getGhiChu());
                            psHm.addBatch();
                        }
                        psHm.executeBatch();
                    }
                }

                // Đồng bộ cập nhật phiếu tiếp nhận sang trạng thái Đã xác nhận (nếu đang ở chờ xác nhận)
                try (PreparedStatement psPtn = c.prepareStatement(
                        "UPDATE PhieuTiepNhan SET trangThai = N'Đã xác nhận' WHERE maPhieuTN = ? AND trangThai LIKE N'Chờ%'")) {
                    psPtn.setString(1, psc.getMaPhieuTN());
                    psPtn.executeUpdate();
                }

                c.commit();
                return maPSC;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public PhieuSuaChuaRow findById(String maPhieuSC) throws SQLException {
        String sql = SELECT_BASE + " WHERE p.maPhieuSC = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuSC);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public PhieuSuaChuaRow findByMaPhieuTN(String maPhieuTN) throws SQLException {
        String sql = SELECT_BASE + " WHERE p.maPhieuTN = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieuTN);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<PhieuSuaChuaRow> search(LocalDate fromDate, LocalDate toDate, String keyword, String trangThai) throws SQLException {
        List<PhieuSuaChuaRow> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (fromDate != null) {
            sql.append(" AND p.thoiGianDuKien >= ?");
            params.add(Timestamp.valueOf(fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            sql.append(" AND p.thoiGianDuKien <= ?");
            params.add(Timestamp.valueOf(toDate.atTime(23, 59, 59)));
        }
        if (trangThai != null && !trangThai.isBlank() && !trangThai.equalsIgnoreCase("Tất cả")) {
            sql.append(" AND p.trangThai = ?");
            params.add(trangThai.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            sql.append(" AND (p.maPhieuSC LIKE ? OR p.maPhieuTN LIKE ? OR tn.bienSo LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?)");
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        sql.append(" ORDER BY p.maPhieuSC DESC");

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean updateTrangThai(String maPhieuSC, String trangThaiMoi, String lyDoHuy) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET trangThai = ?, lyDoHuy = ? WHERE maPhieuSC = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trangThaiMoi);
            ps.setString(2, lyDoHuy);
            ps.setString(3, maPhieuSC);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateThoiGian(String maPhieuSC, LocalDateTime batDau, LocalDateTime hoanThanh) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET thoiGianBatDau = COALESCE(?, thoiGianBatDau), thoiGianHoanThanh = COALESCE(?, thoiGianHoanThanh) WHERE maPhieuSC = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, batDau != null ? Timestamp.valueOf(batDau) : null);
            ps.setTimestamp(2, hoanThanh != null ? Timestamp.valueOf(hoanThanh) : null);
            ps.setString(3, maPhieuSC);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateKtv(String maPhieuSC, String maKTV) throws SQLException {
        String sql = "UPDATE PhieuSuaChua SET maKTV = ? WHERE maPhieuSC = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maKTV);
            ps.setString(2, maPhieuSC);
            return ps.executeUpdate() > 0;
        }
    }
}
