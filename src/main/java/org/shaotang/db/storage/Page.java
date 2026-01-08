package org.shaotang.db.storage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Page {
    public static final int PAGE_SIZE = 4096;  // 4KB

    private final ByteBuffer buffer;

    public Page() {
        this.buffer = ByteBuffer.allocate(PAGE_SIZE);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public Page(byte[] data) {
        if (data.length != PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("页大小必须为%d字节，实际为%d字节", PAGE_SIZE, data.length));
        }
        this.buffer = ByteBuffer.wrap(data);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    // ===== 边界检查方法 =====
    private void checkBounds(int offset, int size) {
        if (offset < 0) {
            throw new IllegalArgumentException(
                    String.format("偏移量不能为负数: offset=%d", offset));
        }
        if (offset + size > PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("偏移量超出页面范围: offset=%d, size=%d, pageSize=%d",
                            offset, size, PAGE_SIZE));
        }
    }

    // ===== 整型数据操作 =====
    public void setInt(int offset, int value) {
        checkBounds(offset, Integer.BYTES);  // 4字节
        buffer.putInt(offset, value);
    }

    public int getInt(int offset) {
        checkBounds(offset, Integer.BYTES);
        return buffer.getInt(offset);
    }

    // ===== 长整型数据操作 =====
    public void setLong(int offset, long value) {
        checkBounds(offset, Long.BYTES);  // 8字节
        buffer.putLong(offset, value);
    }

    public long getLong(int offset) {
        checkBounds(offset, Long.BYTES);
        return buffer.getLong(offset);
    }

    // ===== 短整型数据操作 =====
    public void setShort(int offset, short value) {
        checkBounds(offset, Short.BYTES);  // 2字节
        buffer.putShort(offset, value);
    }

    public short getShort(int offset) {
        checkBounds(offset, Short.BYTES);
        return buffer.getShort(offset);
    }

    // ===== 字节操作 =====
    public void setByte(int offset, byte value) {
        checkBounds(offset, Byte.BYTES);  // 1字节
        buffer.put(offset, value);
    }

    public byte getByte(int offset) {
        checkBounds(offset, Byte.BYTES);
        return buffer.get(offset);
    }

    // ===== 字节数组操作 =====
    public void setBytes(int offset, byte[] data) {
        checkBounds(offset, data.length);
        // 手动复制，因为ByteBuffer.put(offset, byte[])不存在
        for (int i = 0; i < data.length; i++) {
            buffer.put(offset + i, data[i]);
        }
    }

    public byte[] getBytes(int offset, int length) {
        checkBounds(offset, length);
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = buffer.get(offset + i);
        }
        return result;
    }

    // ===== 其他方法 =====
    public byte[] getData() {
        return buffer.array();
    }

    public int getSize() {
        return PAGE_SIZE;
    }

    // 清空页面（全部置0）
    public void clear() {
        for (int i = 0; i < PAGE_SIZE; i++) {
            buffer.put(i, (byte)0);
        }
    }

    // 调试：查看页面内容
    public void dump(int bytesPerRow) {
        byte[] data = getData();
        for (int i = 0; i < PAGE_SIZE; i += bytesPerRow) {
            System.out.printf("%04X: ", i);
            for (int j = 0; j < bytesPerRow && i + j < PAGE_SIZE; j++) {
                System.out.printf("%02X ", data[i + j]);
            }
            System.out.println();
        }
    }
}