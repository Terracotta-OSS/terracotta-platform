/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.startup;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.IntStream;

import static org.terracotta.dynamic_config.server.startup.XmlUtils.escapeXml;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link XmlUtils} methods.
 */
public class XmlUtilsTest {

  @Test
  public void testEscapeXmlNull() {
    assertThat(escapeXml(null), is(nullValue()));
  }

  @Test
  public void testEscapeXmlEmpty() {
    assertThat(escapeXml(""), is(""));
  }

  @Test
  public void testEscapeXmlAmpersand() {
    assertThat(escapeXml("&"), is("&#38;"));
    assertThat(escapeXml("&&&&"), is("&#38;&#38;&#38;&#38;"));
  }

  @Test
  public void testEscapeXmlLessThan() {
    assertThat(escapeXml("<"), is("&#60;"));
    assertThat(escapeXml("<<<<"), is("&#60;&#60;&#60;&#60;"));
  }

  @Test
  public void testEscapeXmlGreaterThan() {
    assertThat(escapeXml(">"), is("&#62;"));
    assertThat(escapeXml(">>>>"), is("&#62;&#62;&#62;&#62;"));
  }

  @Test
  public void testEscapeXmlQuote() {
    assertThat(escapeXml("\""), is("&#34;"));
    assertThat(escapeXml("\"\"\"\""), is("&#34;&#34;&#34;&#34;"));
  }

  @Test
  public void testEscapeXmlApostrophe() {
    assertThat(escapeXml("'"), is("&#39;"));
    assertThat(escapeXml("''''"), is("&#39;&#39;&#39;&#39;"));
  }

  @Test
  public void testEscapeXmlMore() {
    int maxStringLength = 4096;
    Map<Integer, String> reservedCharMap = new LinkedHashMap<>();
    reservedCharMap.put((int)'&', "&#38;");
    reservedCharMap.put((int)'<', "&#60;");
    reservedCharMap.put((int)'>', "&#62;");
    reservedCharMap.put((int)'"', "&#34;");
    reservedCharMap.put((int)'\'', "&#39;");
    List<Map.Entry<Integer, String>> reservedChars = new ArrayList<>(reservedCharMap.entrySet());

    long seed = new Random().nextLong();
    Random rnd = new Random(seed);

    // Legal XML characters
    // Char    ::=    #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
    PrimitiveIterator.OfInt xmlChars = IntStream.generate(() -> rnd.nextInt(Character.MAX_CODE_POINT))
        .filter(i -> i == 0x9 || i == 0xA || i == 0xD || (i >= 0x20 && i <= 0xD7FF) || (i >= 0xE000 && i <= 0xFFFD) || (i >= 0x10000 && i <= 0x10FFFF))
        .iterator();

    StringBuilder original = new StringBuilder(maxStringLength);
    StringBuilder encoded = new StringBuilder(maxStringLength);
    for (int i = 0; i < 8; i++) {

      if (i != 0) {
        Collections.shuffle(reservedChars);
        Map.Entry<Integer, String> reservedChar = reservedChars.get(0);
        original.appendCodePoint(reservedChar.getKey());
        encoded.append(reservedChar.getValue());
      }

      int fragmentLength = rnd.nextInt(maxStringLength / 8);
      for (int j = 0; j < fragmentLength; j++) {
        int codePoint = xmlChars.nextInt();
        original.appendCodePoint(codePoint);
        String encoding = reservedCharMap.get(codePoint);
        if (encoding == null) {
          encoded.appendCodePoint(codePoint);
        } else {
          encoded.append(encoding);
        }
      }
    }

    assertThat("Seed " + seed + " failed", escapeXml(original.toString()), is(encoded.toString()));
  }
}