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
package org.terracotta.management.entity.nms.client;

import org.terracotta.management.entity.nms.Nms;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.model.message.Message;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.client.ProxyEntityClientService;

/**
 * @author Mathieu Carbou
 */
public class NmsEntityClientService extends ProxyEntityClientService<NmsEntity, NmsConfig> {
  public NmsEntityClientService() {
    super(NmsEntity.class, Nms.class, NmsConfig.class, new Class<?>[] {Message.class});
    setCodec(new SerializationCodec());
  }
}
