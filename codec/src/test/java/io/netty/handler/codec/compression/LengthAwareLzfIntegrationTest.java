/*
 * Copyright 2020 The Netty Project
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

import io.netty.channel.embedded.EmbeddedChannel;

import static com.ning.compress.lzf.LZFChunk.MAX_CHUNK_LEN;

public class LengthAwareLzfIntegrationTest extends LzfIntegrationTest {

    @Override
    protected EmbeddedChannel createEncoder() {
        return new EmbeddedChannel(new LzfEncoder(MAX_CHUNK_LEN, 2 * 1024 * 1024));
    }
}
