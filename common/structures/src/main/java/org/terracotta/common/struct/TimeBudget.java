/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;

/**
 * Represents a Voltron connection timeout with three possible states:
 * <ul>
 *   <li><b>finite</b>   — a positive timeout; the timeout expires after the given duration.</li>
 *   <li><b>infinite</b> — a zero timeout; the timeout never expires.</li>
 *   <li><b>empty</b>    — a negative timeout; the operation should be attempted exactly once
 *       without consulting the remaining time.</li>
 * </ul>
 *
 * <p>Use the constants or factory methods to obtain instances:
 * <ul>
 *   <li>{@link #EMPTY} — singleton for the "try-once, no retry" timeout.</li>
 *   <li>{@link #INFINITE} — singleton for the unbounded timeout.</li>
 *   <li>{@link #finite(long, TimeUnit)} — for a bounded timeout.</li>
 *   <li>{@link #parse(long, TimeUnit)} — to pick the appropriate variant based on the sign of a
 *       caller-supplied timeout value.</li>
 * </ul>
 *
 * <p>The start time for finite budgets is captured at construction using {@link System#nanoTime()}.
 * All {@code now}-accepting overloads expect a value in <b>nanoseconds</b> obtained from
 * {@code System.nanoTime()}.
 *
 * <p>Internally all durations are stored in nanoseconds. The {@code TimeUnit} supplied to factory
 * methods is used only at input (to convert the caller's value to nanoseconds) and at output
 * (via {@link #getTimeoutValue(TimeUnit)}, {@link #toVoltronString()}, etc.).
 *
 * <p>This class helps translate the application's timeout (and remaining timeout) to the different
 * Voltron possible values (-1 for try once, 0 for infinite, and a positive value for a finite
 * timeout), also working around the fact that a depleted timeout budget going down to 0 MUST NOT
 * be translated to a Voltron timeout of 0 (infinite) but -1 instead.
 */
public class TimeBudget {
  /**
   * Singleton representing the <em>empty</em> timeout state: the operation is attempted exactly
   * once with no blocking wait. Equivalent to a Voltron timeout of {@code -1}.
   */
  public static final TimeBudget EMPTY = new TimeBudget(0, 0);

  /**
   * Singleton representing the <em>infinite</em> timeout state: the operation waits indefinitely
   * and never times out. Equivalent to a Voltron timeout of {@code 0}.
   */
  public static final TimeBudget INFINITE = new TimeBudget(0, 0) {};

  /**
   * The timeout duration in nanoseconds. Only meaningful for finite budgets; set to {@code 0L}
   * for empty/infinite instances.
   */
  private final long timeoutNanos;

  /**
   * Absolute deadline in nanoseconds ({@code when + timeoutNanos}).  Only meaningful for
   * finite budgets; set to {@code 0L} for empty/infinite instances.
   * <p>
   * Plain wrapping addition is used intentionally.  {@link #isDepleted(long)} checks
   * expiry via {@code now - deadlineNanos >= 0}, which is the JDK-recommended idiom for
   * comparing {@link System#nanoTime()} values: signed subtraction is correct across
   * wrap-around as long as the elapsed duration is within ~292 years.
   */
  private final long deadlineNanos;

  private TimeBudget(long timeoutNanos, long deadlineNanos) {
    this.timeoutNanos = timeoutNanos;
    this.deadlineNanos = deadlineNanos;
  }

  /**
   * Returns {@code true} if this timeout is <em>empty</em>, i.e. it was created with a
   * negative timeout value.  An empty timeout signals that the caller should attempt the
   * operation exactly once and not check or wait on the remaining time.
   *
   * @return {@code true} for an empty timeout
   */
  public boolean isEmpty() {
    return this == EMPTY;
  }

  /**
   * Returns {@code true} if this timeout is <em>infinite</em>, i.e. it was created with a
   * zero timeout value.  An infinite timeout never expires and {@link #isDepleted()} always
   * returns {@code false}.
   *
   * @return {@code true} for an infinite timeout
   */
  public boolean isInfinite() {
    return this == INFINITE;
  }

