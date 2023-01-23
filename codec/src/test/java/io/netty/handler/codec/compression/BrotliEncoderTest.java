/*
 * Copyright 2021 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec.compression;

import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BrotliEncoderTest extends AbstractEncoderTest {

    private EmbeddedChannel ENCODER_CHANNEL;
    private EmbeddedChannel DECODER_CHANNEL;

    @BeforeAll
    static void setUp() {
        try {
            Brotli.ensureAvailability();
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    @Override
    public EmbeddedChannel createChannel() {
        // Setup Encoder and Decoder
        ENCODER_CHANNEL = new EmbeddedChannel(new BrotliEncoder());
        DECODER_CHANNEL = new EmbeddedChannel(new BrotliDecoder());

        // Return the main channel (Encoder)
        return ENCODER_CHANNEL;
    }

    @Override
    public void destroyChannel() {
        ENCODER_CHANNEL.finishAndReleaseAll();
        DECODER_CHANNEL.finishAndReleaseAll();
    }

    @Override
    protected ByteBuf decompress(ByteBuf compressed, int originalLength) {
        DECODER_CHANNEL.writeInbound(compressed);

        ByteBuf aggregatedBuffer = Unpooled.buffer();
        ByteBuf decompressed = DECODER_CHANNEL.readInbound();
        while (decompressed != null) {
            aggregatedBuffer.writeBytes(decompressed);

            decompressed.release();
            decompressed = DECODER_CHANNEL.readInbound();
        }

        return aggregatedBuffer;
    }

    @Override
    protected ByteBuf readDecompressed(final int dataLength) throws Exception {
        CompositeByteBuf decompressed = Unpooled.compositeBuffer();
        ByteBuf msg;
        while ((msg = channel.readOutbound()) != null) {
            if (msg.isReadable()) {
                decompressed.addComponent(true, decompress(msg, -1));
            } else {
                msg.release();
            }
        }
        return decompressed;
    }

    @Test
    public void brotliInputStreamCanReadCompressedData() throws Exception {
        final String text = "Hello Hello Hello Hello Hello";
        ByteBuf textBuf = ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, text);
        final int textByteCount = textBuf.readableBytes();
        System.out.println("text: " + text);
        System.out.println("textByteCount: " + textByteCount);
        ENCODER_CHANNEL.writeOutbound(textBuf);
        ENCODER_CHANNEL.finish();
        Queue<Object> queue = ENCODER_CHANNEL.outboundMessages();
        System.out.println("queue.size: " + queue.size());
        ByteBuf compressed = ENCODER_CHANNEL.readOutbound();
        System.out.println("compressed readableBytes: " + compressed.readableBytes());
        BrotliInputStream brotliInputStream = new BrotliInputStream(new ByteArrayInputStream(ByteBufUtil.getBytes(compressed)));
        byte[] temp = new byte[textByteCount];
        int n = brotliInputStream.read(temp);
        assertEquals(textByteCount, n);
        String result = new String(temp);
        assertEquals(text, result);
    }
}
