package org.shaotang.db.storage;

public class PageBoundaryTest {
    public static void main(String[] args) {
        Page page = new Page();
        
        System.out.println("=== 测试边界检查 ===");
        
        // 1. 测试有效写入
        System.out.println("1. 有效写入测试:");
        page.setInt(0, 100);
        //十进制100 = 十六进制0x64
        //小端序存储：64 00 00 00
        //页面偏移0-3字节：0x64, 0x00, 0x00, 0x00
        page.dump(1024);
        //0000: 64 00 00 00 00 00 00 00 ... （后面1016个00）
        //0400: 00 00 00 00 ... （1024个00）
        //0800: 00 00 00 00 ... （1024个00）
        //0C00: 00 00 00 00 ... （1024个00）


        page.setLong(4088, 123456789L);  // 刚好在边界
        System.out.println("   读取位置4088: " + page.getLong(4088));
        page.setByte(4095, (byte)0xFF);  // 最后一个字节
        System.out.println("   读取位置4095: " + page.getByte(4095));
        System.out.println("   ✅ 所有有效写入成功");
        // 2. 测试无效写入
        System.out.println("\n2. 无效写入测试:");
        
        try {
            page.setInt(4093, 999);
            System.out.println("   ❌ 应该抛出异常但没抛出");
        } catch (IllegalArgumentException e) {
            System.out.println("   ✅ setInt(4093) 正确抛出异常");
        }
        
        try {
            page.setLong(4089, 999L);
            System.out.println("   ❌ 应该抛出异常但没抛出");
        } catch (IllegalArgumentException e) {
            System.out.println("   ✅ setLong(4089) 正确抛出异常");
        }
        
        try {
            page.setByte(4096, (byte)0x01);
            System.out.println("   ❌ 应该抛出异常但没抛出");
        } catch (IllegalArgumentException e) {
            System.out.println("   ✅ setByte(4096) 正确抛出异常");
        }
        
        try {
            page.setInt(-1, 999);
            System.out.println("   ❌ 应该抛出异常但没抛出");
        } catch (IllegalArgumentException e) {
            System.out.println("   ✅ setInt(-1) 正确抛出异常");
        }
        
        // 3. 测试读取
        System.out.println("\n3. 读取测试:");
        System.out.println("   读取位置0: " + page.getInt(0));
        System.out.println("   读取位置4088: " + page.getLong(4088));
        System.out.println("   读取位置4095: " + String.format("0x%02X", page.getByte(4095)));
    }
}