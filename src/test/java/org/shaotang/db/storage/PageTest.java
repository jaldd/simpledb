package org.shaotang.db.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PageTest {

    @Test
    public void testPageBasic() {
        Page page = new Page();

        // 测试写入和读取整数
        page.setInt(0, 42);
        page.setInt(4, 100);

        assertEquals(42, page.getInt(0));
        assertEquals(100, page.getInt(4));

        // 测试边界
        assertDoesNotThrow(() -> {
            page.setInt(4092, 999);  // 应该成功，因为4092+4=4096
        });

        assertThrows(IllegalArgumentException.class, () -> {
            page.setInt(4093, 999);  // 应该失败，越界
        });
    }

    @Test
    public void testPageDataPersistence() {
        // 测试页数据可以序列化和反序列化
        Page page1 = new Page();
        page1.setInt(0, 123);
        page1.setInt(100, 456);

        // 获取原始字节
        byte[] data = page1.getData();

        // 用这些字节创建新页
        Page page2 = new Page(data);

        // 验证数据一致
        assertEquals(123, page2.getInt(0));
        assertEquals(456, page2.getInt(100));
    }
}