package org.shaotang.db.storage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;

/**
 * 数据库文件管理器
 * 负责底层二进制文件的读写
 */
public class DBFile {
    private final File file;
    private RandomAccessFile raf;
    private FileChannel channel;
    private BitSet allocatedPages;  // 位图记录页分配状态

    // 位图页（页0）用于存储分配信息
    private static final int BITMAP_PAGE_ID = 0;

    // 文件头大小：8字节（版本号）
    private static final int HEADER_SIZE = 8;  // 版本号8字节
    public static final int PAGE_SIZE = 4096;  // 与Page类一致

    private static final int META_INFO_PAGE = 1;  // 元数据页

    private static final int FIRST_USER_PAGE = 2; // 第一个用户可用页

    private static final int SYSTEM_PAGES = 2; // 系统页数量：页0和页1
    /**
     * 将用户页号转换为实际页号（加上系统页偏移）
     */
    private int toPhysicalPageId(int logicalPageId) {
        return logicalPageId + SYSTEM_PAGES;
    }

    /**
     * 将实际页号转换为用户页号
     */
    private int toLogicalPageId(int physicalPageId) {
        return physicalPageId - SYSTEM_PAGES;
    }

    public DBFile(String filename) throws IOException {
        this.file = new File(filename);
        initialize();
    }

    private void initialize() throws IOException {
        // 如果文件不存在，创建并初始化文件头
        raf = new RandomAccessFile(file, "rw");
        channel = raf.getChannel();
        this.allocatedPages = new BitSet();
        if (!file.exists() || file.length() == 0) {
            writeVersion(1);  // 默认版本号为1
            allocatedPages.set(BITMAP_PAGE_ID);  // 页0（位图页）已分配
            // 页1（版本信息页）已分配
            allocatedPages.set(META_INFO_PAGE);
            writeBitmap();  // 写到位图页

            initializeMetaPage();
        } else {
            // 1. 读取版本号
            long version = readVersion();
            System.out.println("数据库版本: " + version);

            // 2. 读取位图
            readBitmap();
        }
    }

    private void initializeMetaPage() throws IOException {
        // 在页1中存储更多版本信息
        Page versionPage = new Page();

        // 写入数据库版本（详细）
        versionPage.setInt(0, 1);           // 主版本
        versionPage.setInt(4, 0);           // 次版本
        versionPage.setInt(8, PAGE_SIZE);   // 页大小

        // 写入创建时间（简化）
        long createTime = System.currentTimeMillis();
        versionPage.setLong(100, createTime);

        byte[] data = versionPage.getData();
        writePage(META_INFO_PAGE, data);
    }

    // 从位图页读取分配信息
    private void readBitmap() throws IOException {
        byte[] bitmapData = readPage(BITMAP_PAGE_ID);
        allocatedPages = BitSet.valueOf(bitmapData);
    }

    // 将分配信息写到磁盘
    private void writeBitmap() throws IOException {
        byte[] bitmapData = allocatedPages.toByteArray();
        // 确保位图数据不超过一页
        if (bitmapData.length > PAGE_SIZE) {
            throw new IOException("位图太大，超过一页");
        }
        byte[] pageData = new byte[PAGE_SIZE];
        System.arraycopy(bitmapData, 0, pageData, 0,
                bitmapData.length);
        System.out.println("准备写入的页数据长度: " + pageData.length);
        writePage(BITMAP_PAGE_ID, pageData);
    }

    // 智能分配新页
    public int allocateNewPage() throws IOException {
        // 从第一个用户页开始查找空闲页
        int physicalPageId = allocatedPages.nextClearBit(FIRST_USER_PAGE);

        // 如果当前所有页都已分配，pageId会是当前size()或更大
        // 这意味着我们需要分配一个新页
        if (physicalPageId >= allocatedPages.size()) {
            // 新页号就是pageId（可能等于size()或更大）
            // BitSet的set()方法会自动扩展
            //todo
            physicalPageId = allocatedPages.size();
        }

        // 标记为已分配
        allocatedPages.set(physicalPageId);

        // 确保磁盘文件足够大
        ensureFileSize(physicalPageId + 1);

        // 更新位图到磁盘
        writeBitmap();

        return toLogicalPageId(physicalPageId);
    }

    // 确保文件可以容纳指定数量的页
    private void ensureFileSize(int minPages) throws IOException {
        long requiredSize = HEADER_SIZE + (long) minPages * PAGE_SIZE;
        if (raf.length() < requiredSize) {
            raf.setLength(requiredSize);
            System.out.println("扩展文件到 " + requiredSize + " 字节 (" + minPages + " 页)");
        }
    }

