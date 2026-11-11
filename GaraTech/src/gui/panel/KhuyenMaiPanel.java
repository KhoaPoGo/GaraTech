package gui.panel;

import bus.KhuyenMaiService;
import entity.KhuyenMai;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.UI;
import gui.dialog.FormDialog;
import util.Async;
import util.RoundedPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
        JTextField name=new JTextField(old==null?"":old.getTenChuongTrinh(),28),pct=new JTextField(old==null?"":Double.toString(old.getMucGiamPhanTram()),28),note=new JTextField(old==null?"":old.getGhiChu(),28);
        LocalDate today=LocalDate.now();
        LocalDate[] dates={old==null?today:old.getNgayBatDau(),old==null?today.plusMonths(1):old.getNgayKetThuc()};
        JButton from=UI.secondaryButton("Chọn ngày"),to=UI.secondaryButton("Chọn ngày");
        updateDateButton(from,dates[0]);updateDateButton(to,dates[1]);
        from.addActionListener(e->chooseDate(from,dates,0));to.addActionListener(e->chooseDate(to,dates,1));
        FormDialog.Form f=new FormDialog.Form().add("Tên chương trình *",name).add("Mức giảm (%) *",pct).add("Ngày bắt đầu *",from).add("Ngày kết thúc *",to).add("Ghi chú",note);
        final double[] value={0};
        boolean ok=FormDialog.show(this,old==null?"Thêm khuyến mãi":"Sửa "+old.getMaKM(),f,()->{try{value[0]=Double.parseDouble(pct.getText().trim().replace(',','.'));}catch(NumberFormatException ex){return "Mức giảm phải là số từ 0 đến 100.";}if(value[0]<0||value[0]>100)return "Mức giảm phải từ 0 đến 100%.";if(dates[0]==null||dates[1]==null)return "Vui lòng chọn đủ ngày bắt đầu và ngày kết thúc.";if(dates[1].isBefore(dates[0]))return "Ngày kết thúc không được trước ngày bắt đầu.";if(name.getText().trim().isEmpty())return "Vui lòng nhập tên chương trình.";return null;});
        if(!ok)return;
        Async.run(this,()->{if(old==null)return "Đã tạo chương trình "+service.them(name.getText(),value[0],dates[0],dates[1],note.getText())+".";service.sua(old.getMaKM(),name.getText(),value[0],dates[0],dates[1],note.getText());return "Đã cập nhật "+old.getMaKM()+".";},msg->{Msg.info(this,msg);reload();});
    }
    private void chooseDate(JButton button,LocalDate[] dates,int index){
        LocalDate current=dates[index]==null?LocalDate.now():dates[index];
        java.util.Date initial=java.util.Date.from(current.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner spinner=new JSpinner(new SpinnerDateModel(initial,null,null,java.util.Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner,"dd/MM/yyyy"));
        JPanel content=new JPanel(new BorderLayout(8,0));content.add(new JLabel("Chọn ngày:"),BorderLayout.WEST);content.add(spinner,BorderLayout.CENTER);
        if(JOptionPane.showConfirmDialog(this,content,"Chọn ngày",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            java.util.Date selected=(java.util.Date)spinner.getValue();dates[index]=selected.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();updateDateButton(button,dates[index]);
        }
    }
    private static void updateDateButton(JButton button,LocalDate date){button.setText(date==null?"Chọn ngày":date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));}
    private void remove(){KhuyenMai k=current();if(k==null){Msg.info(this,"Chọn chương trình cần xóa.");return;}if(!Msg.confirm(this,"Xóa chương trình "+k.getMaKM()+"?"))return;Async.run(this,()->{service.xoa(k.getMaKM());return null;},x->reload());}
}
