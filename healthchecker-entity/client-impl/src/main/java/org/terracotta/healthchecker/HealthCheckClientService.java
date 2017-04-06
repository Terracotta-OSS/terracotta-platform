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
package org.terracotta.healthchecker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;

@SuppressWarnings("rawtypes")
public class HealthCheckClientService implements EntityClientService<HealthCheck, Properties, HealthCheckReq, HealthCheckRsp, Object>  {
  private final HealthCheckerCodec CODEC = new HealthCheckerCodec();

  @Override
  public boolean handlesEntityType(Class<HealthCheck> cls) {
    return HealthCheck.class.equals(cls);
  }

  @Override
  public byte[] serializeConfiguration(Properties configuration) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      configuration.store(bos, "");
    } catch (IOException ioe) {

    }
    return bos.toByteArray();
  }

  @Override
  public Properties deserializeConfiguration(byte[] configuration) {
    ByteArrayInputStream bis = new ByteArrayInputStream(configuration);
    Properties props = new Properties();
    try {
      props.load(bis);
    } catch (IOException ioe) {

    }
    return props;
  }

  @Override
  public HealthCheck create(EntityClientEndpoint<HealthCheckReq, HealthCheckRsp> endpoint, Object userData) {
    return new HealthCheckerClient(endpoint);
  }

  @Override
  public MessageCodec<HealthCheckReq, HealthCheckRsp> getMessageCodec() {
    return CODEC;
  }
  
}
