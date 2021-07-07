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
package org.terracotta.common.struct.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Unit;

import java.math.BigInteger;

/**
 * @author Mathieu Carbou
 */
public class StructJsonModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public StructJsonModule() {
    super(StructJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));
    setMixInAnnotation(Measure.class, MeasureMixin.class);
    setMixInAnnotation(org.terracotta.common.struct.Version.class, VersionMixin.class);
  }

  @SuppressWarnings("unused")
  public static abstract class MeasureMixin<T extends Enum<T> & Unit<T>> extends Measure<T> {
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "TIME", value = TimeUnit.class),
        @JsonSubTypes.Type(name = "MEMORY", value = MemoryUnit.class),
    })
    private final T unit;

    @JsonCreator
    protected MeasureMixin(@JsonProperty(value = "quantity", required = true) BigInteger quantity,
                           @JsonProperty(value = "unit", required = true) T unit) {
      super(quantity, unit);
      this.unit = unit;
    }

    @JsonIgnore
    @Override
    public long getQuantity() {
      return super.getQuantity();
    }

    @JsonProperty("quantity")
    @Override
    public BigInteger getExactQuantity() {
      return super.getExactQuantity();
    }
  }

  public static abstract class VersionMixin {
    @JsonCreator
    public static org.terracotta.common.struct.Version valueOf(String version) {
      return org.terracotta.common.struct.Version.valueOf(version);
    }

    @Override
    @JsonValue
    public abstract String toString();
  }
}
