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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Scope;

import java.util.Objects;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;

public class Applicability {
  private final Scope level;
  private final String nodeName;
  private final Integer stripeId;

  public static Applicability cluster() {
    return new Applicability(CLUSTER, null, null);
  }

  public static Applicability stripe(int stripeId) {
    return new Applicability(STRIPE, stripeId, null);
  }

  public static Applicability node(int stripeId, String nodeName) {
    return new Applicability(NODE, stripeId, requireNonNull(nodeName));
  }

  protected Applicability(Scope level,
                          Integer stripeId,
                          String nodeName) {
    this.level = requireNonNull(level);
    this.stripeId = stripeId;
    this.nodeName = nodeName;
  }

  public Scope getLevel() {
    return level;
  }

  public String getNodeName() {
    return nodeName;
  }

  public OptionalInt getStripeId() {
    return stripeId == null ? OptionalInt.empty() : OptionalInt.of(stripeId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Applicability)) return false;
    Applicability that = (Applicability) o;
    return getLevel() == that.getLevel() &&
        Objects.equals(getNodeName(), that.getNodeName()) &&
        Objects.equals(getStripeId(), that.getStripeId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLevel(), getNodeName(), getStripeId());
  }

  @Override
  public String toString() {
    return "Applicability{" +
        "scope=" + level +
        ", nodeName='" + nodeName + '\'' +
        ", stripeId='" + stripeId + '\'' +
        '}';
  }
}
