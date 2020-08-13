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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Parses an InputStream into lines whilst also giving the ability to record the position of the byte that was the last
 * newline.
 */
public class MarkableLineParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(MarkableLineParser.class);

  public static final String LS = "\n";
  public static final String CR = "\r";

  private final InputStream input;
  private long position;
  private long mark;

  public MarkableLineParser(InputStream input) {
    this.input = input;
  }

  public Stream<String> lines() {
    return StreamSupport.stream(new LineParsingSpliterator(), false);
  }

  public void mark() {
    mark = position;
  }

  public long getMark() {
    return mark;
  }

  private class LineParsingSpliterator implements Spliterator<String> {
    private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
      try {
        StringBuilder sb = new StringBuilder();

        while (true) {
          Character nextCharacter = readNextCharacter();

          if (nextCharacter == null) {
            return false;
          }

          sb.append(nextCharacter);

          if (endsWithLineSeparator(sb, LS)) {
            String line = withoutLineSeparator(sb);
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("parsed: {}", line.replace("\r", "\\r").replace("\n", "\\n"));
            }
            action.accept(line);
            return true;
          }
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    private Character readNextCharacter() throws IOException {
      ByteBuffer nextBytes = ByteBuffer.allocate(3); // 3 = maxBytesPerChar for UTF-8

      while (true) {
        int b = input.read();
        position++;

        if (b == -1) {
          return null;
        }

        nextBytes.put((byte) b);
        nextBytes.flip();

        CharBuffer nextCharacter = CharBuffer.allocate(1);
        CoderResult decodeResult = decoder.decode(nextBytes, nextCharacter, false);
        if (decodeResult.isError()) {
          decodeResult.throwException();
        }
        if (nextCharacter.position() == 0) {
          nextBytes.compact();
          continue;
        }
        if (decodeResult.isOverflow()) {
          throw new AssertionError("Cannot decode more than one character at a time");
        }

        return nextCharacter.get(0);
      }
    }

    private boolean endsWithLineSeparator(StringBuilder sb, String eol) {
      if (sb.length() < eol.length()) {
        return false;
      }

      char[] lastChars = new char[eol.length()];
      sb.getChars(sb.length() - eol.length(), sb.length(), lastChars, 0);

      String suffix = new String(lastChars);

      return eol.equals(suffix);
    }

    private String withoutLineSeparator(StringBuilder sb) {
      sb.delete(sb.length() - LS.length(), sb.length());
      if (endsWithLineSeparator(sb, CR)) {
        sb.delete(sb.length() - CR.length(), sb.length());
      }
      return sb.toString();
    }

    @Override
    public Spliterator<String> trySplit() {
      return null;
    }

    @Override
    public long estimateSize() {
      return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
      return ORDERED | NONNULL | IMMUTABLE;
    }
  }
}
