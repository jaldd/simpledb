package org.shaotang.db.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 实现LRU替换策略的缓冲池
 * 最大缓存3个页
 */
public class LRUBufferPool {
    // 最大缓存页数
    private static final int MAX_CACHE_SIZE = 3;
    
    // 底层文件
    private final DBFile dbFile;
    
    // LRU数据结构：哈希表 + 双向链表
    private static class Node {
        int pageId;
        Page page;
        Node prev;
        Node next;
        
        Node(int pageId, Page page) {
            this.pageId = pageId;
            this.page = page;
        }
    }
    
    private final Map<Integer, Node> cacheMap = new HashMap<>();
    private Node head;  // 最近使用
    private Node tail;  // 最久未用
    private int size = 0;
    
    public LRUBufferPool(String filename) throws IOException {
        this.dbFile = new DBFile(filename);
    }
    
    /**
     * 获取页
     * 1. 如果在缓存中，移到链表头部并返回
     * 2. 如果不在缓存中，从磁盘读取，加入缓存
     * 3. 如果缓存已满，淘汰最久未用的页
     */
    public Page getPage(int pageId) throws IOException {
        System.out.printf("请求页 %d: ", pageId);
        
        // 1. 检查缓存中是否存在
        if (cacheMap.containsKey(pageId)) {
            Node node = cacheMap.get(pageId);
            moveToHead(node);
            System.out.println("缓存命中");
            return node.page;
        }
        
        // 2. 缓存未命中，从磁盘读取
        System.out.println("缓存未命中，从磁盘读取");
        Page page = readFromDisk(pageId);
        
        // 3. 创建新节点
        Node newNode = new Node(pageId, page);
        
        // 4. 如果缓存已满，淘汰最久未用的页
        if (size >= MAX_CACHE_SIZE) {
            evictOldest();
        }
        
        // 5. 添加新节点到缓存
        addToHead(newNode);
        cacheMap.put(pageId, newNode);
        size++;
        
        printCacheState();  // 打印当前缓存状态
        return page;
    }
    
    /**
     * 标记页为脏页
     */
    public void markDirty(int pageId) {
        if (cacheMap.containsKey(pageId)) {
            Node node = cacheMap.get(pageId);
            node.page.setDirty(true);
            System.out.println("标记页 " + pageId + " 为脏页");
        }
    }
    
    /**
     * 淘汰最久未使用的页
     */
    private void evictOldest() throws IOException {
        if (tail == null) return;
        
        Node toRemove = tail;
        System.out.println("淘汰页 " + toRemove.pageId);
        
        // 如果是脏页，写回磁盘
        if (toRemove.page.isDirty()) {
            System.out.println("写回脏页 " + toRemove.pageId);
            writeToDisk(toRemove.pageId, toRemove.page);
        }
        
        // 从缓存中移除
        removeNode(toRemove);
        cacheMap.remove(toRemove.pageId);
        size--;
    }
    
    /**
     * 将节点移到链表头部（最近使用）
     */
    private void moveToHead(Node node) {
        // 如果已经是头部，不需要移动
        if (node == head) return;
        
        // 从当前位置移除
        removeNode(node);
        
        // 添加到头部
        addToHead(node);
    }
    
    /**
     * 添加节点到链表头部
     */
    private void addToHead(Node node) {
        node.prev = null;
        node.next = head;
        
        if (head != null) {
            head.prev = node;
        }
        head = node;
        
        if (tail == null) {
            tail = node;
        }
    }
    
    /**
     * 从链表中移除节点
     */
    private void removeNode(Node node) {
        // 更新前驱节点
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            // node是头部
            head = node.next;
        }
        
        // 更新后继节点
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            // node是尾部
            tail = node.prev;
        }
    }
    
    /**
     * 打印当前缓存状态
     */
    private void printCacheState() {
        System.out.print("当前缓存: [");
        Node current = head;
        while (current != null) {
            System.out.print(current.pageId);
            if (current.next != null) System.out.print(" ← ");
            current = current.next;
        }
        System.out.println("] (最近使用 ← 最久未用)");
        System.out.println("缓存大小: " + size + "/" + MAX_CACHE_SIZE);
    }
    
    /**
     * 从磁盘读取页
     */
    private Page readFromDisk(int pageId) throws IOException {
        byte[] data = dbFile.readPage(pageId);
        return new Page(data);
    }
    
    /**
     * 将页写回磁盘
     */
    private void writeToDisk(int pageId, Page page) throws IOException {
        dbFile.writePage(pageId, page.getData());
        page.clearDirty();  // 清除脏页标记
    }
    
    /**
     * 关闭缓冲池，确保所有脏页写回
     */
    public void close() throws IOException {
        System.out.println("\n=== 关闭缓冲池，写回所有脏页 ===");
        for (Node node : cacheMap.values()) {
            if (node.page.isDirty()) {
                System.out.println("写回脏页 " + node.pageId);
                writeToDisk(node.pageId, node.page);
            }
        }
        dbFile.close();
    }
    
    /**
     * 获取当前缓存中的页ID列表（按LRU顺序）
     */
    public String getCacheState() {
        StringBuilder sb = new StringBuilder("[");
        Node current = head;
        while (current != null) {
            sb.append(current.pageId);
            if (current.next != null) sb.append(" ← ");
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}