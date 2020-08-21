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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;

import java.util.Random;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public class UIDTest {

  @Test
  public void newUID() {
    for (int i = 0; i < 10_000; i++) {
      UID uid = UID.newUID();
      assertTrue(UID.isUID(uid.toString()));
      assertThat(UID.encodeB64(UID.decodeB64(uid.toString())), is(equalTo(uid.toString())));
      assertThat(UID.encodeB64(UID.decodeB64(uid.toString())), is(equalTo(uid.toString())));
      assertThat(UID.valueOf(uid.toString()).asUUID(), is(equalTo(uid.asUUID())));
    }
  }

  @Test
  public void newUID_seed() {
    UID[] uids1 = new UID[10_000];
    Random r = new Random(0);
    for (int i = 0; i < 10_000; i++) {
      uids1[i] = UID.newUID(r);
    }

    UID[] uids2 = new UID[10_000];
    r = new Random(0);
    for (int i = 0; i < 10_000; i++) {
      uids2[i] = UID.newUID(r);
    }

    assertArrayEquals(uids1, uids2);
  }

  @Test
  public void bytes() {
    UUID uuid = UUID.randomUUID();
    long msb = uuid.getLeastSignificantBits();
    long lsb = uuid.getLeastSignificantBits();
    long[] longs = new long[]{msb, lsb};
    assertArrayEquals(longs, UID.toLongs(UID.toBytes(longs)));
  }
}