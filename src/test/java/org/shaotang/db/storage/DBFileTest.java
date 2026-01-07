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
}