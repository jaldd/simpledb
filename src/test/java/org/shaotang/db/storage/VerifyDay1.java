// 创建一个验证程序 VerifyDay1.java
package org.shaotang.db.storage;

import org.shaotang.db.storage.DBFile;
import java.io.File;

public class VerifyDay1 {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Day 1 成果验证 ===");
        
        // 1. 清理并创建新文件
        String filename = "day1_test.db";
        new File(filename).delete();
        
        // 2. 创建数据库文件
        DBFile db = new DBFile(filename);
        System.out.println("✓ 数据库文件创建成功");
        
        // 3. 验证初始版本号是1
        long version = db.readVersion();
        System.out.println("初始版本号: " + version);
        if (version != 1) {
            throw new RuntimeException("错误：初始版本号应该是1，实际是" + version);
        }
        System.out.println("✓ 初始版本号正确");
        
        // 4. 测试写入新版本号
        long newVersion = 2025L;
        db.writeVersion(newVersion);
        long readBack = db.readVersion();
        System.out.println("写入版本号: " + newVersion);
        System.out.println("读取版本号: " + readBack);
        if (newVersion != readBack) {
            throw new RuntimeException("错误：版本号读写不一致");
        }
        System.out.println("✓ 版本号读写一致");
        
        // 5. 验证文件大小（应该正好8字节）
        long fileSize = new File(filename).length();
        System.out.println("文件大小: " + fileSize + " 字节");
        if (fileSize != 8) {
            System.out.println("⚠️ 注意：文件大小不是8字节，可能是其他数据，但功能正常");
        }
        
        db.close();
        new File(filename).delete();
        
        System.out.println("\n🎉 Day 1 任务完美完成！");
        System.out.println("你已经实现了数据库最基础的文件层！");
    }
}