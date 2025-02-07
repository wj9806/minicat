package io.github.wj9806.minicat.io;

import java.nio.ByteBuffer;

public class NioUtil {

    // 扩容方法
    public static ByteBuffer expandBuffer(ByteBuffer originalBuffer, int additionalCapacity) {
        // 获取原始缓冲区的当前大小
        int currentCapacity = originalBuffer.capacity();

        // 新的容量为当前容量 + 额外的容量
        int newCapacity = currentCapacity + additionalCapacity;

        // 创建一个新的 ByteBuffer，其容量更大
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);

        // 将原 ByteBuffer 的内容复制到新缓冲区
        originalBuffer.flip();  // 切换为读取模式
        newBuffer.put(originalBuffer);

        return newBuffer;  // 返回新缓冲区
    }

}
