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
package org.terracotta.runnel.utils;

import sun.nio.cs.ThreadLocalCoders;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * A utility class for encoding and decoding {@code String} instances.
 * <p>
 * The encoding supported by this class is a modified UTF encoding based on the encoding
 * described in
 * <a href="http://docs.oracle.com/javase/8/docs/api/java/io/DataInput.html#modified-utf-8">Modified UTF-8</a>.
 * <p>
 * The methods in this class do <b>not</b> handle {@code null} {@code String} values;
 * <p>
 * The following encoded forms are supported:
 * <pre>{@code
 *     byte 0: format designator
 *         0x00: encoded value <= 65535 bytes
 *             byte 1: unsigned short byte length of encoding
 *             byte 3: modified UTF-8 encoded String (< 64K in length)
 *         0x01: encoded value > 65565 bytes, encoded length < String.length * 2
 *             byte 1: long byte length of encoding
 *             byte 9: modified UTF-8 encoded String
 *         0x02: encoded value > 65535 bytes, encoded length >= String.length * 2
 *             byte 1: long byte length of encoding
 *             byte 9: non-encoded String.toCharArray (effectively)
 *         0x03: String.length == encoded value length <= 65535 bytes
 *             byte 1: unsigned short byte length of encoding
 *             byte 3: modified UTF-8 encoded String (< 64K in length)
 *         0x04: String.length == encoded value length > 65535 bytes
 *             byte 1: int byte length of encoding
 *             byte 5: modified UTF-8 encoded String (>= 64K in length)
 * }</pre>
 * Format designators {@code 0x03} and {@code 0x04} support encode/decode optimizations -- when the
 * encoded length is equal to the original string length, the string is composed only of 7-bit ASCII
 * characters which encode by simply casting to {@code byte}.
 *
 * @author Clifford W. Johnson
 * {@code ByteBuffer} capacity is limited to {@code Integer.MAX_VALUE} so the maximum supported
 * length for an encoded {@code String} is less than {@code Integer.MAX_VALUE}.
 */
// PERFORMANCE NOTE:  The methods in this class are organized and sized to promote JIT inlining.
public final class StringTool {
  /**
   * Private niladic constructor to prevent instantiation.
   */
  private StringTool() {
  }

  /**
   * Gets the modified UTF encoded {@code String} from the {@link ByteBuffer} provided.
   * <p>
   * This implementation <b>does not</b> perform the validity checks performed in
   * {@link DataInputStream#readUTF()}.
   *
   * @param buffer the {@code ByteBuffer} from which the string is decoded
   * @return a new {@code String} constructed from the UTF-encode bytes
   * @throws UTFDataFormatException if an error is encountered while decoding the UTF value
   * @throws BufferUnderflowException if {@code buffer} is too small to hold the declared UTF value
   * @throws NullPointerException if {@code buffer} is {@code null}
   */
  public static String getUTF(final ByteBuffer buffer) throws UTFDataFormatException, BufferUnderflowException {
    Objects.requireNonNull(buffer, "buffer");

    final int encodingType = Byte.toUnsignedInt(buffer.get());
    switch (encodingType) {
      case 0x00: {
        /*
         * Modified UTF-8 encoded value no more than 65535 bytes long
         */
        return decodeString(buffer, Short.toUnsignedLong(buffer.getShort()));
      }

      case 0x01: {
        /*
         * Modified UTF-8 encoded value more than 65535 bytes long but less than decoded String length
         */
        final long encodedLength = buffer.getLong();
        if (encodedLength / 3 > Integer.MAX_VALUE) {
          throw new UTFDataFormatException("Encoded length larger than supported: " + encodedLength);
        }
        return decodeString(buffer, encodedLength);
      }

      case 0x02: {
        /*
         * Non-encoded value
         */
        final long encodedLength = buffer.getLong();
        final long charLength = encodedLength / Character.BYTES;
        if (charLength != (int) charLength) {
          throw new UTFDataFormatException("Encoded length larger than supported: " + encodedLength);
        }
        return getCharsAsString(buffer, (int) charLength);
      }

      case 0x03: {
        /*
         * ASCII-only encoded value no more than 65535 bytes long
         */
        return getBytesAsString(buffer, Short.toUnsignedInt(buffer.getShort()));
      }

      case 0x04: {
        /*
         * ASCII-only encoded value more than 65535 bytes long
         */
        return getBytesAsString(buffer, buffer.getInt());
      }

      default:
        throw new UTFDataFormatException("Unexpected encoding type: 0x" + Integer.toHexString(encodingType));
    }
  }

