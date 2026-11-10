package dao;

import connectDB.ConnectDB;
import entity.ChiTietDichVu;
import entity.ChiTietLinhKien;
import entity.DichVu;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuTiepNhanSuaChua;
import entity.PhuTung;
import entity.Xe;
import enums.TrangThaiPhieu;
import util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp DAO quản lý cơ sở dữ liệu cho thực thể PhieuTiepNhanSuaChua
 * và 2 bảng chi tiết dịch vụ, chi tiết linh kiện.
 */
public class PhieuTiepNhanSuaChuaDao {

    /**
     * DTO Row tiện lợi cho các bảng giao diện Swing (JTable).
     */
    public record PhieuTiepNhanSuaChuaRow(
            String maPhieu,
            String bienSo,
            String thongTinXe,
            String maKH,
            String tenKH,
            String sdtKH,
            String maNVTiepNhan,
            String tenNVTiepNhan,
            String maNVKyThuat,
            String tenNVKyThuat,
            LocalDateTime ngayGioTiepNhan,
            String yeuCauBanDau,
            String tinhTrangXe,
            float soKmTiepNhan,
            float mucNhienLieu,
            LocalDateTime thoiGianBatDau,
            LocalDateTime thoiGianHoanThanh,
            String trangThai,
            double tongTienDichVu,
            double tongTienLinhKien,
            double tongChiPhi
    ) {}

    private static final String SELECT_BASE = """
            SELECT p.maPhieu, p.bienSo, p.maNVTiepNhan, p.maNVKyThuat,
                   p.ngayGioTiepNhan, p.yeuCauBanDau, p.tinhTrangXe,
                   p.soKmTiepNhan, p.mucNhienLieu, p.thoiGianHenTra,
                   p.thoiGianBatDau, p.thoiGianHoanThanh, p.ghiChuKyThuat, p.trangThai,
                   x.hangXe, x.model, x.namSanXuat, x.mauXe,
                   kh.maKH, kh.hoTen AS tenKH, kh.soDienThoai AS sdtKH,
                   nvtn.hoTen AS tenNVTiepNhan,
                   nvkt.hoTen AS tenNVKyThuat,
                   ISNULL((SELECT SUM(cdv.thanhTien) FROM ChiTietDichVu cdv WHERE cdv.maPhieu = p.maPhieu), 0) AS tongTienDichVu,
                   ISNULL((SELECT SUM(clk.thanhTien) FROM ChiTietLinhKien clk WHERE clk.maPhieu = p.maPhieu), 0) AS tongTienLinhKien,
                   ISNULL((SELECT SUM(cdv.thanhTien) FROM ChiTietDichVu cdv WHERE cdv.maPhieu = p.maPhieu), 0) +
                   ISNULL((SELECT SUM(clk.thanhTien) FROM ChiTietLinhKien clk WHERE clk.maPhieu = p.maPhieu), 0) AS tongChiPhi
            FROM PhieuTiepNhanSuaChua p
            JOIN Xe x ON p.bienSo = x.bienSo
            JOIN KhachHang kh ON x.maKH = kh.maKH
            JOIN NhanVien nvtn ON p.maNVTiepNhan = nvtn.maNV
            LEFT JOIN NhanVien nvkt ON p.maNVKyThuat = nvkt.maNV
            """;

