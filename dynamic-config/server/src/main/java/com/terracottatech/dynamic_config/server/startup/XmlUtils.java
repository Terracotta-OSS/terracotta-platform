/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.startup;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * Utility functions for interacting with XML structures.
 */
public class XmlUtils {
  private XmlUtils() { }


  /**
   * Escapes characters in a {@code String} to appear as a value in XML element content, attribute value, or
   * CDATA section.
   * @param text the text to process
   * @return an escaped version of {@code text} safe to use as XML element content, attribute value, or CDATA section;
   *    {@code null} if {@code text} is {@code null}
   */
  public static String escapeXml(String text) {
    if (text == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(text.length());
    StringCharacterIterator sci = new StringCharacterIterator(text);
    for (char current = sci.first(); current != CharacterIterator.DONE; current = sci.next()) {
      switch (current) {
        case '<':       // Always needed
        case '&':       // Always needed
        case '"':       // Needed for quote-delimited attribute value
        case '\'':      // Needed for apostrophe-delimited attribute value
        case '>':       // Needed for CDATA section
          sb.append("&#").append((int)current).append(';');
          break;
        default:
          sb.append(current);
      }
    }
    return sb.toString();
  }
}
