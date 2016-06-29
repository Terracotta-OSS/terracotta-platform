/**
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.management.service.buffer.impl;

import org.terracotta.management.service.buffer.BaseByteArrayBufferTest;
import org.terracotta.management.service.buffer.PartitionedRingBuffer;

/**
 * Test the Pooled ring buffer with a byte array type.
 *
 * @author RKAV
 */
public final class ByteArrayLocklessBufferTest extends BaseByteArrayBufferTest {

  @Override
  protected PartitionedRingBuffer<byte[]> getBufferUnderTest(int size) {
    return new SinglePartitionLockFreeRingBuffer<>(size);
  }
}
