package gui.panel;

import bus.KhuyenMaiService;
import entity.KhuyenMai;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.Theme;
import gui.component.UI;
import gui.dialog.FormDialog;
import util.Async;
import util.RoundedPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Quản lý chương trình khuyến mãi theo bảng KhuyenMai. */
public final class KhuyenMaiPanel extends BasePanel {
    private final KhuyenMaiService service=new KhuyenMaiService();
    private final DefaultTableModel model=UI.readOnlyModel("Mã","Chương trình","Giảm (%)","Ngày bắt đầu","Ngày kết thúc","Người tạo","Ghi chú");
    private final JTable table=UI.table(model);
    private List<KhuyenMai> rows=new ArrayList<>();
    public KhuyenMaiPanel(){
        super("Danh mục","Quản lý khuyến mãi","Quản lý tỷ lệ giảm và thời hạn theo dữ liệu chương trình khuyến mãi.");
        JButton add=UI.primaryButton("+ Thêm khuyến mãi"), edit=UI.secondaryButton("Sửa"), delete=UI.secondaryButton("Xóa"), refresh=UI.secondaryButton("Làm mới");
        add.addActionListener(e->form(null));edit.addActionListener(e->{KhuyenMai k=current();if(k!=null)form(k);});delete.addActionListener(e->remove());refresh.addActionListener(e->reload());
        actions.add(add);actions.add(edit);actions.add(delete);actions.add(refresh);
        table.setRowHeight(38);table.getColumnModel().getColumn(0).setPreferredWidth(120);table.getColumnModel().getColumn(1).setPreferredWidth(230);table.getColumnModel().getColumn(6).setPreferredWidth(260);
        RoundedPanel card=new RoundedPanel();card.setLayout(new BorderLayout());card.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));card.add(UI.tableBlock(table),BorderLayout.CENTER);body.add(card,BorderLayout.NORTH);
    }
    @Override public void onShow(){reload();}
    private KhuyenMai current(){int i=table.getSelectedRow();return i<0?null:rows.get(table.convertRowIndexToModel(i));}
    private void reload(){Async.run(this,service::danhSach,list->{rows=list;model.setRowCount(0);for(KhuyenMai k:list)model.addRow(new Object[]{k.getMaKM(),k.getTenChuongTrinh(),k.getMucGiamPhanTram(),k.getNgayBatDau(),k.getNgayKetThuc(),k.getMaNVTao()==null?"—":k.getMaNVTao(),k.getGhiChu()==null?"":k.getGhiChu()});});}
    private void form(KhuyenMai old){
        JTextField name=new JTextField(old==null?"":old.getTenChuongTrinh(),28),pct=new JTextField(old==null?"":Double.toString(old.getMucGiamPhanTram()),28),from=new JTextField(old==null?"":old.getNgayBatDau().toString(),28),to=new JTextField(old==null?"":old.getNgayKetThuc().toString(),28),note=new JTextField(old==null?"":old.getGhiChu(),28);
        FormDialog.Form f=new FormDialog.Form().add("Tên chương trình *",name).add("Mức giảm (%) *",pct).add("Bắt đầu (yyyy-MM-dd) *",from).add("Kết thúc (yyyy-MM-dd) *",to).add("Ghi chú",note);
        final double[] value={0};final LocalDate[] dates=new LocalDate[2];
        boolean ok=FormDialog.show(this,old==null?"Thêm khuyến mãi":"Sửa "+old.getMaKM(),f,()->{try{value[0]=Double.parseDouble(pct.getText().trim().replace(',','.'));dates[0]=LocalDate.parse(from.getText().trim());dates[1]=LocalDate.parse(to.getText().trim());}catch(NumberFormatException|DateTimeParseException ex){return "Mức giảm phải là số và ngày theo định dạng yyyy-MM-dd.";}if(value[0]<0||value[0]>100)return "Mức giảm phải từ 0 đến 100%.";if(dates[1].isBefore(dates[0]))return "Ngày kết thúc không được trước ngày bắt đầu.";if(name.getText().trim().isEmpty())return "Vui lòng nhập tên chương trình.";return null;});
        if(!ok)return;
        Async.run(this,()->{if(old==null)return "Đã tạo chương trình "+service.them(name.getText(),value[0],dates[0],dates[1],note.getText())+".";service.sua(old.getMaKM(),name.getText(),value[0],dates[0],dates[1],note.getText());return "Đã cập nhật "+old.getMaKM()+".";},msg->{Msg.info(this,msg);reload();});
    }
    private void remove(){KhuyenMai k=current();if(k==null){Msg.info(this,"Chọn chương trình cần xóa.");return;}if(!Msg.confirm(this,"Xóa chương trình "+k.getMaKM()+"?"))return;Async.run(this,()->{service.xoa(k.getMaKM());return null;},x->reload());}
}
