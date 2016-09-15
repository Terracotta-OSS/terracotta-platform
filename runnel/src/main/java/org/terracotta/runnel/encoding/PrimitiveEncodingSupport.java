package org.terracotta.runnel.encoding;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public interface PrimitiveEncodingSupport<T> {

  T int32(String name, int value);

  T int64(String name, long value);

  T string(String name, String value);

  T byteBuffer(String name, ByteBuffer value);

}
