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
package org.terracotta.voltron.proxy;

import org.junit.Test;
import org.terracotta.AvailableClass;
import org.terracotta.voltron.proxy.shaded.org.terracotta.AvailableShadedClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

public class SerializationCodecTest {

  @SuppressWarnings("unchecked")
  @Test
  public void testUnmatchedShadedSubstitution() {
    SerializationCodec codec = new SerializationCodec(Pattern.compile("blah"));

    List<String> foobar = codec.decode(List.class, codec.encode(List.class, singletonList("foobar")));

    assertThat(foobar, contains("foobar"));
  }

  @Test
  public void testMatchedShadedSubstitutionWithAvailableClass() {
    SerializationCodec codec = new SerializationCodec(Pattern.compile("^org\\.terracorra\\.voltron\\.proxy\\.shaded\\.(.*)$"));

    AvailableShadedClass foobar = codec.decode(AvailableShadedClass.class, codec.encode(AvailableShadedClass.class, new AvailableShadedClass("foobar")));

    assertThat(foobar.string, is("foobar"));
  }

  @Test
  public void testMatchedShadedSubstitutionWithUnavailableClass() throws IOException {

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oout = new ObjectOutputStream(bout) {
        @Override
        public void writeUTF(String str) throws IOException {
          if (str.equals(AvailableClass.class.getName())) {
            super.writeUTF("org.terracotta.voltron.proxy.shaded." + str);
          } else {
            super.writeUTF(str);
          }
        }
      };
      oout.writeObject(new AvailableClass("foobar"));
    } finally {
      bout.close();
    }

    byte[] encoded = bout.toByteArray();

    SerializationCodec regularCodec = new SerializationCodec();
    try {
      regularCodec.decode(AvailableClass.class, encoded);
      fail("Expected RuntimeException");
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), containsString("java.lang.ClassNotFoundException: org.terracotta.voltron.proxy.shaded.org.terracotta.AvailableClass"));
    }

    SerializationCodec deshadingCodec = new SerializationCodec(Pattern.compile("^org\\.terracotta\\.voltron\\.proxy\\.shaded\\.(.*)$"));
    AvailableClass foobar = deshadingCodec.decode(AvailableClass.class, encoded);

    assertThat(foobar.string, is("foobar"));
  }
}
