# simpledb
我的第一个数据库项目
1：项目初始化
核心概念：数据库的本质是持久化存储，最终都要落到磁盘文件
需求：创建一个项目，能读写一个二进制文件，文件前8字节存储"版本号"
知识点：

理解数据库文件是二进制格式，不是文本

文件头(header)存储元数据

Java的RandomAccessFile或FileChannel

测试思路：

java
// 测试：写入版本号1，读取验证是1
void testVersion() {
写入版本号(1);
assert 读取版本号() == 1;
}

2：页面抽象
核心概念：数据库以页(Page) 为单位管理磁盘和内存，通常4KB
需求：设计Page类，支持在页面内存储多个整数（如每4字节一个int）
页是磁盘和内存交换的最小单位
页内偏移量计算
使用小端序
// 测试：在页的偏移0处写入42，偏移4处写入100
Page p = new Page();
p.setInt(0, 42);
p.setInt(4, 100);
assert p.getInt(0) == 42;
assert p.getInt(4) == 100;
