package bus;

import dao.DichVuDao;
import dao.HangMucSuaChuaDao;
import dao.PhieuSuaChuaDao;
import dao.PhieuTiepNhanDao;
import dao.PhuTungDao;
import entity.DichVu;
import entity.HangMucSuaChua;
import entity.PhieuSuaChua;
import entity.PhuTung;
import enums.TrangThaiHangMuc;
import enums.TrangThaiPhieuSuaChua;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PhieuSuaChuaBus {

    private final PhieuSuaChuaDao phieuSuaChuaDao = new PhieuSuaChuaDao();
    private final HangMucSuaChuaDao hangMucDao = new HangMucSuaChuaDao();
    private final DichVuDao dichVuDao = new DichVuDao();
    private final PhuTungDao phuTungDao = new PhuTungDao();

    public record ChiPhiSummary(
            double tienDichVu,
            double tienPhuTung,
            double tongGiamGia,
            double tongThanhTien
    ) {
    }

    /**
     * Tính toán tổng hợp chi phí từ danh sách các hạng mục hiện có.
     */
    public static ChiPhiSummary tinhChiPhi(List<HangMucSuaChua> hangMucs) {
        double tienDv = 0;
        double tienPt = 0;
        double giamGia = 0;

        if (hangMucs != null) {
            for (HangMucSuaChua hm : hangMucs) {
                if (hm.getTrangThai() != null && hm.getTrangThai().equalsIgnoreCase("Từ chối")) {
                    continue;
                }
                double tt = (hm.getSoLuong() * hm.getDonGia()) - hm.getMucGiamGia();
                if (tt < 0) tt = 0;

                if ("DichVu".equalsIgnoreCase(hm.getLoaiHangMuc())) {
                    tienDv += (hm.getSoLuong() * hm.getDonGia());
                } else {
                    tienPt += (hm.getSoLuong() * hm.getDonGia());
                }
                giamGia += hm.getMucGiamGia();
            }
        }
        double tong = Math.max(0, (tienDv + tienPt) - giamGia);
        return new ChiPhiSummary(tienDv, tienPt, giamGia, tong);
    }

    /**
     * Lập phiếu sửa chữa mới kèm các hạng mục công việc / phụ tùng.
     */
    public String lapPhieuSuaChua(PhieuSuaChua psc, List<HangMucSuaChua> hangMucs, boolean khachDuyetNgay) throws SQLException {
        if (psc.getMaPhieuTN() == null || psc.getMaPhieuTN().isBlank()) {
            throw new BusinessException("Vui lòng chọn phiếu tiếp nhận liên kết.");
        }
        if (psc.getMaNVLap() == null || psc.getMaNVLap().isBlank()) {
            throw new BusinessException("Không xác định được nhân viên lập phiếu.");
        }
        if (hangMucs == null || hangMucs.isEmpty()) {
            throw new BusinessException("Phiếu sửa chữa phải có ít nhất 1 hạng mục dịch vụ hoặc phụ tùng.");
        }

        // Kiểm tra từng hạng mục
        for (int i = 0; i < hangMucs.size(); i++) {
            HangMucSuaChua hm = hangMucs.get(i);
            if (hm.getTenHangMuc() == null || hm.getTenHangMuc().isBlank()) {
                throw new BusinessException("Tên hạng mục ở dòng " + (i + 1) + " không được để trống.");
            }
            if (hm.getSoLuong() <= 0) {
                throw new BusinessException("Số lượng hạng mục '" + hm.getTenHangMuc() + "' phải lớn hơn 0.");
            }
            if (hm.getDonGia() < 0) {
                throw new BusinessException("Đơn giá hạng mục '" + hm.getTenHangMuc() + "' không được âm.");
            }
            if (hm.getMucGiamGia() < 0 || hm.getMucGiamGia() > (hm.getSoLuong() * hm.getDonGia())) {
                throw new BusinessException("Giảm giá hạng mục '" + hm.getTenHangMuc() + "' không hợp lệ.");
            }
            hm.tinhThanhTien();

            // Gán trạng thái cho từng hạng mục theo trạng thái phiếu
            if (khachDuyetNgay) {
                hm.setTrangThai(TrangThaiHangMuc.DA_XAC_NHAN.label());
            } else {
                hm.setTrangThai(TrangThaiHangMuc.CHO_DUYET.label());
            }
        }

        if (khachDuyetNgay) {
            psc.setTrangThai(TrangThaiPhieuSuaChua.DA_XAC_NHAN.label());
        } else {
            psc.setTrangThai(TrangThaiPhieuSuaChua.CHO_KHACH_DUYET.label());
        }

        return phieuSuaChuaDao.insertWithItems(psc, hangMucs);
    }

    /**
     * Chuyển trạng thái phiếu sửa chữa theo quy tắc hợp lệ.
     */
    public void chuyenTrangThai(String maPhieuSC, TrangThaiPhieuSuaChua nextStatus, String lyDoHuy) throws SQLException {
        PhieuSuaChuaDao.PhieuSuaChuaRow row = phieuSuaChuaDao.findById(maPhieuSC);
        if (row == null) {
            throw new BusinessException("Không tìm thấy phiếu sửa chữa " + maPhieuSC);
        }

        TrangThaiPhieuSuaChua current = TrangThaiPhieuSuaChua.fromLabel(row.trangThai());
        if (current == null) {
            throw new BusinessException("Trạng thái hiện tại của phiếu không hợp lệ: " + row.trangThai());
        }

        if (!current.canMoveTo(nextStatus)) {
            throw new BusinessException("Không thể chuyển phiếu từ trạng thái '" + current.label() + "' sang '" + nextStatus.label() + "'.");
        }

        if (nextStatus == TrangThaiPhieuSuaChua.DA_HUY) {
            if (lyDoHuy == null || lyDoHuy.trim().length() < 3) {
                throw new BusinessException("Vui lòng nhập lý do hủy phiếu sửa chữa (ít nhất 3 ký tự).");
            }
        } else {
            lyDoHuy = null;
        }

        phieuSuaChuaDao.updateTrangThai(maPhieuSC, nextStatus.label(), lyDoHuy);

        // Cập nhật mốc thời gian tự động
        if (nextStatus == TrangThaiPhieuSuaChua.DANG_SUA_CHUA && row.thoiGianBatDau() == null) {
            phieuSuaChuaDao.updateThoiGian(maPhieuSC, LocalDateTime.now(), null);
        } else if (nextStatus == TrangThaiPhieuSuaChua.HOAN_THANH && row.thoiGianHoanThanh() == null) {
            phieuSuaChuaDao.updateThoiGian(maPhieuSC, null, LocalDateTime.now());
        }
    }

    /**
     * Thêm hạng mục phát sinh trong quá trình sửa chữa.
     */
    public String themHangMucPhatSinh(String maPhieuSC, HangMucSuaChua hm) throws SQLException {
        if (hm.getSoLuong() <= 0) {
            throw new BusinessException("Số lượng phải lớn hơn 0.");
        }
        if (hm.getDonGia() < 0) {
            throw new BusinessException("Đơn giá không được âm.");
        }
        hm.setMaPhieuSC(maPhieuSC);
        hm.setLaPhatSinh(true);
        hm.setTrangThai(TrangThaiHangMuc.CHO_DUYET.label());
        hm.tinhThanhTien();

        return hangMucDao.insertSingle(hm);
    }

    /**
     * Duyệt hoặc từ chối hạng mục phát sinh.
     */
    public void xuLyHangMucPhatSinh(String maHangMuc, boolean dongY) throws SQLException {
        String tt = dongY ? TrangThaiHangMuc.DA_XAC_NHAN.label() : TrangThaiHangMuc.TU_CHOI.label();
        hangMucDao.updateTrangThai(maHangMuc, tt);
    }

    public PhieuSuaChuaDao.PhieuSuaChuaRow timTheoMa(String maPhieuSC) throws SQLException {
        return phieuSuaChuaDao.findById(maPhieuSC);
    }

    public PhieuSuaChuaDao.PhieuSuaChuaRow timTheoMaPhieuTN(String maPhieuTN) throws SQLException {
        return phieuSuaChuaDao.findByMaPhieuTN(maPhieuTN);
    }

    public List<PhieuSuaChuaDao.PhieuSuaChuaRow> timKiem(LocalDate fromDate, LocalDate toDate, String keyword, String trangThai) throws SQLException {
        return phieuSuaChuaDao.search(fromDate, toDate, keyword, trangThai);
    }

    public List<HangMucSuaChua> layDanhSachHangMuc(String maPhieuSC) throws SQLException {
        return hangMucDao.findByPhieuSC(maPhieuSC);
    }

    public List<DichVu> layDanhSachDichVuHoatDong() throws SQLException {
        return dichVuDao.findActive();
    }

    public List<PhuTung> layDanhSachPhuTungKhaDung() throws SQLException {
        return phuTungDao.findAll();
    }

    public List<PhieuTiepNhanDao.PhieuTiepNhanRow> layDanhSachPhieuTiepNhanHopLe(String keyword) throws SQLException {
        return PhieuTiepNhanDao.findEligibleForRepair(keyword);
    }
}
