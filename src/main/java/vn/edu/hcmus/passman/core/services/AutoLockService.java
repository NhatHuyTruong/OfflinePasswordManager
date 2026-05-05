package vn.edu.hcmus.passman.core.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoLockService {

    private final VaultManager vaultManager;
    private final long timeoutMinutes; // Thời gian chờ (X phút)
    private Runnable onLockCallback;   // Hàm sẽ được gọi để báo cho UI biết đã khóa

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> lockTask;

    public AutoLockService(VaultManager vaultManager, long timeoutMinutes) {
        this.vaultManager = vaultManager;
        // Mặc định ví dụ là 5 phút, có thể cho user cấu hình sau
        this.timeoutMinutes = timeoutMinutes; 
    }

    /**
     * Cài đặt tín hiệu (Callback) để tầng UI cập nhật giao diện khi bị khóa
     */
    public void setOnLockCallback(Runnable onLockCallback) {
        this.onLockCallback = onLockCallback;
    }

    /**
     * Bắt đầu đếm ngược thời gian
     */
    public void start() {
        if (scheduler == null || scheduler.isShutdown()) {
            // Tạo một luồng ngầm duy nhất chuyên lo việc đếm giờ
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        scheduleLockTask();
    }

    /**
     * BẤT CỨ KHI NÀO có tương tác chuột/phím, hàm này sẽ được gọi để reset lại đồng hồ
     */
    public void resetActivity() {
        // Hủy bỏ lịch khóa cũ (nếu chưa chạy)
        if (lockTask != null && !lockTask.isDone()) {
            lockTask.cancel(false);
        }
        // Đặt lại lịch mới
        scheduleLockTask();
    }

    /**
     * Dừng hoàn toàn (dùng khi tắt ứng dụng)
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Cài đặt lịch trình khóa Vault
     */
    private void scheduleLockTask() {
        lockTask = scheduler.schedule(() -> {
            // 1. [BẢO MẬT] Gọi VaultManager xóa toàn bộ RAM ngay lập tức
            vaultManager.lockVault();
            System.out.println("[AutoLock] Hệ thống đã tự động khóa do không có tương tác.");

            // 2. [GIAO DIỆN] Gọi callback để UI tự động văng ra màn hình Login
            if (onLockCallback != null) {
                onLockCallback.run();
            }
        }, timeoutMinutes, TimeUnit.MINUTES);
    }
}