package org.shaotang.db.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DBFileTest {
    private static final String TEST_FILE = "test.db";
    private DBFile dbFile;

    @BeforeEach
    void setUp() throws IOException {
        new File(TEST_FILE).delete();  // 清理旧文件
        dbFile = new DBFile(TEST_FILE);
    }

    @AfterEach
    void tearDown() throws IOException {
        System.out.println(dbFile.readVersion());
        if (dbFile != null) {
            dbFile.close();
        }
        /**
         *
         * 文件内容为
         * hexdump -C test.db
         *
         * 00000000  00 00 00 00 00 00 00 02                           |........|
         * 00000008
         *
         */
        new File(TEST_FILE).delete();
    }

    @Test
    void testVersionReadWrite() throws IOException {
        // 读取默认版本号应该是1
        assertEquals(1, dbFile.readVersion());

        // 测试写入新版本号
        dbFile.writeVersion(2);
        assertEquals(2, dbFile.readVersion());
    }

    public static void main(String[] args) throws Exception {
        String filename = "test.db";
        new File(filename).delete();

        System.out.println("=== 创建新数据库文件 ===");
        DBFile dbFile = new DBFile(filename);
        dbFile.printInfo();

        System.out.println("\n=== 分配几个页 ===");
        int page1 = dbFile.allocateNewPage();
        int page2 = dbFile.allocateNewPage();
        int page5 = dbFile.allocateNewPage();  // 可能是页5，因为位图会自动扩展

        dbFile.printInfo();

        System.out.println("\n=== 读取位图页内容 ===");
        byte[] bitmapData = dbFile.readPage(0);
        System.out.println("位图页前32字节: ");
        for (int i = 0; i < 32; i++) {
            System.out.printf("%02X ", bitmapData[i] & 0xFF);
            if ((i + 1) % 16 == 0) System.out.println();
        }

        dbFile.close();
    }
}