    // 批量分配多个页（更高效）
    public int[] allocatePages(int count) throws IOException {
        int[] pageIds = new int[count];

        for (int i = 0; i < count; i++) {
            pageIds[i] = allocateNewPage();
        }

        // 一次性更新位图
        writeBitmap();

        return pageIds;
    }

    // 释放页
    public void freePage(int pageId) throws IOException {
        if (pageId < FIRST_USER_PAGE) {
            throw new IllegalArgumentException("不能释放系统页: " + pageId);
        }

        if (!allocatedPages.get(pageId)) {
            throw new IllegalArgumentException("页 " + pageId + " 未分配");
        }

        allocatedPages.clear(pageId);
        writeBitmap();
    }

    // 获取总页数（包括已分配和未分配）
    public int getTotalPages() {
        return Math.max(allocatedPages.size(), FIRST_USER_PAGE);
    }

    // 获取已分配的页数
    public int getAllocatedPageCount() {
        return allocatedPages.cardinality();
    }

    // 获取空闲页数
    public int getFreePageCount() {
        return getTotalPages() - getAllocatedPageCount();
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
     * 读取指定页号的数据
     */
    public byte[] readPage(int logicalPageId) throws IOException {
        int physicalPageId = toPhysicalPageId(logicalPageId);
        long offset = HEADER_SIZE + (long) physicalPageId * PAGE_SIZE;

        /**
         raf.seek(offset);

         byte[] data = new byte[PAGE_SIZE];
         int bytesRead = raf.read(data);
         // 如果文件不够大（比如新页），返回全0的页
         if (bytesRead < PAGE_SIZE) {
         for (int i = bytesRead; i < PAGE_SIZE; i++) {
         data[i] = 0;
         }
         }
         **/
        long fileSize = channel.size();

        // 准备返回的数据
        byte[] data = new byte[PAGE_SIZE]; // 默认全0

        // 检查偏移量是否超出文件大小
        if (offset >= fileSize) {
            return data; // 返回全0
        }

        // 计算可读取的最大字节数
        long availableBytes = Math.min(PAGE_SIZE, fileSize - offset);
        if (availableBytes <= 0) {
            return data;
        }


        // 使用ByteBuffer包装数据数组
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, (int)availableBytes);

        // 从指定位置读取数据
        int bytesRead = 0;
        while (bytesRead < availableBytes) {
            int read = channel.read(buffer, offset + bytesRead);
            if (read == -1) {
                break; // 到达文件末尾
            }
            bytesRead += read;
        }

        return data;
    }

    /**
     * 写入指定页号的数据
     */
    public void writePage(int logicalPageId, byte[] data) throws IOException {
        if (data.length != PAGE_SIZE) {
            throw new IllegalArgumentException("页数据大小必须为 " + PAGE_SIZE);
        }
        int physicalPageId = toPhysicalPageId(logicalPageId);
        long offset = HEADER_SIZE + (long) physicalPageId * PAGE_SIZE;

//        raf.seek(offset);
//        raf.write(data);

        // 确保文件足够大
        long requiredSize = offset + PAGE_SIZE;
        if (channel.size() < requiredSize) {
            raf.setLength(requiredSize); // 扩展文件大小
        }

        // 使用ByteBuffer包装数据
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // 写入到指定位置
        int bytesWritten = 0;
        while (bytesWritten < PAGE_SIZE) {
            int written = channel.write(buffer, offset + bytesWritten);
            if (written == -1) {
                throw new IOException("写入失败：到达文件末尾");
            }
            bytesWritten += written;
        }

        // 可选：强制写入磁盘（确保数据持久化）
        channel.force(false); // false表示不强制更新元数据

        System.out.println("写入页 " + physicalPageId + "，偏移量 " + offset + "，写入字节数: " + bytesWritten);
    }

    /**
     * 关闭文件（重要！）
     */
    public void close() throws IOException {
        if (channel != null) channel.close();
        if (raf != null) raf.close();
    }

    // 获取数据库信息
    public void printInfo() throws IOException {
        System.out.println("=== 数据库信息 ===");
        System.out.println("版本: " + readVersion());
        System.out.println("页大小: " + PAGE_SIZE + " 字节");
        System.out.println("总页数: " + getTotalPages());
        System.out.println("已分配页: " + allocatedPages.cardinality());
        System.out.println("空闲页: " + (getTotalPages() - allocatedPages.cardinality()));

        // 显示位图前16页的状态
        System.out.print("前16页分配状态: ");
        for (int i = 0; i < 16; i++) {
            System.out.print(allocatedPages.get(i) ? "■" : "□");
            if ((i + 1) % 8 == 0) System.out.print(" ");
        }
        System.out.println(" (■=已分配, □=空闲)");
    }

}