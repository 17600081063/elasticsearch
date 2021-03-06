/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.nio;

import org.elasticsearch.test.ESTestCase;
import org.junit.Before;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import static org.mockito.Mockito.mock;

public class BytesWriteOperationTests extends ESTestCase {

    private SocketChannelContext channelContext;
    private BiConsumer<Void, Throwable> listener;

    @Before
    @SuppressWarnings("unchecked")
    public void setFields() {
        channelContext = mock(SocketChannelContext.class);
        listener = mock(BiConsumer.class);

    }

    public void testFullyFlushedMarker() {
        ByteBuffer[] buffers = {ByteBuffer.allocate(10)};
        BytesWriteOperation writeOp = new BytesWriteOperation(channelContext, buffers, listener);

        writeOp.incrementIndex(10);

        assertTrue(writeOp.isFullyFlushed());
    }

    public void testPartiallyFlushedMarker() {
        ByteBuffer[] buffers = {ByteBuffer.allocate(10)};
        BytesWriteOperation writeOp = new BytesWriteOperation(channelContext, buffers, listener);

        writeOp.incrementIndex(5);

        assertFalse(writeOp.isFullyFlushed());
    }

    public void testMultipleFlushesWithCompositeBuffer() throws IOException {
        ByteBuffer[] buffers = {ByteBuffer.allocate(10), ByteBuffer.allocate(15), ByteBuffer.allocate(3)};
        BytesWriteOperation writeOp = new BytesWriteOperation(channelContext, buffers, listener);

        ArgumentCaptor<ByteBuffer[]> buffersCaptor = ArgumentCaptor.forClass(ByteBuffer[].class);

        writeOp.incrementIndex(5);
        assertFalse(writeOp.isFullyFlushed());
        ByteBuffer[] byteBuffers = writeOp.getBuffersToWrite();
        assertEquals(3, byteBuffers.length);
        assertEquals(5, byteBuffers[0].remaining());

        writeOp.incrementIndex(5);
        assertFalse(writeOp.isFullyFlushed());
        byteBuffers = writeOp.getBuffersToWrite();
        assertEquals(2, byteBuffers.length);
        assertEquals(15, byteBuffers[0].remaining());

        writeOp.incrementIndex(2);
        assertFalse(writeOp.isFullyFlushed());
        byteBuffers = writeOp.getBuffersToWrite();
        assertEquals(2, byteBuffers.length);
        assertEquals(13, byteBuffers[0].remaining());

        writeOp.incrementIndex(15);
        assertFalse(writeOp.isFullyFlushed());
        byteBuffers = writeOp.getBuffersToWrite();
        assertEquals(1, byteBuffers.length);
        assertEquals(1, byteBuffers[0].remaining());

        writeOp.incrementIndex(1);
        assertTrue(writeOp.isFullyFlushed());
        byteBuffers = writeOp.getBuffersToWrite();
        assertEquals(1, byteBuffers.length);
        assertEquals(0, byteBuffers[0].remaining());
    }
}
