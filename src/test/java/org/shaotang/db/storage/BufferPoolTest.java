package org.shaotang.db.storage;

import org.shaotang.db.storage.Page;

import java.io.File;
import java.io.IOException;

public class BufferPoolTest {
    public static void main(String[] args) throws IOException {
        // 清理旧文件
        String filename = "test_buffer.db";
        new File(filename).delete();
        
        System.out.println("=== 测试缓冲池 ===");
        
        BufferPool pool = new BufferPool(filename);
        DBFile dbFile = pool.getDBFile(); // 假设BufferPool有getDBFile方法
        
        // 分配两个用户页进行测试
        int userPage1 = dbFile.allocateNewPage(); // 应该是页2
        int userPage2 = dbFile.allocateNewPage(); // 应该是页3
        
        System.out.println("分配的用户页ID: " + userPage1 + ", " + userPage2);
        
        // 测试1：第一次读取用户页1（应该从磁盘）
        System.out.println("\n测试1：第一次读取用户页" + userPage1);
        Page page1 = pool.getPage(userPage1);
        System.out.println("读取到的页" + userPage1 + "，偏移0的值: " + page1.getInt(0));
        
        // 测试2：再次读取用户页1（应该从缓存）
        System.out.println("\n测试2：再次读取用户页" + userPage1);
        Page page2 = pool.getPage(userPage1);
        System.out.println("读取到的页" + userPage1 + "，偏移0的值: " + page2.getInt(0));
        
        // 验证是否是同一个对象
        System.out.println("是否是同一个对象？ " + (page1 == page2));
        
        // 测试3：修改用户页1并标记为脏页
        System.out.println("\n测试3：修改用户页" + userPage1 + "并标记脏页");
        page1.setInt(0, 999);
        pool.markDirty();
        
        // 测试4：读取用户页2（缓存只有1个位置，所以用户页1会被替换）
        System.out.println("\n测试4：读取用户页" + userPage2 + "（这会替换缓存中的页" + userPage1 + "）");
        Page page3 = pool.getPage(userPage2);
        System.out.println("读取到的页" + userPage2 + "，偏移0的值: " + page3.getInt(0));
        
        // 测试5：再次读取用户页1（应该再次从磁盘读取）
        System.out.println("\n测试5：再次读取用户页" + userPage1 + "（应该重新从磁盘读）");
        Page page4 = pool.getPage(userPage1);
        System.out.println("读取到的页" + userPage1 + "，偏移0的值: " + page4.getInt(0));
        System.out.println("注意：如果是999，说明脏页写回成功；否则失败");
        
        // 显示数据库信息
        System.out.println("\n数据库信息：");
        dbFile.printInfo();
        
        pool.close();
        
        System.out.println("\n=== 测试完成 ===");
    }
}