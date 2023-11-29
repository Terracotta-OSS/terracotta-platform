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
package org.terracotta.runnel.encoding.dataholders;

import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class StringTest {

  @Test
  public void testWithIndex() throws Exception {
    StringDataHolder stringDataHolder = new StringDataHolder("aNormalString", 5);

    assertThat(stringDataHolder.size(true), is(15));

    ByteBuffer bb = ByteBuffer.allocate(stringDataHolder.size(true));
    stringDataHolder.encode(new WriteBuffer(bb), true);
    assertThat(bb.position(), is(15));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(5));
    assertThat(readBuffer.getVlqInt(), is(13));
    assertThat(readBuffer.getString(13), is("aNormalString"));
  }

  @Test
  public void testWithoutIndex() throws Exception {
    StringDataHolder stringDataHolder = new StringDataHolder("aNormalString", 5);

    assertThat(stringDataHolder.size(false), is(14));

    ByteBuffer bb = ByteBuffer.allocate(stringDataHolder.size(true));
    stringDataHolder.encode(new WriteBuffer(bb), false);
    assertThat(bb.position(), is(14));
    bb.rewind();
    ReadBuffer readBuffer = new ReadBuffer(bb);
    assertThat(readBuffer.getVlqInt(), is(13));
    String s = readBuffer.getString(13);
    assertThat(s, is("aNormalString"));
  }

  @Test
  @Ignore
  public void testEncodeDecodeSpeed() {
    final Random r = new Random(0);
    final int MIN_LEN = 80;
    final int MAX_LEN = 4 * 1024;
    final int PERCENT_NON_ASCII = 0; // utfishness
    final String[] strings = new String[1024];
    int max = 0;
    for (int i = 0; i < strings.length; i++) {
      strings[i] = randomString(r, MIN_LEN, MAX_LEN, PERCENT_NON_ASCII);
      max = Math.max(strings[i].length() * 4 + 1, max);
    }
    ByteBuffer scratch = ByteBuffer.allocate(max);
    WriteBuffer wb = new WriteBuffer(scratch);
    ReadBuffer rb = new ReadBuffer(scratch);
    for (int pass = 0; pass < 10; pass++) {
      long fp = 0;
      long slen = 0;
      long cnt = 0;
      long st = System.nanoTime();
      for (int i = 0; i < 1000000; i++) {
        String str = strings[r.nextInt(strings.length)];
        StringDataHolder dh = new StringDataHolder(str, 0);
        scratch.clear();
        dh.encode(wb, false);
        scratch.flip();
        int sz = rb.getVlqInt();
        String s = rb.getString(sz);
        fp += sz;
        slen += s.length();
        cnt++;
      }
      long tookNS = (System.nanoTime() - st);
      long bytesPerSecond = fp / TimeUnit.MILLISECONDS.convert(tookNS, TimeUnit.NANOSECONDS);
      System.out.println("Pass: " + pass + " footprint: " + fp + "slen: " + slen + " :: " + cnt + " took " + tookNS + "ns; " + bytesPerSecond + " bytes/sec");
    }
  }

  @Test
  @Ignore
  public void testDecodeSpeed() {
    final Random r = new Random(0);
    final int MIN_LEN = 80;
    final int MAX_LEN = 4096;
    final int PERCENT_NON_ASCII = 2; // utf
    final ByteBuffer[] encoded = new ByteBuffer[1024];
    for (int i = 0; i < encoded.length; i++) {
      String str = randomString(r, MIN_LEN, MAX_LEN, PERCENT_NON_ASCII);
      ByteBuffer scratch = ByteBuffer.allocate(str.length() * 4 + 1);
      WriteBuffer wb = new WriteBuffer(scratch);
      StringDataHolder dh = new StringDataHolder(str, 0);
      dh.encode(wb, false);
      scratch.flip();
      ByteBuffer b = ByteBuffer.allocate(scratch.remaining());
      b.put(scratch);
      b.clear();
      encoded[i] = b;
    }
    for (int pass = 0; pass < 10; pass++) {
      long fp = 0;
      long slen = 0;
      long cnt = 0;
      long st = System.nanoTime();
      for (int i = 0; i < 1000000; i++) {
        System.out.println(i);
        ByteBuffer src = encoded[r.nextInt(encoded.length)];
        ReadBuffer rb = new ReadBuffer(src.duplicate());
        int sz = rb.getVlqInt();
        String s = rb.getString(sz);
        fp += sz;
        slen += s.length();
        cnt++;
      }
      long tookNS = (System.nanoTime() - st);
      long bytesPerSecond = fp / TimeUnit.MILLISECONDS.convert(tookNS, TimeUnit.NANOSECONDS);
      System.out.println("Pass: " + pass + " footprint: " + fp + "slen: " + slen + " :: " + cnt + " took " + tookNS + "ns; " + bytesPerSecond + " bytes/sec");
    }
  }

  @Test
  @Ignore
  public void testEncodeSpeed() {
    final Random r = new Random(0);
    final int MIN_LEN = 80;
    final int MAX_LEN = 4096;
    final int PERCENT_NON_ASCII = 30; // utf
    final String[] strings = new String[1024];
    for (int i = 0; i < strings.length; i++) {
      strings[i] = randomString(r, MIN_LEN, MAX_LEN, PERCENT_NON_ASCII);
    }

    for (int pass = 0; pass < 10; pass++) {
      long fp = 0;
      long cnt = 0;
      long st = System.nanoTime();
      for (int i = 0; i < 2000000; i++) {
        StringDataHolder dh = new StringDataHolder(strings[r.nextInt(strings.length)], 0);
        fp += dh.valueSize();
        cnt++;
      }
      long tookNS = (System.nanoTime() - st);
      long bytesPerSecond = fp / TimeUnit.MILLISECONDS.convert(tookNS, TimeUnit.NANOSECONDS);
      System.out.println("Pass: " + pass + " footprint: " + fp + " :: " + cnt + " took " + tookNS + "ns; " + bytesPerSecond + " bytes/sec");
    }
  }

  private static String randomString(Random rnd, int minLength, int maxLength, int percentNonAscii) {

    final StringBuilder sb = new StringBuilder(maxLength - minLength + 1);
    int remaining = minLength + rnd.nextInt(maxLength - minLength + 1);
    while (remaining > 0) {
      if (percentNonAscii == 0 || rnd.nextInt(100) > percentNonAscii) {
        sb.appendCodePoint((rnd.nextInt(0x7F) + 1));
      } else {
        sb.appendCodePoint(rnd.nextInt(Character.MAX_CODE_POINT) + 1);
      }
      remaining--;
    }

    return sb.toString();
  }

}
