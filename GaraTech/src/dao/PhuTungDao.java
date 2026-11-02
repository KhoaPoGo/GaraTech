package dao;

import connectDB.ConnectDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PhuTungDao {

    public static class PhuTungRow {
        public String maPhuTung;
        public String tenPhuTung;
        public String donViTinh;
        public int soLuongTon;
        public int dinhMucToiThieu;
        public double giaNhap;
        public double giaBan;

        public String getTinhTrang() {
            if (soLuongTon <= 0) return "Hết hàng";
            if (soLuongTon <= dinhMucToiThieu) return "Sắp hết";
            return "Còn hàng";
        }
    }

    public static class LichSuKhoRow {
        public Timestamp thoiGian;
        public String loaiGiaoDich;
        public String maPhieu;
        public int soLuong;
        public int tonTruoc;
        public int tonSau;
        public String nguoiThucHien;
        public String ghiChu;
    }

    public List<PhuTungRow> findAll(String keyword) {
        List<PhuTungRow> list = new ArrayList<>();
        String sql = "SELECT maPhuTung, tenPhuTung, donViTinh, soLuongTon, "
                   + "ISNULL(dinhMucToiThieu, 5) AS dinhMucToiThieu, giaNhap, giaBan "
                   + "FROM PhuTung "
                   + "WHERE (? = '' OR maPhuTung LIKE ? OR tenPhuTung LIKE ?) "
                   + "ORDER BY maPhuTung";
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = keyword == null ? "" : keyword.trim();
            ps.setString(1, kw);
            ps.setString(2, "%" + kw + "%");
            ps.setString(3, "%" + kw + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PhuTungRow r = new PhuTungRow();
                    r.maPhuTung = rs.getString("maPhuTung");
                    r.tenPhuTung = rs.getString("tenPhuTung");
                    r.donViTinh = rs.getString("donViTinh");
                    r.soLuongTon = rs.getInt("soLuongTon");
                    r.dinhMucToiThieu = rs.getInt("dinhMucToiThieu");
                    r.giaNhap = rs.getDouble("giaNhap");
                    r.giaBan = rs.getDouble("giaBan");
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<LichSuKhoRow> findLichSuByMaPhuTung(String maPhuTung) {
        List<LichSuKhoRow> list = new ArrayList<>();
        String sql = "SELECT pn.ngayNhap AS thoiGian, N'Nhập kho' AS loai, pn.maPhieuNhap AS maPhieu, "
                   + "ct.soLuong, ct.tonTruoc, ct.tonSau, nv.hoTen AS nguoiThucHien, ncc.tenNCC AS ghiChu "
                   + "FROM ChiTietNhapKho ct "
                   + "JOIN PhieuNhapKho pn ON ct.maPhieuNhap = pn.maPhieuNhap "
                   + "LEFT JOIN NhanVien nv ON pn.maNV = nv.maNV "
                   + "LEFT JOIN NhaCungCap ncc ON pn.maNCC = ncc.maNCC "
                   + "WHERE ct.maPhuTung = ? "
                   + "UNION ALL "
                   + "SELECT px.ngayXuat AS thoiGian, N'Xuất kho' AS loai, px.maPhieuXuat AS maPhieu, "
                   + "ctx.soLuong, ctx.tonTruoc, ctx.tonSau, nv.hoTen AS nguoiThucHien, "
                   + "CONCAT(N'Phiếu SC: ', px.maPhieuSC) AS ghiChu "
                   + "FROM ChiTietXuatKho ctx "
                   + "JOIN PhieuXuatKho px ON ctx.maPhieuXuat = px.maPhieuXuat "
                   + "LEFT JOIN NhanVien nv ON px.maNVKho = nv.maNV "
                   + "WHERE ctx.maPhuTung = ? AND px.trangThai = N'DA_XUAT' "
                   + "ORDER BY thoiGian DESC";
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maPhuTung);
            ps.setString(2, maPhuTung);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LichSuKhoRow r = new LichSuKhoRow();
                    r.thoiGian = rs.getTimestamp("thoiGian");
                    r.loaiGiaoDich = rs.getString("loai");
                    r.maPhieu = rs.getString("maPhieu");
                    r.soLuong = rs.getInt("soLuong");
                    r.tonTruoc = rs.getInt("tonTruoc");
                    r.tonSau = rs.getInt("tonSau");
                    r.nguoiThucHien = rs.getString("nguoiThucHien");
                    r.ghiChu = rs.getString("ghiChu");
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean saveOrUpdate(PhuTungRow pt, boolean isNew) throws SQLException {
        String sql = isNew
            ? "INSERT INTO PhuTung(tenPhuTung, donViTinh, dinhMucToiThieu, giaNhap, giaBan, soLuongTon, maPhuTung) VALUES(?,?,?,?,?,0,?)"
            : "UPDATE PhuTung SET tenPhuTung=?, donViTinh=?, dinhMucToiThieu=?, giaNhap=?, giaBan=? WHERE maPhuTung=?";
        try (Connection conn = ConnectDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pt.tenPhuTung);
            ps.setString(2, pt.donViTinh);
            ps.setInt(3, pt.dinhMucToiThieu);
            ps.setDouble(4, pt.giaNhap);
            ps.setDouble(5, pt.giaBan);
            ps.setString(6, pt.maPhuTung);
            return ps.executeUpdate() > 0;
        }
    }
}