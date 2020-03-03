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
package org.terracotta.persistence.sanskrit;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.terracotta.persistence.sanskrit.MarkableLineParser.LS;

public class MarkableLineParserTest {
  @Test
  public void iterateSimpleLines() throws Exception {
    InputStream bytes = makeStream("ab", "c", "", "def", "", "g");
    MarkableLineParser parser = new MarkableLineParser(bytes);
    List<String> lines = parser.lines().collect(Collectors.toList());
    assertThat(lines, contains("ab", "c", "", "def", "", "g"));
  }

  @Test
  public void iterateMultiByteCharacterLines() throws Exception {
    InputStream bytes = makeStream("Ɵ");
    MarkableLineParser parser = new MarkableLineParser(bytes);
    List<String> lines = parser.lines().collect(Collectors.toList());
    assertThat(lines, contains("Ɵ"));
  }

  @Test
  public void brokenMultiByteCharacter() {
    InputStream bytes = new ByteArrayInputStream(new byte[] { -58 });
    MarkableLineParser parser = new MarkableLineParser(bytes);
    List<String> lines = parser.lines().collect(Collectors.toList());
    assertThat(lines, empty());
  }

  @Test
  public void markLineEnding() throws Exception {
    InputStream bytes = makeStream("ab", "c", "", "def", "", "g");
    MarkableLineParser parser = new MarkableLineParser(bytes);
    parser.lines().forEach(line -> {
      if (line.equals("")) {
        parser.mark();
      }
    });

    int expectedMarkPosition = 6 + LS.length() * 5;

    assertEquals(expectedMarkPosition, parser.getMark());
  }

  private InputStream makeStream(String... lines) throws Exception {
    StringBuilder text = new StringBuilder();

    for (String line : lines) {
      text.append(line);
      text.append(LS);
    }

    ByteBuffer byteBuffer = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(text));
    byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);

    return new ByteArrayInputStream(bytes);
  }
}
