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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.AVAILABILITY;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.CONSISTENCY;

/**
 * @author Mathieu Carbou
 */
public class FailoverPriority {
  static final String ERR_MSG = Setting.FAILOVER_PRIORITY + " should be either 'availability', 'consistency'," +
      " or 'consistency:N' (where 'N' is the voter count expressed as a non-negative integer)";

  private final Type type;
  private final int voters;

  public enum Type {AVAILABILITY, CONSISTENCY}

  private FailoverPriority(Type type, Integer voters) {
    this.type = type;
    this.voters = voters;
  }

  public Type getType() {
    return type;
  }

  public int getVoters() {
    return voters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FailoverPriority)) return false;
    FailoverPriority that = (FailoverPriority) o;
    return getVoters() == that.getVoters() &&
        getType() == that.getType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getVoters());
  }

  @JsonValue
  @Override
  public String toString() {
    return type == AVAILABILITY ? AVAILABILITY.name().toLowerCase() :
        voters == 0 ? CONSISTENCY.name().toLowerCase() :
            (CONSISTENCY.name().toLowerCase() + ":" + voters);
  }

  public static FailoverPriority availability() {
    return new FailoverPriority(AVAILABILITY, 0);
  }

  public static FailoverPriority consistency() {
    return new FailoverPriority(CONSISTENCY, 0);
  }

  public static FailoverPriority consistency(int voters) {
    if (voters < 0) {
      throw new IllegalArgumentException(ERR_MSG);
    }
    return new FailoverPriority(Type.CONSISTENCY, voters);
  }

  @JsonCreator
  public static FailoverPriority valueOf(String str) {
    requireNonNull(str);
    if ("availability".equals(str)) {
      return availability();
    }
    if ("consistency".equals(str)) {
      return consistency();
    }
    if (str.startsWith("consistency:")) {
      try {
        return consistency(Integer.parseInt(str.substring(12)));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(ERR_MSG);
      }
    }
    throw new IllegalArgumentException(ERR_MSG);

  }
}