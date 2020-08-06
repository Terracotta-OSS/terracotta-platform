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
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.terracotta.common.struct.LockContext;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.nomad.server.NomadException;

import static java.lang.String.format;

public class LockAwareNomadChangeProcessor implements NomadChangeProcessor<DynamicConfigNomadChange> {
  private static final String REJECT_MESSAGE = "changes are not allowed as config is locked by '%s (%s)'";

  private final NomadChangeProcessor<DynamicConfigNomadChange> next;

  public LockAwareNomadChangeProcessor(NomadChangeProcessor<DynamicConfigNomadChange> next) {
    this.next = next;
  }

  @Override
  public void validate(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    throwIfNotAllowed(baseConfig, change);

    next.validate(baseConfig, change.unwrap());
  }

  private static void throwIfNotAllowed(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    if (baseConfig != null) {
      LockContext lockContext = baseConfig.getCluster().getConfigurationLockContext().orElse(null);
      if (change instanceof LockAwareDynamicConfigNomadChange) {
        if (lockContext != null) {
          LockAwareDynamicConfigNomadChange lockAwareDynamicConfigNomadChange = (LockAwareDynamicConfigNomadChange)change;
          String tokenFromClient = lockAwareDynamicConfigNomadChange.getLockToken();
          if (!lockContext.getToken().equals(tokenFromClient)) {
            throw new NomadException(format(REJECT_MESSAGE, lockContext.getOwnerName(), lockContext.getOwnerTags()));
          }
        }
      } else {
        if (lockContext != null) {
          throw new NomadException(format(REJECT_MESSAGE, lockContext.getOwnerName(), lockContext.getOwnerTags()));
        }
      }
    }
  }

  @Override
  public void apply(DynamicConfigNomadChange change) throws NomadException {
    next.apply(change.unwrap());
  }
}