  /**
   * Decodes a modified UTF-8 encoding of a {@code String} beginning at the current position of the
   * {@code ByteBuffer} provided.  For encodings larger than 65,535 bytes, this method begins with a
   * 65,535 character buffer and enlarges it as needed to hold the decoded value.
   *
   * @param buffer the {@code ByteBuffer} containing the encoded bytes
   * @param encodedLength the byte length of the encoding
   * @return a decoded {@code String}
   * @throws UTFDataFormatException if an error is encountered while decoding the UTF value
   * @throws BufferUnderflowException if {@code buffer} is too small to hold the declared UTF value
   */
  public static String decodeString(final ByteBuffer buffer,
                                    final long encodedLength) throws UTFDataFormatException, BufferUnderflowException {

    final int initialArrayLength = (int) encodedLength;
    char[] chars = new char[initialArrayLength];
    int charIndex = -1;
    long remaining = encodedLength;
    while (remaining > 0) {
      charIndex++;

      /*
       * If the decoding array has reached it's capacity, enlarge it.
       */
      if (charIndex == chars.length) {
        chars = enlargeChars(encodedLength, chars);
      }

      int b = Byte.toUnsignedInt(buffer.get());
      int f = b >>> 4;
      if (f < 0x08) {
        // Single-byte character
        chars[charIndex] = (char) b;
        remaining -= 1;
      } else if (f == 0x0E) {
        // Three-byte character
        chars[charIndex] = (char) ((b & 0x0F) << 12 | (buffer.get() & 0x3F) << 6 | buffer.get() & 0x3F);
        remaining -= 3;
      } else if (f >= 0x0C) {
        // Two-byte character
        chars[charIndex] = (char) ((b & 0x1F) << 6 | buffer.get() & 0x3F);
        remaining -= 2;
      } else {
        throw new UTFDataFormatException(String.format("Illegal element: %02x", b));
      }
    }

    if (remaining != 0) {
      throw new UTFDataFormatException(String.format("Decoding string: %d bytes remaining after decoding", remaining));
    }

    return new String(chars, 0, 1 + charIndex);
  }

  /**
   * Copies a {@code code} array into one increased in size by 0.25 * encodedLength.
   *
   * @param encodedLength the encoded length
   * @param chars the source {@code char[]}
   * @return a new, larger {@code char[]} containing the content from {@code chars}
   */
  private static char[] enlargeChars(final long encodedLength, final char[] chars) {
    long newLength = (long) chars.length + (int) ((encodedLength + 3) / 4);
    if (newLength != (int) newLength) {
      // arithmetic overflow
      throw new OutOfMemoryError();
    }
    return Arrays.copyOf(chars, (int) newLength);
  }

  /**
   * Copy non-encoded characters from the {@code ByteBuffer} provided into a new {@code String} instance.
   *
   * @param buffer the {@code ByteBuffer} containing the characters
   * @param charLength the number of characters to copy
   * @return a new {@code String} constructed from the characters in {@code buffer}
   */
  private static String getCharsAsString(final ByteBuffer buffer, final int charLength) {
    final char[] chars = new char[charLength];
    for (int i = 0; i < charLength; i++) {
      chars[i] = buffer.getChar();
    }
    return new String(chars);
  }

  /**
   * Decodes an ASCII-only UTF-8 encoded {@code String} from the {@code ByteBuffer} provided into a new
   * {@code String} instance.
   *
   * @param buffer the {@code ByteBuffer} containing the ASCII bytes
   * @param charLength the number of bytes/characters to copy
   * @return a new {@code String} constructed from the bytes in {@code buffer}
   */
  private static String getBytesAsString(final ByteBuffer buffer, final int charLength) {
    final char[] chars = new char[charLength];
    for (int i = 0; i < charLength; i++) {
      chars[i] = (char) Byte.toUnsignedInt(buffer.get());
    }
    return new String(chars);
  }

