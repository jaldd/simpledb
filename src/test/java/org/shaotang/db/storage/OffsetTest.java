package org.shaotang.db.storage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OffsetTest {
    public static void main(String[] args) throws IOException {
        String filename = "offset_test.db";
        boolean delete = new File(filename).delete();
        System.out.println("删除文件：" + delete);

        System.out.println("=== Day 5: 页文件结构测试 ===\n");

        DBFile dbFile = new DBFile(filename);

        // 1. 写入页0
        System.out.println("1. 写入页0:");
        byte[] page0Data = createPageData(0);
        dbFile.writePage(0, page0Data);
        System.out.println("   页0偏移: " + dbFile.getPageOffset(0) + " 字节");

        // 2. 写入页100（跳过1-99页）
        System.out.println("\n2. 写入页100（创建空洞）:");
        byte[] page100Data = createPageData(100);
        dbFile.writePage(100, page100Data);
        System.out.println("   页100偏移: " + dbFile.getPageOffset(100) + " 字节");

        // 3. 验证文件大小
        long fileSize = dbFile.getLogicalFileSize();
        long expectedMinSize = dbFile.getPageOffset(100) + DBFile.PAGE_SIZE;
        System.out.println("\n3. 验证文件大小:");
        System.out.println("   实际文件大小: " + fileSize + " 字节");
        System.out.println("   最小期望大小: " + expectedMinSize + " 字节");
        System.out.println("   是否满足: " + (fileSize >= expectedMinSize ? "✅" : "❌"));

        // 4. 验证页偏移计算
        System.out.println("\n4. 验证偏移计算:");
        for (int pageId : new int[]{0, 50, 100}) {
            long offset = dbFile.getPageOffset(pageId);
            System.out.printf("   页%d: 偏移 = %,d 字节 (从文件头后 %,d 字节)%n",
                    pageId, offset, offset - DBFile.HEADER_SIZE);
        }

        // 5. 读取空洞页（页50）
        System.out.println("\n5. 读取空洞页（页50）:");
        byte[] page50 = dbFile.readPage(50);
        System.out.println("   页50是否存在: " + dbFile.pageExists(50));

        // 检查是否全0（空洞应该返回全0）
        boolean allZero = true;
        for (byte b : page50) {
            if (b != 0) {
                allZero = false;
                break;
            }
        }
        System.out.println("   是否全0（空洞）: " + (allZero ? "" : "❌"));

        // 6. 验证写入的数据可读取
        System.out.println("\n6. 验证数据完整性:");
        byte[] readPage0 = dbFile.readPage(0);
        byte[] readPage100 = dbFile.readPage(100);
        System.out.println("   页0数据一致: " + Arrays.equals(page0Data, readPage0));
        System.out.println("   页100数据一致: " + Arrays.equals(page100Data, readPage100));

        // 7. 可视化文件结构
        System.out.println("\n7. 文件结构可视化:");
        FileStructureViewer.printFileStructure(dbFile, 128);

        // 8. 测试预读优化（可选）
        if (args.length > 0 && args[0].equals("--prefetch")) {
            System.out.println("\n8. 测试预读优化:");
            PrefetchDBFile prefetchDBFile = new PrefetchDBFile(filename);

            // 读取页10，并预读3页
            System.out.println("   读取页10，预读页11-13...");
            CompletableFuture<byte[]> future =
                    prefetchDBFile.readPageWithPrefetch(10, 3);

            future.thenAccept(data -> {
                System.out.println("   页10读取完成");

                // 检查预读缓存
                for (int i = 11; i <= 13; i++) {
                    Optional<byte[]> prefetched =
                            prefetchDBFile.getFromPrefetchCache(i);
                    System.out.println("   页" + i + "预读状态: " +
                            (prefetched.isPresent() ? "✅ 已缓存" : "⏳ 预读中"));
                }
            });

            // 等待完成
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        dbFile.close();

        System.out.println("\n=== Day 5 测试完成 ===");
    }

    private static byte[] createPageData(int pageId) {
        byte[] data = new byte[DBFile.PAGE_SIZE];
        // 在页的前4字节写入页号（用于验证）
        data[0] = (byte) (pageId >> 24);
        data[1] = (byte) (pageId >> 16);
        data[2] = (byte) (pageId >> 8);
        data[3] = (byte) pageId;
        return data;
    }
}