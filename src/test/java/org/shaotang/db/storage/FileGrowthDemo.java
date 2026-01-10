package org.shaotang.db.storage;

import java.io.*;


/**
 * 测试跳页写入。
 */
public class FileGrowthDemo {
    public static final int HEADER_SIZE = 8;
    public static final int PAGE_SIZE = 4096;
    
    public static void main(String[] args) throws IOException {
        String filename = "demo.db";
        new File(filename).delete();
        
        // 创建只有文件头的数据库文件
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        file.writeLong(1);  // 版本号1
        System.out.println("初始文件大小: " + file.length() + " 字节");
        
        // 模拟读取页0
        System.out.println("\n=== 读取页0 ===");
        byte[] page0 = readPage(file, 0);
        System.out.println("页0大小: " + page0.length);
        System.out.println("页0内容（前16字节）:");
        for (int i = 0; i < 16; i++) {
            System.out.printf("%02X ", page0[i] & 0xFF);
        }
        System.out.println("（全是0，因为这是新页）");
        
        // 写入一些数据到页0
        System.out.println("\n=== 写入数据到页0 ===");
        file.seek(HEADER_SIZE);
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)(i + 1);  // 1,2,3,...100
        }
        file.write(data);
        System.out.println("写入后文件大小: " + file.length() + " 字节");
        
        // 再次读取页0
        System.out.println("\n=== 再次读取页0 ===");
        page0 = readPage(file, 0);
        System.out.println("前16字节:");
        for (int i = 0; i < 16; i++) {
            System.out.printf("%02X ", page0[i] & 0xFF);
        }
        System.out.println("\n注意：前100字节是我们写入的数据，后面3996字节是0");
        
        // 读取页5（还不存在）
        System.out.println("\n=== 读取页5（不存在）===");
        byte[] page5 = readPage(file, 5);
        System.out.println("文件大小: " + file.length());
        System.out.println("页5大小: " + page5.length);
        System.out.println("页5内容（前16字节）:");
        for (int i = 0; i < 16; i++) {
            System.out.printf("%02X ", page5[i] & 0xFF);
        }
        System.out.println("（全是0，因为这个页还没分配）");
        
        file.close();
        new File(filename).delete();
    }
    
    // 模拟readPage方法
    static byte[] readPage(RandomAccessFile file, int pageId) throws IOException {
        long offset = HEADER_SIZE + (long)pageId * PAGE_SIZE;
        
        // 如果偏移超出文件长度
        if (offset >= file.length()) {
            System.out.println("偏移" + offset + "超出文件长度" + file.length() + "，返回全0页");
            return new byte[PAGE_SIZE];  // 全0
        }
        
        file.seek(offset);
        byte[] data = new byte[PAGE_SIZE];
        int bytesRead = file.read(data);
        
        // 如果读取不满一页
        if (bytesRead < PAGE_SIZE) {
            System.out.println("只读取到" + bytesRead + "字节，填充" + 
                             (PAGE_SIZE - bytesRead) + "个0");
            for (int i = bytesRead; i < PAGE_SIZE; i++) {
                data[i] = 0;
            }
        }
        
        return data;
    }
}