/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import com.tc.classloader.PermanentEntity;
import java.util.Collections;
import java.util.Set;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;

/**
 *  annotation must match the type, and version below for this to work
 */
@PermanentEntity(type="org.terracotta.healthchecker.HealthCheck", name="staticHealthChecker", version=1)
public class HealthCheckServerEntityService implements EntityServerService<HealthCheckReq, HealthCheckRsp> {

  private static final ConcurrencyStrategy<HealthCheckReq> CONCURRENCY = new ConcurrencyStrategy<HealthCheckReq>() {
    @Override
    public int concurrencyKey(HealthCheckReq message) {
      return ConcurrencyStrategy.UNIVERSAL_KEY;
    }

    @Override
    public Set<Integer> getKeysForSynchronization() {
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
