package vn.edu.hcmus.passman.core.security;

import de.mkammerer.argon2.Argon2Advanced;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

public class KeyGenerator {

    private static final int ITERATIONS = 3;       // Số vòng lặp
    private static final int MEMORY_KB = 65536;    // 64MB RAM
    private static final int PARALLELISM = 1;      // Số luồng
    private static final int OUTPUT_LENGTH = 32;   // Chiều dài khóa 32 bytes (AES-256)

    public byte[] deriveKey(char[] masterPassword, byte[] salt) {
        // CẤU HÌNH Ở ĐÂY: Truyền OUTPUT_LENGTH vào lúc khởi tạo
        // Tham số thứ 2 (16) là độ dài mặc định của Salt do thư viện yêu cầu khai báo
        Argon2Advanced argon2 = Argon2Factory.createAdvanced(Argon2Types.ARGON2id, 16, OUTPUT_LENGTH);

        try {
            // Hàm rawHash bây giờ chỉ truyền đúng 5 tham số như VS Code yêu cầu
            return argon2.rawHash(
                    ITERATIONS,
                    MEMORY_KB,
                    PARALLELISM,
                    masterPassword,
                    salt
            );
        } finally {
            // Dọn dẹp RAM
            argon2.wipeArray(masterPassword);
            MemoryWiper.clear(masterPassword);
        }
    }
}