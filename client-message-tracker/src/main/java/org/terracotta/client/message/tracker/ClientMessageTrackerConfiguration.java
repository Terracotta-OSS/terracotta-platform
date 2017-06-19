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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.ServiceConfiguration;

public class ClientMessageTrackerConfiguration implements ServiceConfiguration<ClientMessageTracker> {

  private final String entityIdentifier;
  private final TrackerPolicy trackerPolicy;

  public ClientMessageTrackerConfiguration(String entityIdentifier, TrackerPolicy trackerPolicy) {
    this.entityIdentifier = entityIdentifier;
    this.trackerPolicy = trackerPolicy;
  }

  public TrackerPolicy getTrackerPolicy() {
    return trackerPolicy;
  }

  public String getEntityIdentifier() {
    return entityIdentifier;
  }

  @Override
  public Class<ClientMessageTracker> getServiceType() {
    return ClientMessageTracker.class;
  }
}
