package org.terracotta.runnel.decoding;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public interface PrimitiveDecodingSupport {

  Integer int32(String name);

  Long int64(String name);

  String string(String name);

  ByteBuffer byteBuffer(String name);

}
