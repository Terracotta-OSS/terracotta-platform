/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * Tests for {@link TimeBudget}.
 * <p>
 * Time-dependent methods are exercised through their {@code now}-accepting overloads so
 * no real time needs to pass.  Tests use a fixed {@code when=0} with
 * {@link TimeBudget#finite(long, long, TimeUnit)} so that the deadline is simply
 * {@code timeUnit.toNanos(timeout)}, making all synthetic {@code now} values
 * fully deterministic without accessing internal fields.
 */
public class TimeBudgetTest {

  // -------------------------------------------------------------------------
  // empty budget — state predicates
  // -------------------------------------------------------------------------

  @Test
  public void empty_isEmpty() {
    assertThat(TimeBudget.EMPTY.isEmpty(), is(true));
  }

  @Test
  public void empty_isNotInfinite() {
    assertThat(TimeBudget.EMPTY.isInfinite(), is(false));
  }

  @Test
  public void empty_isNotFinite() {
    assertThat(TimeBudget.EMPTY.isFinite(), is(false));
  }

  // -------------------------------------------------------------------------
  // empty budget — isDepleted
  // -------------------------------------------------------------------------

  @Test
  public void empty_isAlwaysDepleted() {
    TimeBudget budget = TimeBudget.EMPTY;
    assertThat(budget.isDepleted(Long.MIN_VALUE), is(true));
    assertThat(budget.isDepleted(0L), is(true));
    assertThat(budget.isDepleted(Long.MAX_VALUE), is(true));
  }

  // -------------------------------------------------------------------------
  // empty budget — remaining
  // -------------------------------------------------------------------------

  @Test
  public void empty_remaining_returnsSameInstance() {
    TimeBudget budget = TimeBudget.EMPTY;
    assertThat(budget.remaining(0L), is(budget));
  }

  // -------------------------------------------------------------------------
  // empty budget — toString
  // -------------------------------------------------------------------------

  @Test
  public void empty_toString_describesState() {
    assertThat(TimeBudget.EMPTY.toString(), containsString("empty"));
  }

  // -------------------------------------------------------------------------
  // infinite budget — state predicates
  // -------------------------------------------------------------------------

  @Test
  public void infinite_isNotEmpty() {
    assertThat(TimeBudget.INFINITE.isEmpty(), is(false));
  }

  @Test
  public void infinite_isInfinite() {
    assertThat(TimeBudget.INFINITE.isInfinite(), is(true));
  }

  @Test
  public void infinite_isNotFinite() {
    assertThat(TimeBudget.INFINITE.isFinite(), is(false));
  }

  // -------------------------------------------------------------------------
  // infinite budget — isDepleted
  // -------------------------------------------------------------------------

  @Test
  public void infinite_isNeverDepleted() {
    TimeBudget budget = TimeBudget.INFINITE;
    assertThat(budget.isDepleted(Long.MIN_VALUE), is(false));
    assertThat(budget.isDepleted(0L), is(false));
    assertThat(budget.isDepleted(Long.MAX_VALUE), is(false));
  }

  // -------------------------------------------------------------------------
  // infinite budget — remaining
  // -------------------------------------------------------------------------

  @Test
  public void infinite_remaining_returnsSameInstance() {
    TimeBudget budget = TimeBudget.INFINITE;
    assertThat(budget.remaining(0L), is(budget));
  }

  // -------------------------------------------------------------------------
  // infinite budget — toString
  // -------------------------------------------------------------------------

  @Test
  public void infinite_toString_describesState() {
    assertThat(TimeBudget.INFINITE.toString(), containsString("infinite"));
  }

  // -------------------------------------------------------------------------
  // finite budget — state predicates
  // -------------------------------------------------------------------------

  @Test
  public void finite_isNotEmpty() {
    assertThat(TimeBudget.finite(5, TimeUnit.SECONDS).isEmpty(), is(false));
  }

  @Test
  public void finite_isNotInfinite() {
    assertThat(TimeBudget.finite(5, TimeUnit.SECONDS).isInfinite(), is(false));
  }

  @Test
  public void finite_isFinite() {
    assertThat(TimeBudget.finite(5, TimeUnit.SECONDS).isFinite(), is(true));
  }

  // -------------------------------------------------------------------------
  // finite budget — isDepleted(long now)
  //
  // Use when=0 so deadline = TimeUnit.SECONDS.toNanos(timeout), no internal fields needed.
  // isDepleted(now) == (now - deadlineNanos >= 0)  — safe signed subtraction idiom
  // -------------------------------------------------------------------------

  @Test
  public void finite_notDepletedBeforeDeadline() {
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(10);
    TimeBudget budget = TimeBudget.finite(when, 10, TimeUnit.SECONDS);
    assertThat(budget.isDepleted(when + timeoutNanos - 1), is(false));
  }

  @Test
  public void finite_depletedExactlyAtDeadline() {
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(10);
    TimeBudget budget = TimeBudget.finite(when, 10, TimeUnit.SECONDS);
    assertThat(budget.isDepleted(when + timeoutNanos), is(true));
  }

  @Test
  public void finite_depletedAfterDeadline() {
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(10);
    TimeBudget budget = TimeBudget.finite(when, 10, TimeUnit.SECONDS);
    assertThat(budget.isDepleted(when + timeoutNanos + 1), is(true));
  }

  /**
   * Verifies that a huge-day timeout whose {@code when + timeoutNanos} wraps the signed
   * {@code long} range is handled correctly by the signed-subtraction idiom in
   * {@link TimeBudget#isDepleted(long)}.
   * <p>
   * {@code TimeUnit.DAYS.toNanos(hugeDays)} saturates to {@code Long.MAX_VALUE} (the JDK
   * clamps the overflow), so {@code deadlineNanos = when + Long.MAX_VALUE} wraps to a large
   * negative signed value.  The signed-subtraction check {@code now - deadlineNanos >= 0}
   * is correct as long as {@code now} and {@code deadlineNanos} are within ~292 years of
   * each other — i.e. {@code now} is obtained from values near the deadline, not from
   * a realistic current time that is far from the wrapped deadline.
   */
  @Test
  public void finite_hugeTimeout_signedWrap_depletesBoundaries() {
    long when = 0L;
    long hugeDays = Long.MAX_VALUE / TimeUnit.DAYS.toNanos(1) + 1;
    long saturatedNanos = TimeUnit.DAYS.toNanos(hugeDays); // saturates to Long.MAX_VALUE
    assertThat("toNanos saturates to Long.MAX_VALUE", saturatedNanos, is(Long.MAX_VALUE));

    TimeBudget budget = TimeBudget.finite(when, hugeDays, TimeUnit.DAYS);
    // deadline = when + Long.MAX_VALUE = Long.MIN_VALUE (wrapped)
    long dl = when + saturatedNanos;

    // One nanosecond before the (signed) deadline: not yet depleted.
    assertThat("one ns before deadline", budget.isDepleted(dl - 1), is(false));

    // Exactly at the deadline: depleted.
    assertThat("exactly at deadline", budget.isDepleted(dl), is(true));

    // One nanosecond past the deadline: still depleted.
    assertThat("one ns past deadline", budget.isDepleted(dl + 1), is(true));
  }

  // -------------------------------------------------------------------------
  // finite budget — remaining(long now)
  //
  // Use when=0 so deadline = timeoutNanos; remaining at a given now is (deadline - now).
  // remaining(now) returns a finite TimeBudget with value max(0, deadline - now)
  // in nanoseconds, preserving full precision.
  // -------------------------------------------------------------------------

  @Test
  public void finite_remaining_atStart_equalsFullTimeout() {
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(10);
    TimeBudget budget = TimeBudget.finite(when, 10, TimeUnit.SECONDS);
    TimeBudget rem = budget.remaining(when); // 0 elapsed
    assertThat(rem.isFinite(), is(true));
    assertThat(rem.getTimeoutValue(TimeUnit.NANOSECONDS).getAsLong(), is(timeoutNanos));
  }

  @Test
  public void finite_remaining_halfwayThrough() {
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(10);
    TimeBudget budget = TimeBudget.finite(when, 10, TimeUnit.SECONDS);
    TimeBudget rem = budget.remaining(when + timeoutNanos / 2); // 5 s elapsed
    assertThat(rem.isFinite(), is(true));
    assertThat(rem.getTimeoutValue(TimeUnit.NANOSECONDS).getAsLong(), is(timeoutNanos / 2));
  }

  @Test
  public void finite_remaining_atDeadline_isZero() {
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(10);
    TimeBudget budget = TimeBudget.finite(when, 10, TimeUnit.SECONDS);
    TimeBudget rem = budget.remaining(when + timeoutNanos); // exactly at deadline
    assertThat(rem.isEmpty(), is(false));
    assertThat(rem.isInfinite(), is(false));
    assertThat(rem.getTimeoutValue().getAsLong(), is(0L));
  }

  @Test
  public void finite_remaining_pastDeadline_isClampedToZero() {
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(10);
    TimeBudget budget = TimeBudget.finite(when, 10, TimeUnit.SECONDS);
    TimeBudget rem = budget.remaining(when + timeoutNanos + 1); // 1 ns past deadline
    assertThat(rem.isEmpty(), is(false));
    assertThat(rem.isInfinite(), is(false));
    assertThat(rem.getTimeoutValue().getAsLong(), is(0L));
  }

  // -------------------------------------------------------------------------
  // finite budget — remaining(long now) — full nanosecond precision
  // -------------------------------------------------------------------------

  @Test
  public void finite_remaining_preservesNanoPrecision() {
    // remaining() stores in NANOSECONDS — no unit truncation occurs.
    // A 2s budget queried at its start must report exactly 2_000_000_000 ns.
    long when = 0L;
    TimeBudget budget = TimeBudget.finite(when, 2, TimeUnit.SECONDS);
    TimeBudget rem = budget.remaining(when);
    assertThat(rem.isFinite(), is(true));
    assertThat(rem.getTimeoutValue(TimeUnit.NANOSECONDS).getAsLong(), is(TimeUnit.SECONDS.toNanos(2)));
  }

  @Test
  public void finite_remaining_shouldNotDrainBudget() {
    // remaining() must not truncate to a coarser unit: 4.9 s must not become 4 s.
    long when = 0L;
    TimeBudget budget = TimeBudget.finite(when, 5, TimeUnit.SECONDS);
    long now = when + TimeUnit.MILLISECONDS.toNanos(100); // 100 ms elapsed
    TimeBudget rem = budget.remaining(now);
    assertThat("Remaining budget shouldn't lose time",
      rem.getTimeoutValue(TimeUnit.NANOSECONDS).getAsLong(), is(4_900_000_000L));
  }

  // -------------------------------------------------------------------------
  // finite budget — toString
  // -------------------------------------------------------------------------

  @Test
  public void finite_toString_containsTimeoutAndUnit() {
    String s = TimeBudget.finite(30, TimeUnit.SECONDS).toString();
    assertThat(s, containsString(String.valueOf(TimeUnit.SECONDS.toNanos(30))));
    assertThat(s, containsString("ns"));
  }

  // -------------------------------------------------------------------------
  // finite budget (zero timeout) — state predicates
  // -------------------------------------------------------------------------

  @Test
  public void finite_zero_isNotEmpty() {
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).isEmpty(), is(false));
  }

  @Test
  public void finite_zero_isNotInfinite() {
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).isInfinite(), is(false));
  }

  @Test
  public void finite_zero_isFinite() {
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).isFinite(), is(true));
  }

  // -------------------------------------------------------------------------
  // finite budget (zero timeout) — isDepleted
  // -------------------------------------------------------------------------

  @Test
  public void finite_zero_isDepletedAtAndAfterCreation() {
    long when = 0L;
    TimeBudget budget = TimeBudget.finite(when, 0, TimeUnit.SECONDS);
    // deadline == when (timeout is 0), so depleted at and after creation
    assertThat(budget.isDepleted(when), is(true));
    assertThat(budget.isDepleted(when + 1), is(true));
  }

  @Test
  public void finiteShouldNotExpireInstantly() {
    // System.nanoTime() can return negative values; a negative 'when' must be accepted
    // and isDepleted() must still work correctly using signed arithmetic.
    long when = -10_000_000_000L;
    TimeBudget budget = TimeBudget.finite(when, 20, TimeUnit.SECONDS);
    long now = when + TimeUnit.SECONDS.toNanos(5); // 5 s elapsed, 15 s remain
    assertThat("Budget should not be depleted", budget.isDepleted(now), is(false));
  }

  // -------------------------------------------------------------------------
  // finite budget (zero timeout) — getValue / getWaitValue
  // -------------------------------------------------------------------------

  @Test
  public void finite_zero_getValue_returnsZero() {
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).getTimeoutValue().getAsLong(), is(0L));
  }

  @Test
  public void finite_zero_getWaitValue_returnsZero() {
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).toWaitValue(), is(0L));
  }

  // -------------------------------------------------------------------------
  // finite budget (zero timeout) — toVoltronString
  // -------------------------------------------------------------------------

  @Test
  public void finite_zero_toVoltronString_returnsMinusOne() {
    // A zero finite budget is already depleted; Voltron must not receive "0"
    // (which it would treat as infinite), so "-1" (try-once) is expected instead.
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).toVoltronString(), is("-1"));
  }

  // -------------------------------------------------------------------------
  // finite() factory — validation
  // -------------------------------------------------------------------------

  @Test
  public void finite_factory_nullTimeUnit_throwsNPE() {
    assertThat(
      () -> TimeBudget.finite(1, (TimeUnit) null),
      is(throwing(instanceOf(NullPointerException.class))));
  }

  @Test
  public void finite_factory_negativeTimeout_throwsIAE() {
    assertThat(
      () -> TimeBudget.finite(-1, TimeUnit.SECONDS),
      is(throwing(instanceOf(IllegalArgumentException.class))
        .andMessage(containsString("positive"))));
  }

  @Test
  public void finite_factory_zeroTimeout_doesNotThrow() {
    // zero is a valid (immediately-depleted) finite budget
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).isFinite(), is(true));
  }

  // -------------------------------------------------------------------------
  // parse(long, TimeUnit) factory
  // -------------------------------------------------------------------------

  @Test
  public void parse_negative_returnsEmptyBudget() {
    assertThat(TimeBudget.parse(-1, TimeUnit.SECONDS).isEmpty(), is(true));
  }

  @Test
  public void parse_zero_returnsInfiniteBudget() {
    assertThat(TimeBudget.parse(0, TimeUnit.SECONDS).isInfinite(), is(true));
  }

  @Test
  public void parse_positive_returnsFiniteBudget() {
    assertThat(TimeBudget.parse(5, TimeUnit.SECONDS).isFinite(), is(true));
  }

  // -------------------------------------------------------------------------
  // parse(Duration) factory
  // -------------------------------------------------------------------------

  @Test
  public void parseDuration_negative_returnsEmptyBudget() {
    assertThat(TimeBudget.parse(Duration.ofSeconds(-1)).isEmpty(), is(true));
  }

  @Test
  public void parseDuration_zero_returnsInfiniteBudget() {
    assertThat(TimeBudget.parse(Duration.ZERO).isInfinite(), is(true));
  }

  @Test
  public void parseDuration_positive_returnsFiniteBudget() {
    assertThat(TimeBudget.parse(Duration.ofSeconds(5)).isFinite(), is(true));
  }

  @Test
  public void parseDuration_positive_storesNanos() {
    // parse(Duration) stores the full nanosecond value of the duration
    TimeBudget ct = TimeBudget.parse(Duration.ofSeconds(3));
    assertThat(ct.getTimeoutValue(TimeUnit.NANOSECONDS).getAsLong(),
      is(TimeUnit.SECONDS.toNanos(3)));
  }

  // -------------------------------------------------------------------------
  // toVoltronString()
  //
  // Returns the timeout as a Voltron connection-property string in milliseconds.
  //   empty   →  "-1"
  //   infinite → "0"
  //   finite  → String.valueOf(TimeUnit.NANOSECONDS.toMillis(timeoutNanos))
  // -------------------------------------------------------------------------

  @Test
  public void voltronString_empty_returnsMinusOne() {
    assertThat(TimeBudget.EMPTY.toVoltronString(), is("-1"));
  }

  @Test
  public void voltronString_infinite_returnsZero() {
    assertThat(TimeBudget.INFINITE.toVoltronString(), is("0"));
  }

  @Test
  public void voltronString_finite_seconds_returnsConfiguredMillis() {
    // 5-second finite budget: configured timeout is 5 s = 5000 ms
    assertThat(TimeBudget.finite(5, TimeUnit.SECONDS).toVoltronString(), is("5000"));
  }

  @Test
  public void voltronString_finite_millis_returnsConfiguredMillis() {
    // 2500 ms finite budget
    assertThat(TimeBudget.finite(2500, TimeUnit.MILLISECONDS).toVoltronString(), is("2500"));
  }

  @Test
  public void voltronString_finite_minutes_returnsConfiguredMillis() {
    // 3-minute finite budget: 3 * 60 * 1000 = 180_000 ms.
    assertThat(TimeBudget.finite(3, TimeUnit.MINUTES).toVoltronString(), is("180000"));
  }

  @Test
  public void voltronString_finite_returnsConfiguredValue_notRemainingTime() {
    assertThat(TimeBudget.finite(10, TimeUnit.SECONDS).toVoltronString(), is("10000"));
  }

  @Test
  public void voltronString_finite_depletedRemaining_returnsMinusOne() {
    // When remaining() is called on an expired finite budget it returns a new TimeBudget
    // whose value is 0.  Passing "0" to Voltron would be interpreted as infinite,
    // so toVoltronString() must return "-1" instead.
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(5);
    TimeBudget budget = TimeBudget.finite(when, 5, TimeUnit.SECONDS);
    // Ask for remaining time one nanosecond *past* the deadline — fully depleted.
    TimeBudget depleted = budget.remaining(when + timeoutNanos + 1);
    assertThat("depleted remaining is not empty", depleted.isEmpty(), is(false));
    assertThat("depleted remaining is not infinite", depleted.isInfinite(), is(false));
    assertThat("depleted remaining voltron string", depleted.toVoltronString(), is("-1"));
  }

  @Test
  public void voltronString_finite_exactlyAtDeadline_returnsMinusOne() {
    // remaining() called exactly at the deadline also produces a zero value.
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(5);
    TimeBudget budget = TimeBudget.finite(when, 5, TimeUnit.SECONDS);
    TimeBudget depleted = budget.remaining(when + timeoutNanos);
    assertThat(depleted.toVoltronString(), is("-1"));
  }

  // -------------------------------------------------------------------------
  // toDuration()
  //
  // Voltron sign convention (millisecond precision):
  //   empty                          → Duration.ofMillis(-1)
  //   infinite                       → Duration.ZERO
  //   finite with millis > 0         → Duration.ofMillis(timeoutNanos / 1_000_000)
  //   finite with millis == 0        → Duration.ofMillis(-1)  (depleted or sub-ms)
  // -------------------------------------------------------------------------

  @Test
  public void toDuration_empty_returnsNegativeDuration() {
    assertThat(TimeBudget.EMPTY.toVoltronDuration().isNegative(), is(true));
  }

  @Test
  public void toDuration_infinite_returnsZeroDuration() {
    assertThat(TimeBudget.INFINITE.toVoltronDuration().isZero(), is(true));
  }

  @Test
  public void toDuration_finite_returnsPositiveDuration() {
    assertThat(TimeBudget.finite(5, TimeUnit.SECONDS).toVoltronDuration().isNegative(), is(false));
    assertThat(TimeBudget.finite(5, TimeUnit.SECONDS).toVoltronDuration().isZero(), is(false));
  }

  @Test
  public void toDuration_finite_durationMatchesConfiguredTimeout() {
    // 5 s → Duration.ofMillis(5000)
    Duration d = TimeBudget.finite(5, TimeUnit.SECONDS).toVoltronDuration();
    assertThat(d, is(Duration.ofMillis(5_000)));
  }

  @Test
  public void toDuration_finite_minutes_durationMatchesConfiguredTimeout() {
    // 3 min → Duration.ofMillis(180_000)
    Duration d = TimeBudget.finite(3, TimeUnit.MINUTES).toVoltronDuration();
    assertThat(d, is(Duration.ofMillis(180_000)));
  }

  @Test
  public void toDuration_finite_subMillisecond_returnsMinusOne() {
    // A sub-millisecond budget truncates to 0 ms — must return -1 ms, not zero,
    // to avoid Voltron interpreting 0 as infinite.
    TimeBudget budget = TimeBudget.finite(999_999, TimeUnit.NANOSECONDS);
    assertThat(budget.toVoltronDuration(), is(Duration.ofMillis(-1)));
  }

  @Test
  public void toDuration_finite_depleted_returnsNegativeDuration() {
    // A depleted budget (value 0 from remaining()) must not produce a zero Duration,
    // which would be misread as infinite by Voltron / parse(Duration).
    long when = 0L;
    long timeoutNanos = TimeUnit.SECONDS.toNanos(5);
    TimeBudget budget = TimeBudget.finite(when, 5, TimeUnit.SECONDS);
    TimeBudget depleted = budget.remaining(when + timeoutNanos + 1);
    assertThat("depleted value is 0", depleted.getTimeoutValue().getAsLong(), is(0L));
    assertThat("depleted toDuration is negative", depleted.toVoltronDuration().isNegative(), is(true));
  }

  @Test
  public void toDuration_roundTrip_empty() {
    assertThat(TimeBudget.parse(TimeBudget.EMPTY.toVoltronDuration()).isEmpty(), is(true));
  }

  @Test
  public void toDuration_roundTrip_infinite() {
    assertThat(TimeBudget.parse(TimeBudget.INFINITE.toVoltronDuration()).isInfinite(), is(true));
  }

  @Test
  public void toDuration_roundTrip_finite() {
    TimeBudget original = TimeBudget.finite(10, TimeUnit.SECONDS);
    TimeBudget roundTripped = TimeBudget.parse(original.toVoltronDuration());
    assertThat(roundTripped.isFinite(), is(true));
    assertThat(roundTripped.getTimeoutValue(TimeUnit.SECONDS).getAsLong(), is(10L));
  }
}
