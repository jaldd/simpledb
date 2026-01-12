package org.shaotang.db.storage;

import java.io.File;
import java.io.IOException;

public class LRUBufferPoolTest {
    public static void main(String[] args) throws IOException {
        String filename = "lru_test.db";
        new File(filename).delete();
        
        System.out.println("=== LRU缓冲池测试 ===\n");
        System.out.println("最大缓存页数: 3");
        System.out.println("测试序列: 页1 → 页2 → 页3 → 页1 → 页4");
        System.out.println("预期: 当读取页4时，应该淘汰页2（最久未用）\n");
        
        LRUBufferPool pool = new LRUBufferPool(filename);
        
        // 测试序列：1, 2, 3, 1, 4
        System.out.println("1. 读取页1");
        pool.getPage(1);
        
        System.out.println("\n2. 读取页2");
        pool.getPage(2);
        
        System.out.println("\n3. 读取页3");
        pool.getPage(3);
        
        System.out.println("\n4. 再次读取页1（应该命中缓存）");
        pool.getPage(1);
        
        System.out.println("\n5. 读取页4（应该淘汰页2）");
        pool.getPage(4);
        
        // 验证缓存状态
        System.out.println("\n最终缓存状态: " + pool.getCacheState());
        System.out.println("预期: [4 ← 1 ← 3] 或 [4, 1, 3]（页2被淘汰）");
        
        // 测试脏页处理
        System.out.println("\n=== 测试脏页处理 ===");
        
        // 修改页4并标记为脏页
        System.out.println("\n6. 修改页4，标记为脏页");
        pool.getPage(4).setInt(0, 100);
        pool.markDirty(4);
        
        // 读取页5，应该淘汰某个页（不是页4，因为最近使用了页4）
        System.out.println("\n7. 读取页5（应该淘汰页3？）");
        pool.getPage(5);
        
        System.out.println("\n最终缓存状态: " + pool.getCacheState());
        
        pool.close();
        
        System.out.println("\n=== 测试完成 ===");
    }
}