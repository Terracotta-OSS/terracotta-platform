/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static com.terracottatech.persistence.sanskrit.MarkableLineParser.LS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