  /**
   * Appends the <i>modified</i> UTF-8 representation of a {@code String} provided to the {@link ByteBuffer}
   * provided.  The buffer's position is advanced by the number of bytes required by the modified UTF-8
   * representation (including a type byte and a 2-, 4-, or 8-byte length).
   * <p>
   * This method <b>does not</b> perform the same sanity checks performed by
   * {@link java.io.DataOutput#writeUTF DataOutput.writeUTF} -- the buffer must be adequately sized.
   *
   * @param buffer the {@code ByteBuffer} into which {@code str} is encoded
   * @param str the {@code String} to encode
   * @throws BufferOverflowException if {@code buffer} is too small for the UTF-encoded {@code str}
   * @throws ReadOnlyBufferException if {@code buffer} is read-only
   * @throws NullPointerException if either {@code str} or {@code buffer} is {@code null}
   */
  // PERFORMANCE NOTE: Chained ByteBuffer.put sequences are **intentionally** used in this method;
  // chained calls generate more efficient bytecode than separate statements.
  public static void putUTF(final ByteBuffer buffer,
                            final String str) throws ReadOnlyBufferException, BufferOverflowException {
    Objects.requireNonNull(buffer, "buffer");
    Objects.requireNonNull(str, "str");

    /*
     * Get the raw encoded length of the string to determine the encoding method.
     */
    final long rawEncodedLength = getEncodedLen(str);
    final int strLength = str.length();
    if (rawEncodedLength == strLength) {
      /*
       * Special case -- we have a purely ASCII string so UTF-8 "encoding" is a simple cast
       */
      putASCII(buffer, str, rawEncodedLength, strLength);
      return;

    } else if (rawEncodedLength <= 65535) {
      /*
       * Encoding is not more than 65535 bytes
       */
      buffer.put((byte) 0x00).putShort((short) rawEncodedLength);

    } else {
      if (rawEncodedLength < strLength * 2) {
        /*
         * Encoding is larger than 65535 bytes and smaller than non-encoded String bytes
         */
        buffer.put((byte) 0x01).putLong(rawEncodedLength);

      } else {
        /*
         * Encoding is larger 65535 bytes and larger than non-encoded String bytes;
         * simple copy in String characters.
         */
        putNonEncoded(buffer, str, strLength);
        return;
      }
    }

    /*
     * Encode the string into the buffer.
     */
    if (rawEncodedLength > buffer.remaining()) {
      throw new BufferOverflowException();
    }
    putEncoded(buffer, str, strLength);
  }

  /**
   * Appends the <i>modified</i> UTF-8 representation of a {@code String} to the {@code ByteBuffer} provided.
   * The buffer's position is advanced by the number of bytes required by the modified UTF-8 representation.
   *
   * @param buffer the {@code ByteBuffer} into which {@code str} is encoded
   * @param str the {@code String} to encode
   * @param strLength the length of {@code str}
   * @throws BufferOverflowException if {@code buffer} is too small for the UTF-encoded {@code str}
   * @throws ReadOnlyBufferException if {@code buffer} is read-only
   */
  public static void putEncoded(final ByteBuffer buffer,
                                final String str,
                                final int strLength) throws BufferOverflowException, ReadOnlyBufferException {

    final char[] slice = new char[MAX_SLICE_LENGTH];
    int sz = 0;
    for (int offset = 0; offset < strLength; offset += MAX_SLICE_LENGTH) {
      final int sliceLength = Math.min(MAX_SLICE_LENGTH, strLength - offset);
      str.getChars(offset, offset + sliceLength, slice, 0);
      for (int i = 0; i < sliceLength; i++) {
        final char c = slice[i];
        if (c <= '\u007F' && c != '\u0000') {
          buffer.put((byte) c);
          sz++;
        } else if (c <= '\u07FF') {
          buffer.put((byte) (0xC0 | c >>> 6)).put((byte) (0x80 | (c & 0x3F)));
          sz = sz + 2;
        } else {
          buffer.put((byte) (0xE0 | c >>> 12)).put((byte) (0x80 | ((c >>> 6) & 0x3F))).put((byte) (0x80 | (c & 0x3F)));
          sz = sz + 3;
        }
      }
    }

  }

