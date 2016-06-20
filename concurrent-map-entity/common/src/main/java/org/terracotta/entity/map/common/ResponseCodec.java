/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.entity.map.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


class ResponseCodec {
  public static MapResponse decode(byte[] bytes) throws IOException {
    ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes));
    byte type = input.readByte();

    switch (MapResponse.Type.values()[type]) {
      case NULL:
        return NullResponse.readFrom(input);
      case BOOLEAN:
        return BooleanResponse.readFrom(input);
      case SIZE:
        return SizeResponse.readFrom(input);
      case MAP_VALUE:
        return MapValueResponse.readFrom(input);
      case KEY_SET:
        return KeySetResponse.readFrom(input);
      case VALUE_COLLECTION:
        return ValueCollectionResponse.readFrom(input);
      case ENTRY_SET:
        return EntrySetResponse.readFrom(input);
      default:
        throw new IllegalArgumentException("Unknown map response type " + type);
    }
  }

  public static byte[] encode(MapResponse response) throws IOException {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    ObjectOutputStream output = new ObjectOutputStream(byteOut);

    output.writeByte(response.responseType().ordinal());
    response.writeTo(output);

    output.close();
    return byteOut.toByteArray();
  }
}
