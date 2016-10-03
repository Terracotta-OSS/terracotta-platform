/*
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
package org.terracotta.management.service.monitoring;

import org.junit.Test;
import org.terracotta.management.service.monitoring.buffer.ReadWriteBuffer;
import org.terracotta.management.service.monitoring.buffer.RingBuffer;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class RingBufferTest {

  @Test
  public void test_ringBuffer() throws Exception {
    ReadWriteBuffer<Integer> buffer = new RingBuffer<>(2);
    assertThat(buffer.isEmpty(), equalTo(true));
    assertThat(buffer.size(), equalTo(0));
    assertThat(buffer.read(), equalTo(null));
    assertThat(buffer.stream().count(), equalTo(0L));

    buffer.put(1);
    buffer.put(2);
    assertThat(buffer.isEmpty(), equalTo(false));
    assertThat(buffer.size(), equalTo(2));

    buffer.put(3);
    assertThat(buffer.size(), equalTo(2));

    assertThat(buffer.read(), equalTo(2));
    assertThat(buffer.size(), equalTo(1));
    assertThat(buffer.read(), equalTo(3));
    assertThat(buffer.isEmpty(), equalTo(true));
    assertThat(buffer.size(), equalTo(0));

    assertThat(buffer.read(), equalTo(null));

    buffer.put(1);
    buffer.put(2);
    assertThat(buffer.size(), equalTo(2));
    List<Integer> collect = buffer.stream().collect(Collectors.toList());
    assertThat(collect.get(0), equalTo(1));
    assertThat(collect.get(1), equalTo(2));
    // now that the stream has been consumed once, calling it again on buffer returns an empty stream
    assertThat(buffer.stream().count(), equalTo(0L));
  }

}