    private static PhieuTiepNhanSuaChuaRow mapRow(ResultSet rs) throws SQLException {
        Timestamp tsNhan = rs.getTimestamp("ngayGioTiepNhan");
        Timestamp tsBd = rs.getTimestamp("thoiGianBatDau");
        Timestamp tsHt = rs.getTimestamp("thoiGianHoanThanh");

        String hangXe = rs.getString("hangXe");
        String model = rs.getString("model");
        int nam = rs.getInt("namSanXuat");
        String mau = rs.getString("mauXe");
        String thongTinXe = String.format("%s %s (%d - %s)", hangXe, model, nam, mau);

        return new PhieuTiepNhanSuaChuaRow(
                rs.getString("maPhieu"),
                rs.getString("bienSo"),
                thongTinXe,
                rs.getString("maKH"),
                rs.getString("tenKH"),
                rs.getString("sdtKH"),
                rs.getString("maNVTiepNhan"),
                rs.getString("tenNVTiepNhan"),
                rs.getString("maNVKyThuat"),
                rs.getString("tenNVKyThuat"),
                tsNhan != null ? tsNhan.toLocalDateTime() : null,
                rs.getString("yeuCauBanDau"),
                rs.getString("tinhTrangXe"),
                rs.getFloat("soKmTiepNhan"),
                rs.getFloat("mucNhienLieu"),
                tsBd != null ? tsBd.toLocalDateTime() : null,
                tsHt != null ? tsHt.toLocalDateTime() : null,
                rs.getString("trangThai"),
                rs.getDouble("tongTienDichVu"),
                rs.getDouble("tongTienLinhKien"),
                rs.getDouble("tongChiPhi")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. TẠO PHIẾU TIẾP NHẬN BAN ĐẦU (Chỉ định KTV, trạng thái CHO_SUA)
    // ─────────────────────────────────────────────────────────────────────────
    public String insert(PhieuTiepNhanSuaChua phieu) throws SQLException {
        String sql = """
                INSERT INTO PhieuTiepNhanSuaChua (
                    maPhieu, bienSo, maNVTiepNhan, maNVKyThuat, ngayGioTiepNhan,
                    yeuCauBanDau, tinhTrangXe, soKmTiepNhan, mucNhienLieu,
                    thoiGianHenTra, thoiGianBatDau, thoiGianHoanThanh, ghiChuKyThuat, trangThai
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String sqlUpdateXe = "UPDATE Xe SET soKmHienTai = ? WHERE bienSo = ? AND soKmHienTai < ?";

        return ConnectDB.inTransaction(c -> {
            String maPhieu = phieu.getMaPhieu();
            if (maPhieu == null || maPhieu.isBlank()) {
                maPhieu = IdGenerator.next(c, IdGenerator.Kind.PHIEU_TIEP_NHAN_SUA_CHUA);
                phieu.setMaPhieuRaw(maPhieu);
            }

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, maPhieu);
                ps.setString(2, phieu.getXe().getBienSo());
                ps.setString(3, phieu.getNhanVienTiepNhan().getMaNV());
                if (phieu.getNhanVienKyThuat() != null && phieu.getNhanVienKyThuat().getMaNV() != null) {
                    ps.setString(4, phieu.getNhanVienKyThuat().getMaNV());
                } else {
                    ps.setNull(4, Types.VARCHAR);
                }
                ps.setTimestamp(5, phieu.getNgayGioTiepNhan() != null
                        ? Timestamp.valueOf(phieu.getNgayGioTiepNhan())
                        : Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(6, phieu.getYeuCauBanDau());
                ps.setString(7, phieu.getTinhTrangXe());
                ps.setFloat(8, phieu.getSoKmTiepNhan());
                ps.setFloat(9, phieu.getMucNhienLieu());
                ps.setTimestamp(10, phieu.getThoiGianHenTra() != null ? Timestamp.valueOf(phieu.getThoiGianHenTra()) : null);
                ps.setTimestamp(11, phieu.getThoiGianBatDau() != null ? Timestamp.valueOf(phieu.getThoiGianBatDau()) : null);
                ps.setTimestamp(12, phieu.getThoiGianHoanThanh() != null ? Timestamp.valueOf(phieu.getThoiGianHoanThanh()) : null);
                ps.setString(13, phieu.getGhiChuKyThuat());
                ps.setString(14, phieu.getTrangThai() != null ? phieu.getTrangThai().name() : TrangThaiPhieu.CHO_SUA.name());
                ps.executeUpdate();
            }

            // Lưu các dịch vụ dự kiến ban đầu nếu có
            if (phieu.getChiTietDichVu() != null && !phieu.getChiTietDichVu().isEmpty()) {
                String sqlDichVu = "INSERT INTO ChiTietDichVu(maPhieu, maDichVu, soLuong, donGiaThucHien, ghiChu) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement psDv = c.prepareStatement(sqlDichVu)) {
                    for (ChiTietDichVu ct : phieu.getChiTietDichVu()) {
                        psDv.setString(1, maPhieu);
                        psDv.setString(2, ct.getDichVu().getMaDichVu());
                        psDv.setInt(3, ct.getSoLuong());
                        psDv.setDouble(4, ct.getDonGiaThucHien());
                        psDv.setString(5, ct.getGhiChu());
                        psDv.addBatch();
                    }
                    psDv.executeBatch();
                }
            }

            // Đồng bộ ODO xe
            if (phieu.getXe() != null && phieu.getSoKmTiepNhan() > 0) {
                try (PreparedStatement psXe = c.prepareStatement(sqlUpdateXe)) {
                    psXe.setFloat(1, phieu.getSoKmTiepNhan());
                    psXe.setString(2, phieu.getXe().getBienSo());
                    psXe.setFloat(3, phieu.getSoKmTiepNhan());
                    psXe.executeUpdate();
                }
            }

            return maPhieu;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CẬP NHẬT TRẠNG THÁI PHIẾU (Kiểm tra chỉ cập nhật khi chưa HOAN_TAT)
    // ─────────────────────────────────────────────────────────────────────────
    public boolean updateTrangThai(String maPhieu, TrangThaiPhieu trangThaiMoi, String maKTV) throws SQLException {
        // Kiểm tra trạng thái hiện tại trong database
        kiemTraChuaHoanTat(maPhieu);

        StringBuilder sql = new StringBuilder("UPDATE PhieuTiepNhanSuaChua SET trangThai = ? ");
        if (maKTV != null && !maKTV.isBlank()) {
            sql.append(", maNVKyThuat = COALESCE(maNVKyThuat, ?) ");
        }
        if (trangThaiMoi == TrangThaiPhieu.DANG_SUA) {
            sql.append(", thoiGianBatDau = COALESCE(thoiGianBatDau, SYSDATETIME()) ");
        } else if (trangThaiMoi == TrangThaiPhieu.HOAN_TAT) {
            sql.append(", thoiGianHoanThanh = SYSDATETIME() ");
        }
        sql.append("WHERE maPhieu = ? AND trangThai <> 'HOAN_TAT'");

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, trangThaiMoi.name());
            if (maKTV != null && !maKTV.isBlank()) {
                ps.setString(idx++, maKTV.trim());
            }
            ps.setString(idx, maPhieu.trim());
            return ps.executeUpdate() > 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. THÊM / XÓA CHI TIẾT DỊCH VỤ (Kiểm tra chưa HOAN_TAT)
    // ─────────────────────────────────────────────────────────────────────────
    public boolean themChiTietDichVu(String maPhieu, String maDichVu, int soLuong, double donGia, String ghiChu) throws SQLException {
        kiemTraChuaHoanTat(maPhieu);

        String sql = """
                MERGE ChiTietDichVu AS target
                USING (SELECT ? AS maPhieu, ? AS maDichVu) AS source
                ON (target.maPhieu = source.maPhieu AND target.maDichVu = source.maDichVu)
                WHEN MATCHED THEN
                    UPDATE SET soLuong = target.soLuong + ?, donGiaThucHien = ?, ghiChu = ?
                WHEN NOT MATCHED THEN
                    INSERT (maPhieu, maDichVu, soLuong, donGiaThucHien, ghiChu)
                    VALUES (?, ?, ?, ?, ?);
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maDichVu);
            ps.setInt(3, soLuong);
            ps.setDouble(4, donGia);
            ps.setString(5, ghiChu);
            ps.setString(6, maPhieu);
            ps.setString(7, maDichVu);
            ps.setInt(8, soLuong);
            ps.setDouble(9, donGia);
            ps.setString(10, ghiChu);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean xoaChiTietDichVu(String maPhieu, String maDichVu) throws SQLException {
        kiemTraChuaHoanTat(maPhieu);

        String sql = "DELETE FROM ChiTietDichVu WHERE maPhieu = ? AND maDichVu = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maDichVu);
            return ps.executeUpdate() > 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. THÊM / XÓA CHI TIẾT LINH KIỆN (Kiểm tra chưa HOAN_TAT)
    // ─────────────────────────────────────────────────────────────────────────
    public boolean themChiTietLinhKien(String maPhieu, String maPhuTung, int soLuong, double donGia, String ghiChu) throws SQLException {
        kiemTraChuaHoanTat(maPhieu);

        String sql = """
                MERGE ChiTietLinhKien AS target
                USING (SELECT ? AS maPhieu, ? AS maPhuTung) AS source
                ON (target.maPhieu = source.maPhieu AND target.maPhuTung = source.maPhuTung)
                WHEN MATCHED THEN
                    UPDATE SET soLuong = target.soLuong + ?, donGiaXuat = ?, ghiChu = ?
                WHEN NOT MATCHED THEN
                    INSERT (maPhieu, maPhuTung, soLuong, donGiaXuat, ghiChu)
                    VALUES (?, ?, ?, ?, ?);
                """;
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maPhuTung);
            ps.setInt(3, soLuong);
            ps.setDouble(4, donGia);
            ps.setString(5, ghiChu);
            ps.setString(6, maPhieu);
            ps.setString(7, maPhuTung);
            ps.setInt(8, soLuong);
            ps.setDouble(9, donGia);
            ps.setString(10, ghiChu);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean xoaChiTietLinhKien(String maPhieu, String maPhuTung) throws SQLException {
        kiemTraChuaHoanTat(maPhieu);

        String sql = "DELETE FROM ChiTietLinhKien WHERE maPhieu = ? AND maPhuTung = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            ps.setString(2, maPhuTung);
            return ps.executeUpdate() > 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. TRUY VẤN DANH SÁCH CHO TIẾP NHẬN VIÊN VÀ KỸ THUẬT VIÊN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ danh sách phiếu cho Tiếp nhận viên (tìm kiếm & lọc đa tiêu chí).
     */
    public List<PhieuTiepNhanSuaChuaRow> layDanhSachChoTiepNhanVien(String tuKhoa, TrangThaiPhieu trangThai,
                                                                   LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (trangThai != null) {
            sql.append(" AND p.trangThai = ? ");
            params.add(trangThai.name());
        }
        if (tuNgay != null) {
            sql.append(" AND p.ngayGioTiepNhan >= ? ");
            params.add(Timestamp.valueOf(tuNgay.atStartOfDay()));
        }
        if (denNgay != null) {
            sql.append(" AND p.ngayGioTiepNhan <= ? ");
            params.add(Timestamp.valueOf(denNgay.atTime(23, 59, 59)));
        }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            String pat = "%" + tuKhoa.trim() + "%";
            sql.append(" AND (p.maPhieu LIKE ? OR p.bienSo LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?) ");
            for (int i = 0; i < 4; i++) params.add(pat);
        }

        sql.append(" ORDER BY p.ngayGioTiepNhan DESC, p.maPhieu DESC");
        return queryList(sql.toString(), params);
    }

    /**
     * Lấy danh sách phiếu được phân công theo mã Kỹ thuật viên (kèm các phiếu CHO_SUA chưa có người nhận).
     */
    public List<PhieuTiepNhanSuaChuaRow> layDanhSachTheoKTV(String maNVKyThuat, String tuKhoa, TrangThaiPhieu trangThai,
                                                           LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (maNVKyThuat != null && !maNVKyThuat.isBlank()) {
            sql.append(" AND (p.maNVKyThuat = ? OR (p.maNVKyThuat IS NULL AND p.trangThai = 'CHO_SUA')) ");
            params.add(maNVKyThuat.trim());
        }
        if (trangThai != null) {
            sql.append(" AND p.trangThai = ? ");
            params.add(trangThai.name());
        }
        if (tuNgay != null) {
            sql.append(" AND p.ngayGioTiepNhan >= ? ");
            params.add(Timestamp.valueOf(tuNgay.atStartOfDay()));
        }
        if (denNgay != null) {
            sql.append(" AND p.ngayGioTiepNhan <= ? ");
            params.add(Timestamp.valueOf(denNgay.atTime(23, 59, 59)));
        }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            String pat = "%" + tuKhoa.trim() + "%";
            sql.append(" AND (p.maPhieu LIKE ? OR p.bienSo LIKE ? OR kh.hoTen LIKE ? OR kh.soDienThoai LIKE ?) ");
            for (int i = 0; i < 4; i++) params.add(pat);
        }

        sql.append(" ORDER BY p.ngayGioTiepNhan DESC, p.maPhieu DESC");
        return queryList(sql.toString(), params);
    }

    /**
     * Lấy danh sách các phiếu HOAN_TAT đủ điều kiện lập hóa đơn (chưa có hóa đơn hoặc chưa thanh toán).
     */
    public List<PhieuTiepNhanSuaChuaRow> layDanhSachDuDieuKienLapHoaDon() throws SQLException {
        String sql = SELECT_BASE + """
                 WHERE p.trangThai = 'HOAN_TAT'
                   AND NOT EXISTS (
                       SELECT 1 FROM HoaDon hd 
                       WHERE hd.maPhieu = p.maPhieu AND hd.trangThai = N'Đã thanh toán'
                   )
                 ORDER BY p.ngayGioTiepNhan DESC
                """;
        return queryList(sql, List.of());
    }

    /**
     * Lấy đầy đủ thông tin một phiếu sửa chữa (kèm 2 danh sách chi tiết dịch vụ và linh kiện).
     */
    public PhieuTiepNhanSuaChua findById(String maPhieu) throws SQLException {
        String sqlPhieu = SELECT_BASE + " WHERE p.maPhieu = ?";
        PhieuTiepNhanSuaChua phieu = null;

        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sqlPhieu)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    phieu = new PhieuTiepNhanSuaChua();
                    phieu.setMaPhieuRaw(rs.getString("maPhieu"));

                    Xe x = new Xe();
                    x.setBienSo(rs.getString("bienSo"));
                    x.setHangXe(rs.getString("hangXe"));
                    x.setModel(rs.getString("model"));
                    x.setMauXe(rs.getString("mauXe"));
                    x.setNamSanXuat(rs.getInt("namSanXuat"));

                    KhachHang kh = new KhachHang();
                    kh.setMaKH(rs.getString("maKH"));
                    kh.setHoTen(rs.getString("tenKH"));
                    kh.setSoDienThoai(rs.getString("sdtKH"));
                    x.setKhachHang(kh);
                    phieu.setXeRaw(x);

                    NhanVien tnv = new NhanVien();
                    tnv.setMaNV(rs.getString("maNVTiepNhan"));
                    tnv.setHoTen(rs.getString("tenNVTiepNhan"));
                    phieu.setNhanVienTiepNhanRaw(tnv);

                    String maKTV = rs.getString("maNVKyThuat");
                    if (maKTV != null) {
                        NhanVien ktv = new NhanVien();
                        ktv.setMaNV(maKTV);
                        ktv.setHoTen(rs.getString("tenNVKyThuat"));
                        phieu.setNhanVienKyThuatRaw(ktv);
                    }

                    Timestamp tsNhan = rs.getTimestamp("ngayGioTiepNhan");
                    if (tsNhan != null) phieu.setNgayGioTiepNhanRaw(tsNhan.toLocalDateTime());
                    phieu.setYeuCauBanDauRaw(rs.getString("yeuCauBanDau"));
                    phieu.setTinhTrangXeRaw(rs.getString("tinhTrangXe"));
                    phieu.setSoKmTiepNhanRaw(rs.getFloat("soKmTiepNhan"));
                    phieu.setMucNhienLieuRaw(rs.getFloat("mucNhienLieu"));

                    Timestamp tsBd = rs.getTimestamp("thoiGianBatDau");
                    if (tsBd != null) phieu.setThoiGianBatDauRaw(tsBd.toLocalDateTime());
                    Timestamp tsHt = rs.getTimestamp("thoiGianHoanThanh");
                    if (tsHt != null) phieu.setThoiGianHoanThanhRaw(tsHt.toLocalDateTime());

                    phieu.setGhiChuKyThuatRaw(rs.getString("ghiChuKyThuat"));
                    phieu.setTrangThaiRaw(TrangThaiPhieu.fromDb(rs.getString("trangThai")));
                }
            }
        }

        if (phieu != null) {
            phieu.setChiTietDichVu(layChiTietDichVu(maPhieu));
            phieu.setChiTietLinhKien(layChiTietLinhKien(maPhieu));
        }

        return phieu;
    }

    public List<ChiTietDichVu> layChiTietDichVu(String maPhieu) throws SQLException {
        String sql = """
                SELECT cdv.maPhieu, cdv.maDichVu, cdv.soLuong, cdv.donGiaThucHien, cdv.ghiChu,
                       dv.tenDichVu, dv.thoiGianUocTinh
                FROM ChiTietDichVu cdv
                JOIN DichVu dv ON cdv.maDichVu = dv.maDichVu
                WHERE cdv.maPhieu = ?
                ORDER BY dv.tenDichVu
                """;
        List<ChiTietDichVu> list = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DichVu dv = new DichVu(rs.getString("maDichVu"));
                    dv.setTenDichVu(rs.getString("tenDichVu"));
                    dv.setThoiGianUocTinh(rs.getDouble("thoiGianUocTinh"));

                    ChiTietDichVu ct = new ChiTietDichVu(
                            null,
                            dv,
                            rs.getInt("soLuong"),
                            rs.getDouble("donGiaThucHien"),
                            rs.getString("ghiChu")
                    );
                    list.add(ct);
                }
            }
        }
        return list;
    }

    public List<ChiTietLinhKien> layChiTietLinhKien(String maPhieu) throws SQLException {
        String sql = """
                SELECT clk.maPhieu, clk.maPhuTung, clk.soLuong, clk.donGiaXuat, clk.ghiChu,
                       pt.tenPhuTung, pt.donViTinh, pt.soLuongTon
                FROM ChiTietLinhKien clk
                JOIN PhuTung pt ON clk.maPhuTung = pt.maPhuTung
                WHERE clk.maPhieu = ?
                ORDER BY pt.tenPhuTung
                """;
        List<ChiTietLinhKien> list = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PhuTung pt = new PhuTung();
                    pt.setMaPhuTung(rs.getString("maPhuTung"));
                    pt.setTenPhuTung(rs.getString("tenPhuTung"));
                    pt.setDonViTinh(rs.getString("donViTinh"));
                    pt.setSoLuongTon(rs.getInt("soLuongTon"));

                    ChiTietLinhKien ct = new ChiTietLinhKien(
                            null,
                            pt,
                            rs.getInt("soLuong"),
                            rs.getDouble("donGiaXuat"),
                            rs.getString("ghiChu")
                    );
                    list.add(ct);
                }
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HÀM TIỆN ÍCH NỘI BỘ
    // ─────────────────────────────────────────────────────────────────────────
    private void kiemTraChuaHoanTat(String maPhieu) throws SQLException {
        String sql = "SELECT trangThai FROM PhieuTiepNhanSuaChua WHERE maPhieu = ?";
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maPhieu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString(1);
                    if ("HOAN_TAT".equalsIgnoreCase(status)) {
                        throw new IllegalStateException("Phiếu sửa chữa " + maPhieu + " đã HOÀN TẤT - Bị khóa hoàn toàn để chống gian lận.");
                    }
                }
            }
        }
    }

    private List<PhieuTiepNhanSuaChuaRow> queryList(String sql, List<Object> params) throws SQLException {
        List<PhieuTiepNhanSuaChuaRow> list = new ArrayList<>();
        try (Connection c = ConnectDB.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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
}
