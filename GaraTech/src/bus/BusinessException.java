package bus;

/** Lỗi nghiệp vụ có thông điệp tiếng Việt hiển thị thẳng cho người dùng (khác với lỗi kết nối / SQL). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
