package gui.panel;

import bus.DieuPhoiService;
import dao.DieuPhoiDao.PhieuCard;
import enums.TrangThaiPhieu;
import gui.component.BasePanel;
import gui.component.StatusBadge;
import gui.component.Theme;
import gui.component.UI;
import util.Async;
import util.Fmt;
import util.RoundedPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Màn theo dõi tiến độ chỉ đọc cho quản lý. */
public final class DieuPhoiPanel extends BasePanel {
    private final DieuPhoiService service=new DieuPhoiService();
    private final JPanel board=new JPanel(new GridLayout(1,TrangThaiPhieu.values().length,12,0));
    private final JTextArea info=new JTextArea();
    private final JLabel heading=UI.h2("Chọn một phiếu để xem chi tiết");
    private List<PhieuCard> tickets=new ArrayList<>();
    private String selected;

    public DieuPhoiPanel(){
        super("Điều hành gara","Tiến độ sửa chữa","Theo dõi trạng thái các phiếu tiếp nhận-sửa chữa.");
        JButton refresh=UI.secondaryButton("Làm mới"); refresh.addActionListener(e->reload()); actions.add(refresh);
        board.setOpaque(false); board.setPreferredSize(new Dimension(1200,360));
        JPanel detail=new JPanel(new BorderLayout(0,10)); detail.setOpaque(false);
        detail.add(heading,BorderLayout.NORTH);
        info.setEditable(false); info.setLineWrap(true); info.setWrapStyleWord(true); info.setOpaque(false);
        info.setFont(Theme.font(Font.PLAIN,13)); info.setForeground(Theme.MUTED);
        RoundedPanel card=new RoundedPanel(); card.setLayout(new BorderLayout()); card.setBorder(BorderFactory.createEmptyBorder(16,18,16,18)); card.add(new JScrollPane(info),BorderLayout.CENTER);
        detail.add(card,BorderLayout.CENTER);
        JPanel content=new JPanel(new BorderLayout(0,18)); content.setOpaque(false); content.add(board,BorderLayout.NORTH); content.add(detail,BorderLayout.CENTER); body.add(content,BorderLayout.NORTH);
    }
    @Override public void onShow(){reload();}
    private void reload(){Async.run(this,service::phieuTrenBang,rows->{tickets=rows;buildBoard();showSelected();});}
    private void buildBoard(){
        board.removeAll();
        for(TrangThaiPhieu status:TrangThaiPhieu.values()){
            List<PhieuCard> rows=tickets.stream().filter(p->p.trangThai()==status).toList();
            RoundedPanel col=new RoundedPanel(12,new Color(0xEBEEF4)); col.setLayout(new BorderLayout(0,8)); col.setBorder(BorderFactory.createEmptyBorder(12,10,12,10));
            JPanel head=new JPanel(new BorderLayout()); head.setOpaque(false); head.add(new StatusBadge(status.label()),BorderLayout.WEST); head.add(UI.label(""+rows.size(),Font.BOLD,13,Theme.MUTED),BorderLayout.EAST); col.add(head,BorderLayout.NORTH);
            JPanel list=new JPanel(); list.setOpaque(false); list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
            if(rows.isEmpty())list.add(UI.muted("Không có phiếu"));
            for(PhieuCard p:rows){RoundedPanel item=new RoundedPanel(10,Color.WHITE);item.setLayout(new BoxLayout(item,BoxLayout.Y_AXIS));item.setBorder(BorderFactory.createEmptyBorder(9,10,9,10));item.add(UI.label(p.maPhieuSC(),Font.BOLD,12,Theme.TEXT));item.add(UI.muted(p.bienSo()+" · "+p.tenXe()));item.add(UI.muted(p.tenKhach()));item.add(UI.muted("KTV: "+(p.tenKTV()==null?"Chưa gán":p.tenKTV())));if(p.duKien()!=null)item.add(UI.muted("Hẹn trả: "+Fmt.dateTime(p.duKien())));item.setAlignmentX(Component.LEFT_ALIGNMENT);item.setMaximumSize(new Dimension(Integer.MAX_VALUE,item.getPreferredSize().height));item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));item.addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){selected=p.maPhieuSC();showSelected();}});list.add(item);list.add(Box.createVerticalStrut(8));}
            col.add(new JScrollPane(list),BorderLayout.CENTER); board.add(col);
        }
        board.revalidate();board.repaint();
    }
    private void showSelected(){
        PhieuCard p=tickets.stream().filter(x->x.maPhieuSC().equals(selected)).findFirst().orElse(null);
        if(p==null){heading.setText("Chọn một phiếu để xem chi tiết");info.setText("Thông tin được đọc từ PhieuTiepNhanSuaChua. Màn hình này không thay đổi trạng thái hay phân công kỹ thuật viên.");return;}
        heading.setText("Phiếu "+p.maPhieuSC()+" · "+p.trangThai().label());
        info.setText("Khách hàng: "+p.tenKhach()+"\nXe: "+p.bienSo()+" · "+p.tenXe()+"\nKỹ thuật viên: "+(p.tenKTV()==null?"Chưa gán":p.tenKTV())+"\nYêu cầu: "+(p.yeuCau()==null?"":p.yeuCau())+"\nNgày bắt đầu: "+(p.batDau()==null?"Chưa bắt đầu":Fmt.dateTime(p.batDau()))+"\nHẹn trả: "+(p.duKien()==null?"Chưa có":Fmt.dateTime(p.duKien()))+"\nTiến độ: "+p.tienDo()+"%\n\nSQL hiện lưu trạng thái phiếu và thông tin bắt đầu/hẹn trả; chưa lưu tiến độ từng hạng mục.");
    }

    // ======================================================================
    // XỬ LÝ SỰ KIỆN Ở CUỐI CÙNG THEO QUY TẮC BẮT BUỘC
    // ======================================================================
    private void initEvents() {
        btnRefresh.addActionListener(e -> reload());
        btnLuu.addActionListener(e -> luuPhanCong());
        btnBatDau.addActionListener(e -> batDau());
        btnHuy.addActionListener(e -> huy());
        btnKiemTra.addActionListener(e -> Navigator.go(Screen.KIEM_TRA_CL));
    }
}
