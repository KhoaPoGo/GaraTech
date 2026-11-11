package gui.panel;

import bus.DichVuService;
import dao.DichVuDao;
import entity.DichVu;
import gui.component.BasePanel;
import gui.component.Msg;
import gui.component.StatusCellRenderer;
import gui.component.UI;
import gui.dialog.FormDialog;
import util.Async;
import util.Fmt;
import util.RoundedPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Màn hình độc lập quản lý danh mục dịch vụ. */
public final class QuanLyDichVuPanel extends BasePanel {
    private final DichVuService service=new DichVuService();
    private final DefaultTableModel model=UI.readOnlyModel("Mã","Tên dịch vụ","Mô tả","Đơn giá công","Giờ ước tính","Cấp độ","Trạng thái");
    private final JTable table=UI.table(model);
    private final TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
    private final JTextField search=new JTextField(20);
    private List<DichVu> services=new ArrayList<>();

    public QuanLyDichVuPanel(){
        super("Danh mục","Quản lý dịch vụ","Thêm, cập nhật đơn giá hoặc ngừng cung cấp dịch vụ.");
        JButton add=UI.primaryButton("+ Thêm dịch vụ"),edit=UI.secondaryButton("Sửa"),toggle=UI.secondaryButton("Ngừng cung cấp"),refresh=UI.secondaryButton("Làm mới");
        add.addActionListener(e->form(null)); edit.addActionListener(e->{DichVu d=current();if(d!=null)form(d);});toggle.addActionListener(e->toggle());refresh.addActionListener(e->reload());
        actions.add(add);actions.add(edit);actions.add(toggle);actions.add(refresh);
        table.setRowSorter(sorter);table.setRowHeight(38);table.getColumnModel().getColumn(0).setPreferredWidth(80);table.getColumnModel().getColumn(1).setPreferredWidth(220);table.getColumnModel().getColumn(2).setPreferredWidth(280);table.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
        search.putClientProperty("JTextField.placeholderText","Tìm theo mã, tên hoặc cấp độ...");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){private void apply(){String q=search.getText().trim();sorter.setRowFilter(q.isEmpty()?null:RowFilter.regexFilter("(?iu)"+Pattern.quote(q)));}public void insertUpdate(javax.swing.event.DocumentEvent e){apply();}public void removeUpdate(javax.swing.event.DocumentEvent e){apply();}public void changedUpdate(javax.swing.event.DocumentEvent e){apply();}});
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));top.setOpaque(false);top.add(search);top.add(UI.muted("Tìm kiếm dịch vụ"));
        RoundedPanel card=new RoundedPanel();card.setLayout(new BorderLayout(0,10));card.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));card.add(top,BorderLayout.NORTH);card.add(UI.tableBlock(table),BorderLayout.CENTER);body.add(card,BorderLayout.NORTH);
    }
    @Override public void onShow(){reload();}
    private DichVu current(){int i=table.getSelectedRow();return i<0?null:services.get(table.convertRowIndexToModel(i));}
    private void reload(){Async.run(this,service::danhSach,list->{services=list;model.setRowCount(0);for(DichVu d:list)model.addRow(new Object[]{d.getMaDichVu(),d.getTenDichVu(),d.getMoTa()==null?"":d.getMoTa(),Fmt.money(Math.round(d.getTienCong())),d.getThoiGianUocTinh(),d.getCapDoBaoDuong()==null?"":d.getCapDoBaoDuong(),d.getTrangThai()});});}
    private void toggle(){DichVu d=current();if(d==null){Msg.info(this,"Chọn dịch vụ cần cập nhật.");return;}boolean stop=!DichVuDao.NGUNG.equals(d.getTrangThai());if(!Msg.confirm(this,(stop?"Ngừng cung cấp":"Mở lại")+" dịch vụ ‘"+d.getTenDichVu()+"’?"))return;Async.run(this,()->{service.ngungCungCap(d.getMaDichVu(),stop);return null;},x->reload());}
    private void form(DichVu old){
        JTextField name=new JTextField(old==null?"":old.getTenDichVu(),26),price=new JTextField(old==null?"":Long.toString(Math.round(old.getTienCong())),26),hours=new JTextField(old==null?"":Double.toString(old.getThoiGianUocTinh()),26);
        JTextArea desc=new JTextArea(old==null?"":old.getMoTa()==null?"":old.getMoTa(),3,26);desc.setLineWrap(true);desc.setWrapStyleWord(true);
        JComboBox<String> level=new JComboBox<>();level.addItem("(Không có)");DichVuService.CAP_DO.forEach(level::addItem);if(old!=null&&old.getCapDoBaoDuong()!=null)level.setSelectedItem(old.getCapDoBaoDuong());
        FormDialog.Form f=new FormDialog.Form().add("Tên dịch vụ *",name).add("Mô tả",new JScrollPane(desc)).add("Đơn giá công (₫) *",price).add("Giờ ước tính *",hours).add("Cấp độ bảo dưỡng",level);
        Long[] p={null};Double[] h={null};boolean ok=FormDialog.show(this,old==null?"Thêm dịch vụ":"Sửa dịch vụ "+old.getMaDichVu(),f,()->{try{String s=price.getText().trim().replace(".","").replace(",","");if(!s.matches("\\d{1,15}"))return "Đơn giá phải là số nguyên không âm.";p[0]=Long.parseLong(s);h[0]=Double.parseDouble(hours.getText().trim().replace(',','.'));}catch(NumberFormatException ex){return "Nhập đơn giá hợp lệ và thời gian dạng số (ví dụ 0,5).";}if(name.getText().trim().isEmpty())return "Vui lòng nhập tên dịch vụ.";if(h[0]<=0)return "Thời gian ước tính phải lớn hơn 0.";return null;});
        if(!ok)return;String cap=level.getSelectedIndex()==0?null:(String)level.getSelectedItem();
        Async.run(this,()->{if(old==null)return "Đã thêm dịch vụ "+service.them(name.getText(),desc.getText(),p[0],h[0],cap)+".";service.sua(old.getMaDichVu(),name.getText(),desc.getText(),p[0],h[0],cap);return "Đã cập nhật dịch vụ "+old.getMaDichVu()+".";},msg->{Msg.info(this,msg);reload();});
    }
}