  /**
   * Returns {@code true} if this timeout is <em>finite</em>, i.e. it is neither {@link #EMPTY}
   * nor {@link #INFINITE}.  Only finite timeouts carry a meaningful {@link #remaining()} value.
   *
   * @return {@code true} for a finite timeout
   */
  public boolean isFinite() {
    return !isEmpty() && !isInfinite();
  }

  /**
   * Checks whether this budget is depleted, using {@link System#nanoTime()} as the current
   * time source.
   * <ul>
   *   <li>If the budget is <em>empty</em>   &rarr; always returns {@code true}
   *       (treat as immediately depleted; do not retry).</li>
   *   <li>If the budget is <em>infinite</em> &rarr; always returns {@code false}
   *       (never expires).</li>
   *   <li>If the budget is <em>finite</em>   &rarr; returns {@code true} when the
   *       configured timeout has elapsed since construction.</li>
   * </ul>
   *
   * @return {@code true} if the budget has been used up
   */
  public boolean isDepleted() {
    return isDepleted(System.nanoTime());
  }

  /**
   * Checks whether this budget is depleted at the given point in time.
   * <ul>
   *   <li>If the budget is <em>empty</em>   &rarr; always returns {@code true}.</li>
   *   <li>If the budget is <em>infinite</em> &rarr; always returns {@code false}.</li>
   *   <li>If the budget is <em>finite</em>   &rarr; returns {@code true} when
   *       {@code now - deadlineNanos >= 0}.</li>
   * </ul>
   * <p>
   * The subtraction form {@code now - deadlineNanos >= 0} is used rather than
   * {@code now >= deadlineNanos} to correctly handle the case where
   * {@code deadlineNanos = when + timeoutNanos} has wrapped around the signed
   * {@code long} range: signed subtraction of two {@link System#nanoTime()} values
   * is always meaningful as long as the elapsed time is within ~292 years.
   *
   * @param now the current time in nanoseconds, as returned by {@link System#nanoTime()}
   * @return {@code true} if the budget has been used up at time {@code now}
   */
  public boolean isDepleted(long now) {
    if (isEmpty()) return true;
    if (isInfinite()) return false;
    return now - deadlineNanos >= 0;
  }

  /**
   * Returns a {@link TimeBudget} representing the time remaining from now.
   * <ul>
   *   <li><em>empty</em>   &rarr; returns {@link #EMPTY} (same singleton).</li>
   *   <li><em>infinite</em> &rarr; returns {@link #INFINITE} (same singleton).</li>
   *   <li><em>finite</em>   &rarr; returns a new finite timeout whose value is
   *       {@code max(0, deadline - now)} in nanoseconds.</li>
   * </ul>
   *
   * @return a {@link TimeBudget} holding the remaining time
   */
  public TimeBudget remaining() {
    return remaining(System.nanoTime());
  }

  /**
   * Returns a {@link TimeBudget} representing the time remaining at the given point in
   * time, always expressed in nanoseconds for full precision.
   * <ul>
   *   <li><em>empty</em>   &rarr; returns {@link #EMPTY} (same singleton).</li>
   *   <li><em>infinite</em> &rarr; returns {@link #INFINITE} (same singleton).</li>
   *   <li><em>finite not yet depleted</em> &rarr; returns a new finite budget whose
   *       {@link #getTimeoutValue()} is {@code deadline - now} nanoseconds.</li>
   *   <li><em>finite depleted</em> &rarr; returns a new finite budget with
   *       {@link #getTimeoutValue()} of {@code 0} and {@link #isDepleted(long)} returning
   *       {@code true} immediately. Note: the returned budget is still <em>finite</em>
   *       (not {@link #EMPTY}): {@code EMPTY} means "try once, don't consult the time",
   *       whereas a depleted finite budget means "the time ran out".</li>
   * </ul>
   *
   * @param now the current time in nanoseconds, as returned by {@link System#nanoTime()}
   * @return a {@link TimeBudget} holding the remaining time at {@code now}
   */
  public TimeBudget remaining(long now) {
    if (isEmpty() || isInfinite()) {
      return this;
    }
    // deadlineNanos - now: safe signed subtraction using the JDK-recommended nanoTime idiom,
    // correct even when deadlineNanos has wrapped the signed long range.
    // remainingNanos < 0 means depleted (consistent with isDepleted: now - deadlineNanos >= 0).
    long remainingNanos = deadlineNanos - now;
    // Store full nanosecond precision — no unit truncation.
    long remainingTimeout = Math.max(0, remainingNanos);
    // When depleted, set the corrected deadline to now so that isDepleted() on the returned
    // budget returns true immediately (now - now == 0 >= 0).
    long correctedDeadlineNanos = remainingNanos < 0 ? now : deadlineNanos;
    return new TimeBudget(remainingTimeout, correctedDeadlineNanos);
  }

