/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.entity.helloworld.server;

import org.terracotta.management.entity.helloworld.HelloWorld;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyServerEntityService;

/**
 * @author Mathieu Carbou
 */
public class HelloWorldEntityServerService extends ProxyServerEntityService<Void> {
  public HelloWorldEntityServerService() {
    super(HelloWorld.class, Void.class, new SerializationCodec());
  }

  @Override
  public HelloWorldEntity createActiveEntity(ServiceRegistry registry, Void config) {
    return new HelloWorldEntity();
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return "org.terracotta.management.entity.helloworld.client.HelloWorldEntity".equals(typeName);
  }

}