  /**
   * Appends the <i>modified</i> UTF-8 representation of a {@code String} to the {@code ByteBuffer} provided.
   * The buffer's position is advanced by the number of bytes required by the modified UTF-8 representation.
   * Number of bytes written is returned.
   *
   * @param os the {@code OutputStream} into which {@code str} is encoded
   * @param str the {@code String} to encode
   * @param strLength the length of {@code str}
   * @return size
   * @throws IOException if thereis a problem writing to the underlying stream
   */
  public static int putEncoded(OutputStream os, final String str, final int strLength) throws IOException {
    final char[] slice = new char[MAX_SLICE_LENGTH];
    final byte[] tmp = new byte[(MAX_SLICE_LENGTH + 1) * 3];
    int sz = 0;
    for (int offset = 0; offset < strLength; offset += MAX_SLICE_LENGTH) {
      final int sliceLength = Math.min(MAX_SLICE_LENGTH, strLength - offset);
      str.getChars(offset, offset + sliceLength, slice, 0);
      int tmpCount = 0;
      for (int i = 0; i < sliceLength; i++) {
        final char c = slice[i];
        if (c <= '\u007F' && c != '\u0000') {
          tmp[tmpCount++] = ((byte) c);
        } else if (c <= '\u07FF') {
          tmp[tmpCount++] = (byte) (0xC0 | c >>> 6);
          tmp[tmpCount++] = (byte) (0x80 | (c & 0x3F));
        } else {
          tmp[tmpCount++] = (byte) (0xE0 | c >>> 12);
          tmp[tmpCount++] = (byte) (0x80 | ((c >>> 6) & 0x3F));
          tmp[tmpCount++] = (byte) (0x80 | (c & 0x3F));
        }
      }
      if (tmpCount > 0) {
        os.write(tmp, 0, tmpCount);
        sz = sz + tmpCount;
      }
    }
    return sz;
  }

  /**
   * Appends the non-encoded, byte-by-byte representation of a {@code String} to the {@code ByeBuffer}
   * provided.  The buffer's position is advanced by the number of bytes required by the representation
   * (including the length).  The bytes are <b>not</b> locale-encoded.
   *
   * @param buffer the {@code ByteBuffer} into which {@code str} is encoded
   * @param str the {@code String} to encode
   * @param strLength the length of {@code str}
   * @throws BufferOverflowException if {@code buffer} is too small for the UTF-encoded {@code str}
   * @throws ReadOnlyBufferException if {@code buffer} is read-only
   */
  private static void putNonEncoded(final ByteBuffer buffer,
                                    final String str,
                                    final int strLength) throws BufferOverflowException, ReadOnlyBufferException {

    final int byteLength = strLength * 2;
    buffer.put((byte) 0x02).putLong(byteLength);
    if (byteLength > buffer.remaining()) {
      throw new BufferOverflowException();
    }

    final char[] slice = new char[MAX_SLICE_LENGTH];

    for (int offset = 0; offset < strLength; offset += MAX_SLICE_LENGTH) {
      final int sliceLength = Math.min(MAX_SLICE_LENGTH, strLength - offset);
      str.getChars(offset, offset + sliceLength, slice, 0);
      for (int i = 0; i < sliceLength; i++) {
        buffer.putChar(slice[i]);
      }
    }
  }