  /**
   * Returns the timeout value in nanoseconds for finite (including depleted) timeouts.
   * This is a snapshot of the value at the time this budget was created or last obtained
   * from {@link #remaining(long)} — it does not decrease over time. Use
   * {@link #isDepleted()} for a live expiry check.
   * <ul>
   *   <li><em>empty</em>   &rarr; {@link OptionalLong#empty()} — no meaningful value.</li>
   *   <li><em>infinite</em> &rarr; {@link OptionalLong#empty()} — no meaningful value.</li>
   *   <li><em>finite</em>   &rarr; {@link OptionalLong#of(long)} wrapping the timeout in
   *       nanoseconds. This may be {@code 0L} for a depleted budget obtained via
   *       {@link #remaining(long)}.</li>
   * </ul>
   *
   * @return the snapshot timeout value in nanoseconds, or empty for non-finite timeouts
   */
  public OptionalLong getTimeoutValue() {
    if (isEmpty()) return OptionalLong.empty();
    if (isInfinite()) return OptionalLong.empty();
    return OptionalLong.of(timeoutNanos);
  }

  /**
   * Returns the snapshot timeout value for finite (including depleted) timeouts, converted
   * to the requested unit. See {@link #getTimeoutValue()} for the snapshot semantics.
   * <ul>
   *   <li><em>empty</em>   &rarr; {@link OptionalLong#empty()} — no meaningful value.</li>
   *   <li><em>infinite</em> &rarr; {@link OptionalLong#empty()} — no meaningful value.</li>
   *   <li><em>finite</em>   &rarr; {@link OptionalLong#of(long)} wrapping the timeout value
   *       converted to {@code unit} (truncated toward zero).
   *       This may be {@code 0L} for a depleted budget obtained via {@link #remaining(long)}.</li>
   * </ul>
   *
   * @param unit the unit to express the timeout value in; must not be {@code null}
   * @return the snapshot timeout value in {@code unit}, or empty for non-finite timeouts
   */
  public OptionalLong getTimeoutValue(TimeUnit unit) {
    if (isEmpty()) return OptionalLong.empty();
    if (isInfinite()) return OptionalLong.empty();
    return OptionalLong.of(unit.convert(timeoutNanos, TimeUnit.NANOSECONDS));
  }

  /**
   * Returns the timeout value as a {@code long} suitable for use in a Java
   * {@link java.util.concurrent.locks.LockSupport#parkNanos} / latch-await call, in nanoseconds:
   * <ul>
   *   <li><em>finite</em>   &rarr; the timeout value in nanoseconds.</li>
   *   <li><em>infinite</em> &rarr; {@link Long#MAX_VALUE}, because a Java latch does not accept
   *       an "infinite" sentinel; {@code Long.MAX_VALUE} nanoseconds is effectively unbounded.</li>
   *   <li><em>empty</em>    &rarr; {@code 0L}: no waiting — the lease is expected to be present
   *       already and the operation is attempted exactly once.</li>
   * </ul>
   *
   * @return the timeout value to pass to a latch-wait in nanoseconds; never negative
   */
  public long toWaitValue() {
    // TimeBudget is a positive number ? return it.
    // TimeBudget is empty (-1) ? no timeout: try once (0)
    // TimeBudget is infinite ? => Long.MAX_VALUE since this is a Java wait on a latch
    return getTimeoutValue().orElseGet(() -> isInfinite() ? Long.MAX_VALUE : 0);
  }

