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
package org.terracotta.dynamic_config.server.startup;

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
