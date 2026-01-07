package org.shaotang.db.storage;

import java.io.*;

public class DBFile {
    private final RandomAccessFile file;
    private static final int HEADER_SIZE = 8;  // 8字节存储版本号
    
    public DBFile(String filename) throws IOException {
        this.file = new RandomAccessFile(filename, "rw");
        // 如果文件是新建的，初始化版本号为1
        if (file.length() == 0) {
            writeVersion(1);
        }
    }
    
    public void writeVersion(long version) throws IOException {
        file.seek(0);
        file.writeLong(version);
    }
    
    public long readVersion() throws IOException {
        file.seek(0);
        return file.readLong();
    }
    
    public void close() throws IOException {
        file.close();
    }
}