  /**
   * Returns the timeout value as a {@code long} suitable for use in a Java latch-await call,
   * converted to the requested unit:
   * <ul>
   *   <li><em>finite</em>   &rarr; the timeout value converted to {@code unit}.</li>
   *   <li><em>infinite</em> &rarr; {@link Long#MAX_VALUE}.</li>
   *   <li><em>empty</em>    &rarr; {@code 0L}: no waiting.</li>
   * </ul>
   *
   * @param unit the unit to express the wait value in; must not be {@code null}
   * @return the timeout value to pass to a latch-wait in {@code unit}; never negative
   */
  public long toWaitValue(TimeUnit unit) {
    return getTimeoutValue(unit).orElseGet(() -> isInfinite() ? Long.MAX_VALUE : 0);
  }

  @Override
  public String toString() {
    if (isEmpty()) return "TimeBudget{empty}";
    if (isInfinite()) return "TimeBudget{infinite}";
    return "TimeBudget{" + timeoutNanos + " ns}";
  }

  /**
   * Returns this timeout as the string representation expected by the Voltron connection
   * property {@code ConnectionPropertyNames.CONNECTION_TIMEOUT}, i.e.:
   *
   * <pre>
   *   properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT,
   *       timeout.toVoltronString());
   * </pre>
   * <p>
   * Voltron interprets the property value as follows:
   * <ul>
   *   <li>{@code -1} — try exactly once, no blocking wait.</li>
   *   <li>{@code  0} — infinite wait (never times out).</li>
   *   <li>{@code >0} — finite wait in milliseconds.</li>
   * </ul>
   * <p>
   * This method maps the {@link TimeBudget} states to those values:
   * <ul>
   *   <li><em>empty</em>   &rarr; {@code "-1"}: try exactly once, no blocking wait.</li>
   *   <li><em>infinite</em> &rarr; {@code "0"}: infinite Voltron timeout</li>
   *   <li><em>finite with remaining &gt; 0</em> &rarr; the remaining timeout converted to
   *       milliseconds as a decimal string (always &ge; 1).</li>
   *   <li><em>finite with remaining == 0</em> &rarr; {@code "-1"}: the budget is already
   *       depleted (obtained via {@link #remaining(long)} on an expired finite timeout).
   *       Passing {@code 0} to Voltron would be treated as infinite, so {@code "-1"} is
   *       returned instead so that core tries exactly once and does not block.</li>
   * </ul>
   *
   * @return the Voltron connection-timeout string in milliseconds; never {@code null}
   */
  public String toVoltronString() {
    return String.valueOf(toVoltronDuration().toMillis());
  }

