/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.healthchecker;

import java.nio.charset.Charset;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

/**
 *
 */
public class HealthCheckerCodec implements MessageCodec<HealthCheckReq, HealthCheckRsp> {
  private static final Charset CHARSET = Charset.forName("ASCII");
  
  @Override
  public byte[] encodeMessage(HealthCheckReq message) throws MessageCodecException {
    return message.toString().getBytes(CHARSET);
  }

  @Override
  public HealthCheckReq decodeMessage(byte[] payload) throws MessageCodecException {
    return new HealthCheckReq(new String(payload,CHARSET));
  }

  @Override
  public byte[] encodeResponse(HealthCheckRsp response) throws MessageCodecException {
    return response.toString().getBytes(CHARSET);
  }

  @Override
  public HealthCheckRsp decodeResponse(byte[] payload) throws MessageCodecException {
    return new HealthCheckRsp(new String(payload, CHARSET));
  }
}
