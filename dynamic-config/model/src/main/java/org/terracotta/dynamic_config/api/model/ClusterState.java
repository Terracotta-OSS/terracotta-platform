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
package org.terracotta.dynamic_config.api.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static org.terracotta.dynamic_config.api.model.Operation.GET;
import static org.terracotta.dynamic_config.api.model.Operation.IMPORT;
import static org.terracotta.dynamic_config.api.model.Operation.SET;
import static org.terracotta.dynamic_config.api.model.Operation.UNSET;

/**
 * @author Mathieu Carbou
 */
public enum ClusterState {

  /**
   * Represents a node that has been activated and is able to become active or passive. It has a persisted config folder on disk.
   */
  ACTIVATED(GET, SET, UNSET),

  /**
   * Represents a node that is in configuring state. The node has started in diagnostic mode, either because this is a new node
   * that has just been created and it is pending activation, or because this is an already activated node that has been started
   * with -repair-mode (to be repaired)
   */
  CONFIGURING(GET, SET, UNSET, IMPORT);

  private final Collection<Operation> supportedOperations;

  ClusterState(Operation... operations) {
    this.supportedOperations = Arrays.asList(operations);
  }

  public Collection<Operation> getOperations() {
    return Collections.unmodifiableCollection(supportedOperations);
  }

  public Stream<Operation> filter(Operation... superset) {
    return Stream.of(superset).filter(supportedOperations::contains);
  }

  public boolean supports(Operation operation) {
    return supportedOperations.contains(operation);
  }

  @Override
  public String toString() {
    return "node is " + name().toLowerCase();
  }
}
