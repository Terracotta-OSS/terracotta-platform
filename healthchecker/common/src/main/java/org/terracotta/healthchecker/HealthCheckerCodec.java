/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.healthchecker;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class HealthCheckerCodec implements MessageCodec<HealthCheckReq, HealthCheckRsp> {
  private static final Charset CHARSET = StandardCharsets.US_ASCII;

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
