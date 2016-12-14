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
package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyMessageCodec;

/**
 * @author Mathieu Carbou
 */
class DelegatingSyncMessageCodec implements SyncMessageCodec<ProxyEntityMessage> {

  private final ProxyMessageCodec messageCodec;

  public DelegatingSyncMessageCodec(ProxyMessageCodec messageCodec) {
    this.messageCodec = messageCodec;
  }

  public void setCodec(Codec codec) {
    this.messageCodec.setCodec(codec);
  }

  @Override
  public byte[] encode(int concurrencyKey, ProxyEntityMessage message) throws MessageCodecException {
    return messageCodec.encodeMessage(message);
  }

  @Override
  public ProxyEntityMessage decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
    return messageCodec.decodeMessage(payload);
  }

}
