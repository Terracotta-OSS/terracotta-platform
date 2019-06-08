/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

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

  public static final String LS = "\n";

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
    private CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

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

          if (endsWithLineSeparator(sb)) {
            String line = withoutLineSeparator(sb);
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

    private boolean endsWithLineSeparator(StringBuilder sb) {
      if (sb.length() < LS.length()) {
        return false;
      }

      char[] lastChars = new char[LS.length()];
      sb.getChars(sb.length() - LS.length(), sb.length(), lastChars, 0);

      String suffix = new String(lastChars);

      return LS.equals(suffix);
    }

    private String withoutLineSeparator(StringBuilder sb) {
      sb.delete(sb.length() - LS.length(), sb.length());
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
