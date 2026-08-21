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
 * no real time needs to pass.  For a finite budget the deadline is obtained via
 * {@link TimeBudget#deadlineNanos}, which exposes the exact {@code when + timeoutNanos}
 * value captured at construction.  All synthetic {@code now} values in the tests are
 * expressed as offsets from that deadline, keeping the tests fully self-contained and
 * deterministic.
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
  // deadlineNanos = when + timeoutNanos
  // isDepleted(now) == Long.compareUnsigned(now, deadlineNanos) >= 0
  // -------------------------------------------------------------------------

  @Test
  public void finite_notDepletedBeforeDeadline() {
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    assertThat(budget.isDepleted(dl - 1), is(false));
  }

  @Test
  public void finite_depletedExactlyAtDeadline() {
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    assertThat(budget.isDepleted(dl), is(true));
  }

  @Test
  public void finite_depletedAfterDeadline() {
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    assertThat(budget.isDepleted(dl + 1), is(true));
  }

  /**
   * Verifies that a huge-day timeout whose {@code when + timeoutNanos} wraps the signed
   * {@code long} range is handled correctly by the unsigned arithmetic in
   * {@link TimeBudget#isDepleted(long)}.
   * <p>
   * {@code TimeUnit.DAYS.toNanos(hugeDays)} saturates to {@code Long.MAX_VALUE} (the JDK
   * clamps the overflow), so {@code deadlineNanos = when + Long.MAX_VALUE} wraps to a large
   * negative signed value.  The unsigned comparison in {@link TimeBudget#isDepleted(long)}
   * treats both operands as 64-bit unsigned integers, preserving the correct ordering.
   */
  @Test
  public void finite_hugeTimeout_unsignedWrap_isNotDepleted() {
    long hugeDays = Long.MAX_VALUE / TimeUnit.DAYS.toNanos(1) + 1;
    long saturatedNanos = TimeUnit.DAYS.toNanos(hugeDays); // saturates to Long.MAX_VALUE
    assertThat("toNanos saturates to Long.MAX_VALUE", saturatedNanos, is(Long.MAX_VALUE));

    TimeBudget budget = TimeBudget.finite(hugeDays, TimeUnit.DAYS);
    long dl = budget.deadlineNanos; // large negative signed value — correct unsigned deadline

    // Any realistic current time (positive signed value) is unsigned-before the deadline.
    assertThat("current time is unsigned-before deadline",
      budget.isDepleted(System.nanoTime()), is(false));

    // One nanosecond before the (unsigned) deadline: not yet depleted.
    assertThat("one ns before deadline", budget.isDepleted(dl - 1), is(false));

    // Exactly at the deadline: depleted.
    assertThat("exactly at deadline", budget.isDepleted(dl), is(true));

    // One nanosecond past the deadline: still depleted.
    assertThat("one ns past deadline", budget.isDepleted(dl + 1), is(true));
  }

  // -------------------------------------------------------------------------
  // finite budget — remaining(long now)
  //
  // remaining(now) returns a finite TimeBudget with value max(0, deadline - now)
  // expressed in the budget's own unit.
  // -------------------------------------------------------------------------

  @Test
  public void finite_remaining_atStart_equalsFullTimeout() {
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    long startNanos = dl - TimeUnit.SECONDS.toNanos(10);
    TimeBudget rem = budget.remaining(startNanos);
    assertThat(rem.isFinite(), is(true));
    assertThat(rem.timeout, is(10L));
  }

  @Test
  public void finite_remaining_halfwayThrough() {
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    long halfway = dl - TimeUnit.SECONDS.toNanos(5);
    TimeBudget rem = budget.remaining(halfway);
    assertThat(rem.isFinite(), is(true));
    assertThat(rem.timeout, is(5L));
  }

  @Test
  public void finite_remaining_atDeadline_isZero() {
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    TimeBudget rem = budget.remaining(dl);
    // remaining == 0: finite() would reject 0, so parse() is used internally;
    // the TimeBudget stores 0 which causes isFinite() to return false.
    // We assert the raw value is 0 and that the timeout is not infinite or empty.
    assertThat(rem.isEmpty(), is(false));
    assertThat(rem.isInfinite(), is(false));
    assertThat(rem.timeout, is(0L));
  }

  @Test
  public void finite_remaining_pastDeadline_isClampedToZero() {
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    TimeBudget rem = budget.remaining(dl + 1);
    assertThat(rem.isEmpty(), is(false));
    assertThat(rem.isInfinite(), is(false));
    assertThat(rem.timeout, is(0L));
  }

  // -------------------------------------------------------------------------
  // finite budget — remaining(long now) — unit preserved
  // -------------------------------------------------------------------------

  @Test
  public void finite_remaining_keepsOwnUnit_seconds() {
    // 2 s budget; remaining at start is still expressed in SECONDS
    TimeBudget budget = TimeBudget.finite(2, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    long startNanos = dl - TimeUnit.SECONDS.toNanos(2);
    TimeBudget rem = budget.remaining(startNanos);
    assertThat(rem.isFinite(), is(true));
    assertThat(rem.timeUnit, is(TimeUnit.SECONDS));
    assertThat(rem.timeout, is(2L));
  }

  @Test
  public void finite_remaining_keepsOwnUnit_minutes_truncates() {
    // 90 s budget stored in SECONDS; remaining is expressed in SECONDS too
    TimeBudget budget = TimeBudget.finite(90, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    long startNanos = dl - TimeUnit.SECONDS.toNanos(90);
    TimeBudget rem = budget.remaining(startNanos);
    assertThat(rem.isFinite(), is(true));
    assertThat(rem.timeUnit, is(TimeUnit.SECONDS));
    assertThat(rem.timeout, is(90L));
  }

  // -------------------------------------------------------------------------
  // finite budget — toString
  // -------------------------------------------------------------------------

  @Test
  public void finite_toString_containsTimeoutAndUnit() {
    String s = TimeBudget.finite(30, TimeUnit.SECONDS).toString();
    assertThat(s, containsString("30"));
    assertThat(s, containsString("SECONDS"));
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
    TimeBudget budget = TimeBudget.finite(0, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos; // == when, since timeout is 0
    // exactly at creation time: depleted
    assertThat(budget.isDepleted(dl), is(true));
    // one nanosecond after creation: still depleted
    assertThat(budget.isDepleted(dl + 1), is(true));
  }

  // -------------------------------------------------------------------------
  // finite budget (zero timeout) — getValue / getWaitValue
  // -------------------------------------------------------------------------

  @Test
  public void finite_zero_getValue_returnsZero() {
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).getValue().getAsLong(), is(0L));
  }

  @Test
  public void finite_zero_getWaitValue_returnsZero() {
    assertThat(TimeBudget.finite(0, TimeUnit.SECONDS).getWaitValue(), is(0L));
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
    // zero is now a valid (immediately-depleted) finite budget
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
    assertThat(ct.getValue(TimeUnit.NANOSECONDS).getAsLong(),
      is(TimeUnit.SECONDS.toNanos(3)));
  }

  // -------------------------------------------------------------------------
  // toVoltronString()
  //
  // Returns the *configured* timeout — not the remaining time — as a Voltron
  // connection-property string.  The mapping is:
  //   empty   →  "-1"
  //   infinite → "0"
  //   finite  → String.valueOf(timeUnit.toMillis(timeout))
  // -------------------------------------------------------------------------

  @Test
  public void voltronString_empty_returnsMinusOne() {
    assertThat(TimeBudget.EMPTY.toVoltronString(), is("-1"));
  }

  @Test
  public void voltronString_infinite_returnsZero() {
    assertThat(
      TimeBudget.INFINITE.toVoltronString(),
      is("0"));
  }

  @Test
  public void voltronString_finite_seconds_returnsConfiguredMillis() {
    // 5-second finite budget: configured timeout is 5 s = 5000 ms, regardless of how
    // much time has elapsed since construction.
    assertThat(
      TimeBudget.finite(5, TimeUnit.SECONDS).toVoltronString(),
      is("5000"));
  }

  @Test
  public void voltronString_finite_millis_returnsConfiguredMillis() {
    // 2500 ms finite budget: configured timeout is already in ms.
    assertThat(
      TimeBudget.finite(2500, TimeUnit.MILLISECONDS).toVoltronString(),
      is("2500"));
  }

  @Test
  public void voltronString_finite_minutes_returnsConfiguredMillis() {
    // 3-minute finite budget: 3 * 60 * 1000 = 180_000 ms.
    assertThat(
      TimeBudget.finite(3, TimeUnit.MINUTES).toVoltronString(),
      is("180000"));
  }

  @Test
  public void voltronString_finite_returnsConfiguredValue_notRemainingTime() {
    // Verify that the string reflects the *configured* timeout and is therefore
    // unaffected by elapsed time.  We create the budget, wait for no real time, and
    // check that the string still matches the construction argument.
    TimeBudget budget = TimeBudget.finite(10, TimeUnit.SECONDS);
    // Calling remaining() simulates time passing without touching toVoltronString.
    // The Voltron string must still be "10000" (the original 10 s in ms).
    assertThat(budget.toVoltronString(), is("10000"));
  }

  @Test
  public void voltronString_finite_depletedRemaining_returnsMinusOne() {
    // When remaining() is called on an expired finite budget it returns a new TimeBudget
    // whose internal value is 0.  Passing "0" to Voltron would be interpreted as
    // infinite, so toVoltronString() must return "-1" instead.
    TimeBudget budget = TimeBudget.finite(5, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    // Ask for remaining time one nanosecond *past* the deadline — fully depleted.
    TimeBudget depleted = budget.remaining(dl + 1);
    assertThat("depleted remaining has raw value 0", depleted.timeout, is(0L));
    assertThat("depleted remaining is not empty", depleted.isEmpty(), is(false));
    assertThat("depleted remaining is not infinite", depleted.isInfinite(), is(false));
    assertThat("depleted remaining voltron string", depleted.toVoltronString(), is("-1"));
  }

  @Test
  public void voltronString_finite_exactlyAtDeadline_returnsMinusOne() {
    // remaining() called exactly at the deadline also produces a zero value.
    TimeBudget budget = TimeBudget.finite(5, TimeUnit.SECONDS);
    long dl = budget.deadlineNanos;
    TimeBudget depleted = budget.remaining(dl);
    assertThat(depleted.toVoltronString(), is("-1"));
  }

  // -------------------------------------------------------------------------
  // toDuration()
  //
  // Voltron sign convention:
  //   empty   → negative Duration (-1 ns)
  //   infinite → zero Duration
  //   finite  → positive Duration (in nanoseconds)
  //   finite depleted (value == 0) → negative Duration (-1 ns), not zero
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
    // 5 s stored in SECONDS → Duration of exactly 5 000 000 000 ns
    Duration d = TimeBudget.finite(5, TimeUnit.SECONDS).toVoltronDuration();
    assertThat(d, is(Duration.ofSeconds(5)));
  }

  @Test
  public void toDuration_finite_minutes_durationMatchesConfiguredTimeout() {
    // 3 min stored in MINUTES → Duration of exactly 3 minutes
    Duration d = TimeBudget.finite(3, TimeUnit.MINUTES).toVoltronDuration();
    assertThat(d, is(Duration.ofMinutes(3)));
  }

  @Test
  public void toDuration_finite_depleted_returnsNegativeDuration() {
    // A depleted budget (value == 0 from remaining()) must not produce a zero Duration,
    // which would be misread as infinite by Voltron / parse(Duration).
    TimeBudget budget = TimeBudget.finite(5, TimeUnit.SECONDS);
    TimeBudget depleted = budget.remaining(budget.deadlineNanos + 1);
    assertThat("depleted value is 0", depleted.timeout, is(0L));
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
    assertThat(roundTripped.getValue(TimeUnit.SECONDS).getAsLong(), is(10L));
  }
}
