package bus;

/** Lỗi nghiệp vụ khi đăng nhập; getMessage() hiển thị thẳng cho người dùng. */
public class AuthException extends Exception {
    public AuthException(String message) { super(message); }
}
