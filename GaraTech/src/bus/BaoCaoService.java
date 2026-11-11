package bus;

import dao.BaoCaoDao;
import dao.BaoCaoDao.CanhBaoKho;
import dao.BaoCaoDao.DongThongKe;
import dao.DieuPhoiDao;
import dao.DieuPhoiDao.PhieuCard;
import enums.TrangThaiPhieu;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Số liệu cho Dashboard và màn Báo cáo & thống kê của quản lý. */
public final class BaoCaoService {
    private final BaoCaoDao dao = new BaoCaoDao();
    private final DieuPhoiDao phieuDao = new DieuPhoiDao();

    /** Một cột của biểu đồ doanh thu. */
    public record Cot(String nhan, long giaTri) {}

    /** Số liệu báo cáo cho một khoảng ngày. */
    public record BaoCao(long thucThu, long conChoThu, long soHoaDon, long soPhieuHoanThanh,
                         List<Cot> bieuDo, String donViCot, List<DongThongKe> dichVu, List<DongThongKe> phuTung) {}

    public record DashboardData(long tiepNhanHomNay, long phieuChoXuLy, long dangSua, long quaHan,
                                long hoanThanhHomNay, long daBanGiaoHomNay, long choKiemTra,
                                long thuHomNay, long conChoThu, List<Cot> bayNgay, long tong7Ngay, long tong7NgayTruoc,
                                LocalDate tuNgay, LocalDate denNgay, CanhBaoKho khoCanhBao, List<PhieuCard> phieuDangHoatDong) {}

    public BaoCao baoCao(LocalDate from, LocalDate to) throws SQLException {
        if (from == null || to == null) throw new BusinessException("Vui lòng nhập đủ ngày bắt đầu và ngày kết thúc.");
        if (to.isBefore(from)) throw new BusinessException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        long ngay = ChronoUnit.DAYS.between(from, to) + 1;
        if (ngay > 366) throw new BusinessException("Chỉ xem được tối đa 366 ngày mỗi lần. Hãy chọn khoảng ngắn hơn.");
        Map<LocalDate, Long> theoNgay = dao.thucThuTheoNgay(from, to);
        return new BaoCao(dao.thucThu(from, to), dao.conChoThu(from, to), dao.soHoaDon(from, to),
                dao.soPhieuHoanThanh(from, to), gom(from, to, theoNgay),
                ngay <= 14 ? "ngày" : ngay <= 92 ? "tuần" : "tháng",
                dao.coCauDichVu(from, to), dao.nhomPhuTung(from, to));
    }

    /** Gom doanh thu theo ngày (<= 14 ngày), theo tuần (<= 92 ngày) hoặc theo tháng. */
    static List<Cot> gom(LocalDate from, LocalDate to, Map<LocalDate, Long> theoNgay) {
        long ngay = ChronoUnit.DAYS.between(from, to) + 1;
        List<Cot> out = new ArrayList<>();
        if (ngay <= 14) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1))
                out.add(new Cot(String.format("%02d/%02d", d.getDayOfMonth(), d.getMonthValue()), theoNgay.getOrDefault(d, 0L)));
        } else if (ngay <= 92) {
            for (LocalDate s = from; !s.isAfter(to); s = s.plusDays(7)) {
                LocalDate e = s.plusDays(6).isAfter(to) ? to : s.plusDays(6);
                long sum = 0;
                for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) sum += theoNgay.getOrDefault(d, 0L);
                out.add(new Cot(String.format("%02d/%02d", s.getDayOfMonth(), s.getMonthValue()), sum));
            }
        } else {
            for (LocalDate s = from.withDayOfMonth(1); !s.isAfter(to); s = s.plusMonths(1)) {
                long sum = 0;
                for (Map.Entry<LocalDate, Long> en : theoNgay.entrySet())
                    if (en.getKey().getYear() == s.getYear() && en.getKey().getMonth() == s.getMonth()) sum += en.getValue();
                out.add(new Cot(String.format("%02d/%d", s.getMonthValue(), s.getYear()), sum));
            }
        }
        return out;
    }

    public DashboardData dashboard() throws SQLException {
        LocalDate hom = LocalDate.now();
        LocalDate tu = hom.minusDays(6);
        Map<LocalDate, Long> ngay = dao.thucThuTheoNgay(tu, hom);
        List<Cot> bay = gom(tu, hom, ngay);
        long tong7 = bay.stream().mapToLong(Cot::giaTri).sum();
        long truoc = dao.thucThu(tu.minusDays(7), tu.minusDays(1));
        Map<String, Long> theoTT = dao.demPhieuTheoTrangThai();
        Map<String, Long> xongHom = dao.demHoanThanhTrongNgay(hom);
        return new DashboardData(dao.demTiepNhanTrongNgay(hom), dao.demPhieuTiepNhanChoXuLy(),
                theoTT.getOrDefault(TrangThaiPhieu.DANG_SUA.name(), 0L) + theoTT.getOrDefault(TrangThaiPhieu.CHO_LINH_KIEN.name(), 0L), dao.demPhieuQuaHan(LocalDateTime.now()),
                xongHom.getOrDefault(TrangThaiPhieu.HOAN_TAT.name(), 0L),
                0L,
                0L,
                ngay.getOrDefault(hom, 0L), dao.conChoThu(LocalDate.of(2000, 1, 1), hom),
                bay, tong7, truoc, tu, hom, dao.canhBaoTonKho(3),
                phieuDao.findByTrangThai(6, TrangThaiPhieu.CHO_SUA, TrangThaiPhieu.DANG_SUA,
                        TrangThaiPhieu.CHO_LINH_KIEN));
    }
}
