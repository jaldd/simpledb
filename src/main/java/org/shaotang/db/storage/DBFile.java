package org.shaotang.db.storage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 数据库文件管理器
 * 负责底层二进制文件的读写
 */
public class DBFile {
    private final File file;
    private RandomAccessFile raf;
    private FileChannel channel;

    // 文件头大小：8字节（版本号）
    private static final int HEADER_SIZE = 8;

    public DBFile(String filename) throws IOException {
        this.file = new File(filename);
        initialize();
    }

    private void initialize() throws IOException {
        // 如果文件不存在，创建并初始化文件头
        if (!file.exists()) {
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
            writeVersion(1);  // 默认版本号为1
        } else {
            raf = new RandomAccessFile(file, "rw");
            channel = raf.getChannel();
        }
    }

    /**
     * 写入版本号（文件头前8字节）
     */
    public void writeVersion(long version) throws IOException {
        // 方法1：使用RandomAccessFile（简单）
//        raf.seek(0);  // 定位到文件开头
//        raf.writeLong(version);

        // 或者方法2：使用FileChannel + ByteBuffer（性能更好）
         ByteBuffer buffer = ByteBuffer.allocate(8);
         buffer.putLong(version);
         buffer.flip();
        int write = channel.write(buffer, 0);
        System.out.println("✓ 写入版本号成功：" + write);
    }

    /**
     * 读取版本号
     */
    public long readVersion() throws IOException {
//        raf.seek(0);
//        return raf.readLong();

        // 或者使用FileChannel
         ByteBuffer buffer = ByteBuffer.allocate(8);
         channel.read(buffer, 0);
         buffer.flip();
         return buffer.getLong();
    }

    /**
     * 关闭文件（重要！）
     */
    public void close() throws IOException {
        if (channel != null) channel.close();
        if (raf != null) raf.close();
    }
}