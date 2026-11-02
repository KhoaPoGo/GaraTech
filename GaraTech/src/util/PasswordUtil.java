package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Băm / kiểm tra mật khẩu theo đúng định dạng đang lưu trong cột TaiKhoan.matKhau (sql/02_ThemDuLieu.sql):
 *
 *     {số vòng lặp}${salt Base64}${hash Base64}      ví dụ  120000$Z2FyYXRlY2gtc2VlZC0wMQ==$KTRpww...=
 *
 * Thuật toán: PBKDF2-HMAC-SHA256, khóa 256 bit, salt là byte giải mã từ Base64.
 *
 * Mỗi tài khoản có salt riêng nên KHÔNG so sánh mật khẩu bằng SQL (WHERE matKhau = ?).
 * Phải lấy chuỗi hash từ CSDL theo tenDangNhap rồi gọi {@link #verify}.
 * Khi tạo / đổi mật khẩu trong ứng dụng thì lưu kết quả của {@link #hash}.
 */
public final class PasswordUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int MAX_ITERATIONS = 10_000_000; // chặn dữ liệu hỏng làm treo máy
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /** Băm mật khẩu mới (salt ngẫu nhiên). Dùng khi tạo tài khoản / đổi mật khẩu. */
    public static String hash(String plain) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(plain, salt, ITERATIONS, KEY_BITS);
        return ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(key);
    }

    /** @return true nếu mật khẩu gõ vào khớp với chuỗi hash lưu trong CSDL. Chuỗi hash sai định dạng thì trả về false. */
    public static boolean verify(String plain, String stored) {
        if (plain == null || plain.isEmpty() || stored == null) return false;
        String[] p = stored.trim().split("\\$");
        if (p.length != 3) return false;
        try {
            int iterations = Integer.parseInt(p[0]);
            byte[] salt = Base64.getDecoder().decode(p[1]);
            byte[] expected = Base64.getDecoder().decode(p[2]);
            if (iterations < 1 || iterations > MAX_ITERATIONS || expected.length == 0) return false;
            byte[] actual = derive(plain, salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual); // so sánh thời gian không đổi
        } catch (IllegalArgumentException e) { // số vòng lặp hoặc Base64 sai định dạng
            return false;
        }
    }

    private static byte[] derive(String plain, byte[] salt, int iterations, int keyBits) {
        PBEKeySpec spec = new PBEKeySpec(plain.toCharArray(), salt, iterations, keyBits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không băm được mật khẩu", e);
        } finally {
            spec.clearPassword();
        }
    }
}
