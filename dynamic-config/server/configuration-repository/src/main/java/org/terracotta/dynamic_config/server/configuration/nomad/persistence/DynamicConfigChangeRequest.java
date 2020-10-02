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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.nomad.server.ChangeRequestState;

import java.time.Instant;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigChangeRequest extends ChangeRequest<NodeContext> {
  public DynamicConfigChangeRequest(ChangeRequestState state, long version, String prevChangeId, NomadChange change, NodeContext changeResult, String creationHost, String creationUser, Instant creationTimestamp) {
    super(state, version, prevChangeId, change, changeResult, creationHost, creationUser, creationTimestamp);
  }
}
