/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

import java.nio.charset.Charset;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

/**
 *
 * @author mscott
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
