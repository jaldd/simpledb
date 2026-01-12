package org.shaotang.db.storage;

import java.io.IOException;

public class FileStructureViewer {

    public static void printFileStructure(DBFile dbFile, int maxPagesToShow)
            throws IOException {
        long fileSize = dbFile.getLogicalFileSize();
        int totalPages = dbFile.getTotalPages();

        System.out.println("=== 数据库文件结构分析 ===");
        System.out.printf("文件大小: %,d 字节 (%.2f MB)%n",
                fileSize, fileSize / (1024.0 * 1024.0));
        System.out.printf("页大小: %d 字节%n", DBFile.PAGE_SIZE);
        System.out.printf("文件头: %d 字节%n", DBFile.HEADER_SIZE);
        System.out.printf("逻辑页数: %d 页%n", totalPages);

        System.out.println("\n页分布图 (■=有数据, □=空洞):");

        // 每行显示32页
        int pagesPerRow = 32;
        int rows = (maxPagesToShow + pagesPerRow - 1) / pagesPerRow;

        for (int row = 0; row < rows; row++) {
            int startPage = row * pagesPerRow;
            int endPage = Math.min(startPage + pagesPerRow, maxPagesToShow);

            System.out.printf("页%04d-%04d: ", startPage, endPage - 1);

            for (int pageId = startPage; pageId < endPage; pageId++) {
                long offset = dbFile.getPageOffset(pageId);
                long pageEnd = offset + DBFile.PAGE_SIZE;

                if (pageEnd <= fileSize) {
                    System.out.print("■");  // 有数据
                } else {
                    System.out.print("□");  // 空洞
                }

                if ((pageId - startPage + 1) % 8 == 0) {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }

        // 显示空洞统计
        System.out.println("\n空洞分析:");
        long holeSize = calculateHoleSize(dbFile, maxPagesToShow);
        double holePercentage = (double) holeSize / (fileSize - DBFile.HEADER_SIZE) * 100;
        System.out.printf("空洞大小: %,d 字节 (%.1f%%)%n", holeSize, holePercentage);
    }

    private static long calculateHoleSize(DBFile dbFile, int maxPages)
            throws IOException {
        long holeSize = 0;
        long fileSize = dbFile.getLogicalFileSize();

        for (int pageId = 0; pageId < maxPages; pageId++) {
            long pageStart = dbFile.getPageOffset(pageId);
            long pageEnd = pageStart + DBFile.PAGE_SIZE;

            if (pageEnd > fileSize) {
                // 从pageStart到fileSize都是空洞
                holeSize += (pageEnd - Math.max(pageStart, fileSize));
            }
        }

        return holeSize;
    }
}