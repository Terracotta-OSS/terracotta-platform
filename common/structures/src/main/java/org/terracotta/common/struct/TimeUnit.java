/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.common.struct;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public enum TimeUnit implements Unit<TimeUnit> {
  MILLISECONDS("ms", BigInteger.ONE),
  SECONDS("s", BigInteger.valueOf(1000)),
  MINUTES("m", BigInteger.valueOf(60 * 1000)),
  HOURS("h", BigInteger.valueOf(60 * 60 * 1000)),
  ;

  private final String shortName;
  private final BigInteger factor;

  TimeUnit(String shortName, BigInteger factor) {
    this.shortName = shortName;
    this.factor = factor;
  }

  @Override
  public String getShortName() {
    return shortName;
  }

  @Override
  public BigInteger convert(BigInteger quantity, TimeUnit unit) {
    if (this == unit) {
      return quantity;
    }
    return quantity.multiply(unit.factor).divide(this.factor);
  }

  @Override
  public TimeUnit getBaseUnit() {
    return SECONDS;
  }

  @Override
  public String toString() {
    return getShortName();
  }

  public long toMillis(long quantity) {
    return MILLISECONDS.convert(quantity, this);
  }

  public long toSeconds(long quantity) {
    return SECONDS.convert(quantity, this);
  }

  public long toMinutes(long quantity) {
    return MINUTES.convert(quantity, this);
  }

  public long toHours(long quantity) {
    return HOURS.convert(quantity, this);
  }

  public static Optional<TimeUnit> from(String shortName) {
    return Arrays.stream(TimeUnit.values()).filter(timeUnit -> timeUnit.shortName.equals(shortName)).findAny();
  }

  public static Optional<TimeUnit> from(java.util.concurrent.TimeUnit jdkTimeUnit) {
    return Stream.of(values()).filter(unit -> unit.name().equals(jdkTimeUnit.name())).findAny();
  }

}