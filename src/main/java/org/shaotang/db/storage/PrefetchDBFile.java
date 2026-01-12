package org.shaotang.db.storage;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrefetchDBFile extends DBFile {
    // 预读线程池
    private final ExecutorService prefetchExecutor =
            Executors.newSingleThreadExecutor();

    // 预读缓存（可选的优化）
    private final Map<Integer, CompletableFuture<byte[]>> prefetchCache =
            new ConcurrentHashMap<>();

    public PrefetchDBFile(String filename) throws IOException {
        super(filename);
    }

    // 带预读的读取
    public CompletableFuture<byte[]> readPageWithPrefetch(int pageId, int prefetchCount) {
        CompletableFuture<byte[]> result = new CompletableFuture<>();

        // 1. 立即读取请求的页
        prefetchExecutor.submit(() -> {
            try {
                byte[] data = readPage(pageId);
                result.complete(data);

                // 2. 异步预读后续的页
                for (int i = 1; i <= prefetchCount; i++) {
                    int nextPageId = pageId + i;
                    prefetchCache.computeIfAbsent(nextPageId, pid -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return readPage(pid);
                        } catch (IOException e) {
                            return null;
                        }
                    }, prefetchExecutor));
                }
            } catch (IOException e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    // 检查预读缓存
    public Optional<byte[]> getFromPrefetchCache(int pageId) {
        CompletableFuture<byte[]> future = prefetchCache.get(pageId);
        if (future != null && future.isDone()) {
            try {
                return Optional.ofNullable(future.get());
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    // 当页被修改时
    public void markPageDirty(int pageId, byte[] dirtyData) {
        // 移除预读缓存中的脏页
//        prefetchCache.remove(pageId);

        // 或者更新为脏数据
        CompletableFuture<byte[]> future = CompletableFuture.completedFuture(dirtyData);
        prefetchCache.put(pageId, future);
    }
}