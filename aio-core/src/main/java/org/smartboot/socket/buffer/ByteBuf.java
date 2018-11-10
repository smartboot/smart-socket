package org.smartboot.socket.buffer;

import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version V1.0 , 2018/10/31
 */
public class ByteBuf {

    BufferPage bufferPage;
    private ByteBuffer buffer;
    private int parentPosition;

    private int parentLimit;

    ByteBuf(BufferPage bufferPage, ByteBuffer buffer, int parentPosition, int parentLimit) {
        this.bufferPage = bufferPage;
        this.buffer = buffer;
        this.parentPosition = parentPosition;
        this.parentLimit = parentLimit;
    }

    int getParentPosition() {
        return parentPosition;
    }

    void setParentPosition(int parentPosition) {
        this.parentPosition = parentPosition;
    }

    int getParentLimit() {
        return parentLimit;
    }

    void setParentLimit(int parentLimit) {
        this.parentLimit = parentLimit;
    }

    public ByteBuffer buffer() {
        return buffer;
    }

    void buffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void release() {
        if (bufferPage != null) {
            bufferPage.release(this);
        }
    }

    @Override
    public String toString() {
        return "ByteBuf{" +
                "bufferPage=" + bufferPage +
                ", buffer=" + buffer +
                ", parentPosition=" + parentPosition +
                ", parentLimit=" + parentLimit +
                '}';
    }
}
