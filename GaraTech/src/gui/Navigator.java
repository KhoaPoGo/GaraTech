package gui;

import java.util.function.Consumer;

/** Cho phép màn hình bất kỳ chuyển sang màn hình khác: {@code Navigator.go(Screen.HOA_DON);} */
public final class Navigator {

    /** Yêu cầu mở màn hình Thêm khách hàng từ module Tiếp nhận xe */
    public record TiepNhanAddCustomerRequest(String initialQuery) {}

    private static Consumer<Screen> handler = s -> { };
    private static Object payload;

    private Navigator() {}

    static void setHandler(Consumer<Screen> h) { handler = h; }

    public static void go(Screen s) { handler.accept(s); }

    public static void setPayload(Object p) { payload = p; }

    public static Object consumePayload() {
        Object p = payload;
        payload = null;
        return p;
    }
}
