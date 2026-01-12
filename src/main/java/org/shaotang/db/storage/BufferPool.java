package org.shaotang.db.storage;

import java.io.IOException;

/**
 * 简单的缓冲池，只能缓存一个页
 */
public class BufferPool {
    // 当前缓存的页
    private Page cachedPage;
    
    // 当前缓存的页号
    private Integer cachedPageId;
    
    // 当前页是否是脏页（被修改过）
    private boolean dirty;
    
    // 底层文件管理器
    private final DBFile dbFile;
    
    public BufferPool(String filename) throws IOException {
        this.dbFile = new DBFile(filename);
        this.cachedPage = null;
        this.cachedPageId = null;
        this.dirty = false;
    }

    public DBFile getDBFile() {
        return dbFile;
    }
    /**
     * 获取指定页号的页
     * 如果缓存中有，直接返回；否则从磁盘读取并缓存
     */
    public Page getPage(int pageId) throws IOException {
        // 检查缓存是否命中
        if (cachedPageId != null && cachedPageId == pageId) {
            System.out.println("缓存命中！从内存返回页 " + pageId);
            return cachedPage;
        }
        
        // 缓存未命中，需要从磁盘读取
        System.out.println("缓存未命中，从磁盘读取页 " + pageId);
        
        // 如果当前有缓存页且是脏页，需要先写回磁盘
        if (dirty && cachedPageId != null) {
            System.out.println("将脏页 " + cachedPageId + " 写回磁盘");
            // 这里需要实现将页写回磁盘的功能
            // 由于Day 1的DBFile只支持版本号，我们需要扩展它
        }
        
        // 从磁盘读取页（暂时简单实现）
        Page page = readPageFromDisk(pageId);
        
        // 更新缓存
        cachedPage = page;
        cachedPageId = pageId;
        dirty = false;
        
        return page;
    }
    
    /**
     * 标记当前缓存页为脏页
     */
    public void markDirty() {
        if (cachedPage != null) {
            this.dirty = true;
            System.out.println("标记页 " + cachedPageId + " 为脏页");
        }
    }

    // 添加写页方法
    private void writePageToDisk(int pageId, Page page) throws IOException {
        byte[] data = page.getData();
        dbFile.writePage(pageId, data);
    }

    // 更新flush方法
    public void flush() throws IOException {
        if (dirty && cachedPage != null && cachedPageId != null) {
            System.out.println("将脏页 " + cachedPageId + " 写回磁盘");
            writePageToDisk(cachedPageId, cachedPage);
            dirty = false;
        }
    }
    
    /**
     * 从磁盘读取页（简化版）
     * 页大小是4096字节
     */
    private Page readPageFromDisk(int pageId) throws IOException {
        byte[] data = dbFile.readPage(pageId);
        Page page = new Page(data);

        // 重要：如果这是新分配的页，我们需要知道！
        // 因为它可能还没有在磁盘上分配空间
        boolean isNewPage = (dbFile.getTotalPages() <= pageId);

        if (isNewPage) {
            System.out.println("页" + pageId + "是新页（还未分配磁盘空间）");
        }

        return page;
    }
    
    public void close() throws IOException {
        flush();  // 关闭前确保脏页写回
        dbFile.close();
    }
}