  /**
   * Returns this timeout as a millisecond-precision {@link Duration} using the same sign
   * convention that Voltron uses for connection timeouts (and that {@link #parse(Duration)}
   * accepts), so that {@code parse(toDuration())} round-trips correctly:
   * <ul>
   *   <li><em>empty</em>   &rarr; {@code Duration.ofMillis(-1)} — negative duration, which Voltron
   *       interprets as try exactly once with no blocking wait.</li>
   *   <li><em>infinite</em> &rarr; {@code Duration.ofMillis(0)} — zero duration, which Voltron
   *       interprets as an infinite wait (never times out).</li>
   *   <li><em>finite with millis &gt; 0</em> &rarr; {@code Duration.ofMillis(timeoutNanos / 1_000_000)} —
   *       positive duration in milliseconds (Voltron's resolution).</li>
   *   <li><em>finite with millis == 0</em> (depleted or sub-millisecond) &rarr; {@code Duration.ofMillis(-1)} —
   *       a zero millisecond value would be interpreted by Voltron as infinite,
   *       so {@code -1 ms} is returned instead to preserve the try-once semantic.</li>
   * </ul>
   *
   * @return a millisecond-precision {@link Duration} in Voltron sign convention; never {@code null}
   * @see #parse(Duration)
   * @see #toVoltronString()
   */
  public Duration toVoltronDuration() {
    if (isEmpty()) return Duration.ofMillis(-1);
    if (isInfinite()) return Duration.ZERO;
    // Voltron only handles milliseconds — truncate to millis.
    // A depleted budget (timeoutNanos == 0) or a sub-millisecond remainder must not produce
    // Duration.ZERO, which Voltron would treat as infinite.  Return -1 ms instead.
    long millis = TimeUnit.NANOSECONDS.toMillis(timeoutNanos);
    if (millis == 0) return Duration.ofMillis(-1);
    return Duration.ofMillis(millis);
  }

  /**
   * Creates a {@link TimeBudget} from a signed timeout value, dispatching to the appropriate
   * factory based on the sign. The semantic is based on how Voltron understands timeout values:
   * <ul>
   *   <li>{@code timeout > 0}  &rarr; {@link #finite(long, TimeUnit)}</li>
   *   <li>{@code timeout == 0} &rarr; {@link #INFINITE}</li>
   *   <li>{@code timeout < 0}  &rarr; {@link #EMPTY} means try once (-1)</li>
   * </ul>
   *
   * @param timeout  the timeout value; its sign determines the timeout type
   * @param timeUnit the time unit; only used when {@code timeout > 0}
   * @return a {@link TimeBudget} matching the given timeout
   */
  public static TimeBudget parse(long timeout, TimeUnit timeUnit) {
    if (timeout < 0) return EMPTY;
    if (timeout == 0) return INFINITE;
    return finite(timeout, timeUnit);
  }

