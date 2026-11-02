package util;

import bus.BusinessException;
import gui.component.Msg;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Chạy việc nặng (truy vấn CSDL) ngoài luồng giao diện rồi trả kết quả về luồng giao diện.
 * Lỗi nghiệp vụ / lỗi SQL được hiện bằng hộp thoại, không cần try-catch ở từng màn hình.
 * <pre>
 * Async.run(this, () -> service.load(), rows -> fill(rows));
 * </pre>
 */
public final class Async {
    private Async() {}

    public static <T> void run(Component parent, Callable<T> work, Consumer<T> onSuccess) {
        run(parent, work, onSuccess, null);
    }

    public static <T> void run(Component parent, Callable<T> work, Consumer<T> onSuccess, Runnable onFail) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }

            @Override protected void done() {
                try {
                    T result = get();
                    if (onSuccess != null) onSuccess.accept(result);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    if (c instanceof BusinessException) Msg.error(parent, c.getMessage());
                    else if (c instanceof SQLException sql) Msg.error(parent, DbError.friendly(sql));
                    else Msg.error(parent, "Có lỗi không mong muốn: " + c);
                    if (onFail != null) onFail.run();
                }
            }
        }.execute();
    }
}
