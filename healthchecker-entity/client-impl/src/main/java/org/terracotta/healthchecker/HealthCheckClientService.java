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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;

@SuppressWarnings("rawtypes")
public class HealthCheckClientService implements EntityClientService<HealthCheck, Properties, HealthCheckReq, HealthCheckRsp>  {
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
  public HealthCheck create(EntityClientEndpoint<HealthCheckReq, HealthCheckRsp> endpoint) {
    return new HealthCheckerClient(endpoint);
  }

  @Override
  public MessageCodec<HealthCheckReq, HealthCheckRsp> getMessageCodec() {
    return CODEC;
  }
  
}
