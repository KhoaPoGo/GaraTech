package gui;

import java.util.function.Consumer;

/** Cho phép màn hình bất kỳ chuyển sang màn hình khác: {@code Navigator.go(Screen.HOA_DON);} */
public final class Navigator {
    private static Consumer<Screen> handler = s -> { };

    private Navigator() {}

    static void setHandler(Consumer<Screen> h) { handler = h; }

    public static void go(Screen s) { handler.accept(s); }
}
