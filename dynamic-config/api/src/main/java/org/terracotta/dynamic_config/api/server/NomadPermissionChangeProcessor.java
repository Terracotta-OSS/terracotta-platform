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
package org.terracotta.dynamic_config.api.server;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.nomad.server.NomadException;

/**
 * Processor which can accept some rules about rejecting or accepting a PREPARE phase based on some server states
 *
 * @author Mathieu Carbou
 */
public interface NomadPermissionChangeProcessor {
  void addCheck(Check check);

  void removeCheck(Check check);

  interface Check {
    /**
     * Validate that we can go forward. If not, throw a NomadException with the reason.
     */
    void check(NodeContext config, DynamicConfigNomadChange change) throws NomadException;
  }
}