  /**
   * Creates a {@link TimeBudget} from a {@link Duration}, dispatching to the appropriate
   * factory based on the sign of the duration. The semantic mirrors how Voltron understands
   * timeout values:
   * <ul>
   *   <li>{@code duration.isNegative()} &rarr; {@link #EMPTY} — try exactly once, no blocking wait.</li>
   *   <li>{@code duration.isZero()}     &rarr; {@link #INFINITE} — never times out.</li>
   *   <li>positive duration             &rarr; {@link #finite(long, TimeUnit)} with the full
   *       nanosecond value of the duration.</li>
   * </ul>
   *
   * @param duration the timeout duration; must not be {@code null}
   * @return a {@link TimeBudget} matching the given duration
   * @see #parse(long, TimeUnit)
   */
  public static TimeBudget parse(Duration duration) {
    if (duration.isNegative()) return EMPTY;
    if (duration.isZero()) return INFINITE;
    return finite(duration.toNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * Creates a {@link TimeBudget} from a {@link Measure}, delegating to
   * {@link #parse(long, TimeUnit)} using the measure's quantity and unit.
   *
   * @param measure the timeout expressed as a {@link Measure}; must not be {@code null}
   * @return a {@link TimeBudget} matching the given measure
   * @see #parse(long, TimeUnit)
   */
  public static TimeBudget parse(Measure<org.terracotta.common.struct.TimeUnit> measure) {
    return parse(measure.getQuantity(), measure.getUnit().toTimeUnit());
  }

  /**
   * Creates a finite {@link TimeBudget} with a non-negative timeout.
   *
   * <p>A value of {@code 0} is accepted and represents an already-depleted budget:
   * {@link #isDepleted()} will return {@code true} immediately, {@link #getTimeoutValue()} will
   * return {@code OptionalLong.of(0L)}, and {@link #toVoltronString()} will return
   * {@code "-1"} (try-once) so that Voltron does not mistake {@code 0} for infinite.
   *
   * @param timeout  the duration; must be {@code >= 0}
   * @param timeUnit the unit of {@code timeout}; must not be {@code null}
   * @return a new finite timeout
   * @throws IllegalArgumentException if {@code timeout} is negative
   * @throws NullPointerException     if {@code timeUnit} is {@code null}
   */
  public static TimeBudget finite(long timeout, TimeUnit timeUnit) throws IllegalArgumentException {
    return finite(System.nanoTime(), timeout, timeUnit);
  }

  public static TimeBudget finite(long timeout, org.terracotta.common.struct.TimeUnit timeUnit) throws IllegalArgumentException {
    return finite(System.nanoTime(), timeout, timeUnit.toTimeUnit());
  }

  /**
   * Creates a finite {@link TimeBudget} with a non-negative timeout and an explicit start
   * time. This overload is intended for testing or for callers that already hold a
   * {@link System#nanoTime()} sample they want to reuse as the budget's origin.
   *
   * <p>A value of {@code 0} for {@code timeout} is accepted and represents an already-depleted
   * budget: {@link #isDepleted(long)} will return {@code true} for any {@code now >= when},
   * {@link #getTimeoutValue()} will return {@code OptionalLong.of(0L)}, and {@link #toVoltronString()}
   * will return {@code "-1"} (try-once) so that Voltron does not mistake {@code 0} for infinite.
   *
   * @param when     the budget's start time in nanoseconds, as returned by {@link System#nanoTime()}
   * @param timeout  the duration; must be {@code >= 0}
   * @param timeUnit the unit of {@code timeout}; must not be {@code null}
   * @return a new finite timeout
   * @throws IllegalArgumentException if {@code timeout} is negative
   * @throws NullPointerException     if {@code timeUnit} is {@code null}
   */
  public static TimeBudget finite(long when, long timeout, TimeUnit timeUnit) throws IllegalArgumentException {
    if (timeout < 0) {
      throw new IllegalArgumentException("TimeBudget must be positive or 0");
    }
    long timeoutNanos = timeUnit.toNanos(timeout);
    return new TimeBudget(timeoutNanos, when + timeoutNanos);
  }

  /**
   * Creates a finite {@link TimeBudget} of the given number of days.
   *
   * @param days the duration in days; must be {@code >= 0}
   * @return a new finite timeout
   * @throws IllegalArgumentException if {@code days} is negative
   */
  public static TimeBudget ofDays(long days) {
    return finite(days, TimeUnit.DAYS);
  }

  /**
   * Creates a finite {@link TimeBudget} of the given number of hours.
   *
   * @param hours the duration in hours; must be {@code >= 0}
   * @return a new finite timeout
   * @throws IllegalArgumentException if {@code hours} is negative
   */
  public static TimeBudget ofHours(long hours) {
    return finite(hours, TimeUnit.HOURS);
  }

  /**
   * Creates a finite {@link TimeBudget} of the given number of minutes.
   *
   * @param minutes the duration in minutes; must be {@code >= 0}
   * @return a new finite timeout
   * @throws IllegalArgumentException if {@code minutes} is negative
   */
  public static TimeBudget ofMinutes(long minutes) {
    return finite(minutes, TimeUnit.MINUTES);
  }

  /**
   * Creates a finite {@link TimeBudget} of the given number of seconds.
   *
   * @param seconds the duration in seconds; must be {@code >= 0}
   * @return a new finite timeout
   * @throws IllegalArgumentException if {@code seconds} is negative
   */
  public static TimeBudget ofSeconds(long seconds) {
    return finite(seconds, TimeUnit.SECONDS);
  }

  /**
   * Creates a finite {@link TimeBudget} of the given number of milliseconds.
   *
   * @param millis the duration in milliseconds; must be {@code >= 0}
   * @return a new finite timeout
   * @throws IllegalArgumentException if {@code millis} is negative
   */
  public static TimeBudget ofMillis(long millis) {
    return finite(millis, TimeUnit.MILLISECONDS);
  }
}
