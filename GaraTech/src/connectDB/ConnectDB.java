package connectDB;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Kết nối SQL Server qua JDBC.
 * Cấu hình đọc từ db.properties (trong resources) và được ghi đè bởi db.local.properties
 * (đặt cạnh pom.xml, không commit) nên mỗi người dùng mật khẩu SQL Server riêng của mình.
 */
public final class ConnectDB {
    private static final Properties PROPS = load();

    private ConnectDB() {}

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = ConnectDB.class.getResourceAsStream("/db.properties")) {
            if (in != null) p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
        Path local = Path.of("db.local.properties");
        if (Files.exists(local)) {
            try (Reader r = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException ignored) { }
        }
        return p;
    }

    /** Mở một kết nối mới. Luôn dùng try-with-resources để đóng. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password", ""));
    }

    /** true: khi chưa có CSDL vẫn cho đăng nhập bằng tài khoản demo (để làm giao diện). */
    public static boolean demoMode() {
        return Boolean.parseBoolean(PROPS.getProperty("app.demoMode", "false"));
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection c) throws SQLException;
    }

    /**
     * Chạy nhiều câu lệnh trong MỘT transaction: thành công thì commit, lỗi thì rollback.
     * Bắt buộc dùng cho: xuất kho / nhập kho (ghi phiếu + chi tiết + cập nhật tồn), lập hóa đơn + chi tiết.
     *
     * <pre>
     * ConnectDB.inTransaction(c -> {
     *     // insert PhieuXuatKho, insert ChiTietXuatKho, update PhuTung.soLuongTon ...
     *     return null;
     * });
     * </pre>
     */
    public static <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
