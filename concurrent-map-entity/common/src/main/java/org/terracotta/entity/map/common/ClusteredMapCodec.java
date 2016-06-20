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

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.io.IOException;


public class ClusteredMapCodec implements MessageCodec<MapOperation, MapResponse> {

  @Override
  public byte[] encodeMessage(MapOperation message) throws MessageCodecException {
    try {
      return OperationCodec.encode(message);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }

  @Override
  public MapOperation decodeMessage(byte[] payload) throws MessageCodecException {
    try {
      return OperationCodec.decode(payload);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }

  @Override
  public byte[] encodeResponse(MapResponse response) throws MessageCodecException {
    try {
      return ResponseCodec.encode(response);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }

  @Override
  public MapResponse decodeResponse(byte[] payload) throws MessageCodecException {
    try {
      return ResponseCodec.decode(payload);
    } catch (IOException e) {
      throw new MessageCodecException("something wrong happend", e);
    }
  }
}
