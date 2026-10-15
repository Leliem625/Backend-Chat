package com.example.demo.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionUtils {

    private TransactionUtils() {
    }

    // Đang trong transaction -> đợi lưu xong (commit) mới chạy, tránh bên nhận gọi API
    // khi dữ liệu chưa có trong DB; transaction rollback thì không chạy.
    // Không có transaction thì chạy luôn.
    public static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