  /**
   * Appends the <i>modified</i> UTF-8 representation of a {@code String} known to be all 7-bit ASCII
   * to the {@code ByteBuffer} provided.  The buffer's position is advanced by the number of bytes
   * required by the modified UTF-8 representation (including either a 2- or 4-byte length).
   *
   * @param buffer the {@code ByteBuffer} into which {@code str} is encoded
   * @param str the {@code String} to encode
   * @param rawEncodedLength the encoded length (as calculated by {@link #getEncodedLen(String)}
   * @param strLength the length of {@code str}
   * @throws BufferOverflowException if {@code buffer} is too small for the UTF-encoded {@code str}
   * @throws ReadOnlyBufferException if {@code buffer} is read-only
   */
  private static void putASCII(final ByteBuffer buffer,
                               final String str,
                               final long rawEncodedLength,
                               final int strLength) throws BufferOverflowException, ReadOnlyBufferException {

    if (rawEncodedLength <= 65535) {
      buffer.put((byte) 0x03).putShort((short) rawEncodedLength);
    } else {
      buffer.put((byte) 0x04).putInt((int) rawEncodedLength);
    }
    if (rawEncodedLength > buffer.remaining()) {
      throw new BufferOverflowException();
    }

    for (int i = 0; i < strLength; i++) {
      buffer.put((byte) str.charAt(i));
    }
  }

  /**
   * Calculates the number of bytes into which a {@code String} can be encoded as modified UTF.
   *
   * @param str the {@code String} for which the length is to be calculated
   * @return the length of {@code str} encoded as modified UTF
   * @throws NullPointerException if {@code str} is {@code null}
   * @throws IllegalStateException if the calculated length is greater than {@code Integer.MAX_VALUE}
   */
  public static int getLengthAsUTF(final String str) {
    Objects.requireNonNull(str, "str");

    long len = getEncodedLen(str);
    final int strLength = str.length();
    if (len == strLength) {
      /*
       * Special ASCII-only cases
       */
      len += 1 + (len <= 65535 ? 2 : 4);
    } else if (len <= 65535) {
      /*
       * "short" encoding
       */
      len += 3;
    } else if (len < strLength * 2) {
      /*
       * "long" encoding where encoding is shorter than chars
       */
      len += 9;
    } else {
      /*
       * "long" non-encoded
       */
      len = strLength * 2;
      len += 9;
    }

    if (len > Integer.MAX_VALUE) {
      throw new IllegalStateException("Encoded length greater than Integer.MAX_VALUE: " + len);
    }
    return (int) len;
  }

  /**
   * Calculates the length of a {@code String} when encoded using modified UTF-8.
   *
   * @param str the {@code String} to calculate
   * @return the byte length of the raw encoding
   */
  @SuppressWarnings( { "StatementWithEmptyBody", "UnnecessarySemicolon" })
  private static long getEncodedLen(final String str) {
    // Performing the "ASCII" checks with an "empty if" improves the performance of
    // ASCII counting by as much as 70%.
    final int strLength = str.length();
    long len = strLength;                               // at least one byte per char
    for (int i = 0; i < strLength; i++) {
      final char c = str.charAt(i);
      if (c < 0x0080 && c != 0) {
        ; // already counted
      } else if (c < 0x0800) {
        len++;                                          // One byte more; 2 bytes total
      } else {
        len += 2;                                       // Two more bytes; 3 bytes total
      }
    }
    return len;
  }

  /**
   * Largest character slice from a {@code String} being "put".
   */
  private static final int MAX_SLICE_LENGTH = 512;

  /**
   * Return the worst case size needed to store a string.
   *
   * @param str string
   * @return size
   */
  public static int worstCaseByteArraySize(String str) {
    return str.length() * 4 + 8;
  }

  public static String attemptDecodeAsAscii(ByteBuffer binary, int probeSize) {
    int start = binary.position();
    boolean utfSeen = false;
    int passes = Math.min(binary.remaining() / Long.BYTES, probeSize / Long.BYTES);
    if (passes > 0) {
      for (int i = start; !utfSeen && passes-- > 0; i+= Long.BYTES) {
        long val = binary.getLong(i);
        utfSeen = (val & 0x8080808080808080L) != 0L;
      }
    }
    if (!utfSeen) {
      try {
        return ThreadLocalCoders.decoderFor(StandardCharsets.US_ASCII)
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                                .decode(binary)
                                .toString();
      } catch (CharacterCodingException e) {
        binary.position(start);
      }
    }
    return null;
  }
}
