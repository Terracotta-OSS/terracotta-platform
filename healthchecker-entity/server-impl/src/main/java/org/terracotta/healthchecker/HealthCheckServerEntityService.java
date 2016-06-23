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

import com.tc.classloader.PermanentEntity;
import java.util.Collections;
import java.util.Set;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;

/**
 *  annotation must match the type, and version below for this to work
 */
@PermanentEntity(type="org.terracotta.healthchecker.HealthCheck", names={"staticHealthChecker"}, version=1)
public class HealthCheckServerEntityService implements ServerEntityService<HealthCheckReq, HealthCheckRsp> {
  
  private static final ConcurrencyStrategy CONCURRENCY = new ConcurrencyStrategy() {
    @Override
    public int concurrencyKey(EntityMessage message) {
      return ConcurrencyStrategy.UNIVERSAL_KEY;
    }

    @Override
    public Set getKeysForSynchronization() {
      return Collections.emptySet();
    }

  };

  @Override
  public long getVersion() {
    return 1L;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return "org.terracotta.healthchecker.HealthCheck".equals(typeName);
  }

  @Override
  public ActiveServerEntity<HealthCheckReq, HealthCheckRsp> createActiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new HealthCheckerServer();
  }

  @Override
  public PassiveServerEntity<HealthCheckReq, HealthCheckRsp> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new HealthCheckerServer();
  }

  @Override
  public ConcurrencyStrategy<HealthCheckReq> getConcurrencyStrategy(byte[] configuration) {
    return  CONCURRENCY;
  }

  @Override
  public MessageCodec<HealthCheckReq, HealthCheckRsp> getMessageCodec() {
    return new HealthCheckerCodec();
  }

  @Override
  public SyncMessageCodec<HealthCheckReq> getSyncMessageCodec() {
    return null;
  }
  
  
  
}
