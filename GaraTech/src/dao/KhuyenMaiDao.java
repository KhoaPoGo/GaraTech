package dao;

import connectDB.ConnectDB;
import entity.KhuyenMai;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** DAO khớp trực tiếp bảng KhuyenMai trong sql/01_TaoBang.sql. */
public class KhuyenMaiDao {
    private static final List<KhuyenMai> DEMO=new ArrayList<>();
    private static final String COLS="maKM,maNV,tenChuongTrinh,mucGiamPhanTram,ngayBatDau,ngayKetThuc,ghiChu";
    static {
        LocalDate today=LocalDate.now();
        DEMO.add(new KhuyenMai("KM-DEMO-1","Ưu đãi bảo dưỡng",10,today.minusDays(15),today.plusDays(30),"Dữ liệu minh họa"));
        DEMO.add(new KhuyenMai("KM-DEMO-2","Tri ân khách hàng",5,today.minusDays(5),today.plusDays(60),"Dữ liệu minh họa"));
    }
    private static KhuyenMai map(ResultSet rs)throws SQLException{
        KhuyenMai k=new KhuyenMai(rs.getString("maKM"),rs.getString("tenChuongTrinh"),rs.getDouble("mucGiamPhanTram"),rs.getDate("ngayBatDau").toLocalDate(),rs.getDate("ngayKetThuc").toLocalDate(),rs.getString("ghiChu"));
        k.setMaNVTao(rs.getString("maNV"));return k;
    }
    public List<KhuyenMai> layTatCa()throws SQLException{
        if(ConnectDB.demoMode())return new ArrayList<>(DEMO);
        List<KhuyenMai> out=new ArrayList<>();
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement("SELECT "+COLS+" FROM KhuyenMai ORDER BY ngayBatDau DESC,maKM");ResultSet rs=ps.executeQuery()){while(rs.next())out.add(map(rs));}
        return out;
    }
    public List<KhuyenMai> layDanhSachHieuLuc(){try{return layTatCa().stream().filter(k->k.conHieuLuc(LocalDate.now())).toList();}catch(SQLException e){throw new IllegalStateException("Không tải được chương trình khuyến mãi.",e);}}
    public KhuyenMai timTheoMa(String id){
        if(id==null||id.isBlank())return null;
        if(ConnectDB.demoMode())return DEMO.stream().filter(k->k.getMaKM().equalsIgnoreCase(id.trim())).findFirst().orElse(null);
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement("SELECT "+COLS+" FROM KhuyenMai WHERE maKM=?")){ps.setString(1,id.trim());try(ResultSet rs=ps.executeQuery()){return rs.next()?map(rs):null;}}catch(SQLException e){throw new IllegalStateException("Không tải được khuyến mãi.",e);}
    }
    public void insert(Connection c,KhuyenMai k)throws SQLException{
        String sql="INSERT INTO KhuyenMai(maKM,maNV,tenChuongTrinh,mucGiamPhanTram,ngayBatDau,ngayKetThuc,ghiChu) VALUES(?,?,?,?,?,?,?)";
        try(PreparedStatement ps=c.prepareStatement(sql)){bind(ps,k,true);ps.executeUpdate();}
    }
    public int update(KhuyenMai k)throws SQLException{
        String sql="UPDATE KhuyenMai SET tenChuongTrinh=?,mucGiamPhanTram=?,ngayBatDau=?,ngayKetThuc=?,ghiChu=? WHERE maKM=?";
        try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setNString(1,k.getTenChuongTrinh());ps.setDouble(2,k.getMucGiamPhanTram());ps.setDate(3,Date.valueOf(k.getNgayBatDau()));ps.setDate(4,Date.valueOf(k.getNgayKetThuc()));ps.setNString(5,k.getGhiChu());ps.setString(6,k.getMaKM());return ps.executeUpdate();}
    }
    public int delete(String id)throws SQLException{try(Connection c=ConnectDB.getConnection();PreparedStatement ps=c.prepareStatement("DELETE FROM KhuyenMai WHERE maKM=?")){ps.setString(1,id);return ps.executeUpdate();}}
    private static void bind(PreparedStatement ps,KhuyenMai k,boolean insert)throws SQLException{
        ps.setString(1,k.getMaKM());ps.setString(2,k.getMaNVTao());ps.setNString(3,k.getTenChuongTrinh());ps.setDouble(4,k.getMucGiamPhanTram());ps.setDate(5,Date.valueOf(k.getNgayBatDau()));ps.setDate(6,Date.valueOf(k.getNgayKetThuc()));ps.setNString(7,k.getGhiChu());
    }
    public void demoInsert(KhuyenMai k){DEMO.add(k);}
    public boolean demoUpdate(KhuyenMai k){for(int i=0;i<DEMO.size();i++)if(DEMO.get(i).getMaKM().equals(k.getMaKM())){DEMO.set(i,k);return true;}return false;}
    public boolean demoDelete(String id){return DEMO.removeIf(k->k.getMaKM().equals(id));}
}
