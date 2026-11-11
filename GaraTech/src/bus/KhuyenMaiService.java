package bus;

import connectDB.ConnectDB;
import dao.KhuyenMaiDao;
import entity.KhuyenMai;
import util.IdGenerator;
import util.Session;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Nghiệp vụ khuyến mãi theo đúng các cột của bảng KhuyenMai. */
public final class KhuyenMaiService {
    private final KhuyenMaiDao dao=new KhuyenMaiDao();
    public List<KhuyenMai> danhSach()throws SQLException{return dao.layTatCa();}
    public String them(String ten,double pct,LocalDate from,LocalDate to,String note)throws SQLException{
        KhuyenMai k=validate(null,ten,pct,from,to,note);
        k.setMaNVTao(Session.maNV());
        if(ConnectDB.demoMode()){int i=1;List<String> ids=dao.layTatCa().stream().map(KhuyenMai::getMaKM).toList();while(ids.contains("KM-DEMO-"+i))i++;k.setMaKM("KM-DEMO-"+i);dao.demoInsert(k);return k.getMaKM();}
        return ConnectDB.inTransaction(c->{k.setMaKM(IdGenerator.next(c,IdGenerator.Kind.KHUYEN_MAI));dao.insert(c,k);return k.getMaKM();});
    }
    public void sua(String id,String ten,double pct,LocalDate from,LocalDate to,String note)throws SQLException{
        KhuyenMai old=dao.timTheoMa(id);if(old==null)throw new BusinessException("Không tìm thấy chương trình khuyến mãi.");
        KhuyenMai k=validate(id,ten,pct,from,to,note);k.setMaNVTao(old.getMaNVTao());
        if(ConnectDB.demoMode()){if(!dao.demoUpdate(k))throw new BusinessException("Chương trình không còn tồn tại.");}
        else if(dao.update(k)==0)throw new BusinessException("Chương trình không còn tồn tại.");
    }
    public void xoa(String id)throws SQLException{
        if(ConnectDB.demoMode()){if(!dao.demoDelete(id))throw new BusinessException("Chương trình không còn tồn tại.");}
        else if(dao.delete(id)==0)throw new BusinessException("Chương trình không còn tồn tại.");
    }
    private static KhuyenMai validate(String id,String name,double pct,LocalDate from,LocalDate to,String note){
        name=name==null?"":name.trim();note=note==null?"":note.trim();
        if(name.isEmpty()||name.length()>150)throw new BusinessException("Tên chương trình bắt buộc và tối đa 150 ký tự.");
        if(Double.isNaN(pct)||pct<0||pct>100)throw new BusinessException("Mức giảm phải từ 0 đến 100%.");
        if(from==null||to==null||to.isBefore(from))throw new BusinessException("Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.");
        if(note.length()>255)throw new BusinessException("Ghi chú tối đa 255 ký tự.");
        return new KhuyenMai(id,name,pct,from,to,note.isEmpty()?null:note);
    }
}
