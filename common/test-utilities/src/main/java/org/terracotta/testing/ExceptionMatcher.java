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
package org.terracotta.testing;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.instanceOf;

/**
 * @author Mathieu Carbou
 */
public class ExceptionMatcher extends TypeSafeMatcher<ExceptionMatcher.Closure> {

  private static final CustomMatcher<String> ANY_MESSAGE = new CustomMatcher<String>("ANY MESSAGE") {
    @Override
    public boolean matches(Object item) {
      return true;
    }
  };
  private static final CustomMatcher<? super Class<? extends Throwable>> ANY_CAUSE = new CustomMatcher<Class<? extends Throwable>>("ANY CAUSE") {
    @Override
    public boolean matches(Object item) {
      return true;
    }
  };

  private final Matcher<? super Class<? extends Throwable>> typeMatcher;
  private Matcher<? super String> messageMatcher = ANY_MESSAGE;
  private Matcher<? super Class<? extends Throwable>> causeMatcher = ANY_CAUSE;

  private Throwable failure;

  private ExceptionMatcher(Matcher<? super Class<? extends Throwable>> typeMatcher) {
    this.typeMatcher = requireNonNull(typeMatcher);
  }

  @Override
  public void describeTo(Description description) {
    typeMatcher.describeTo(description);
    if (messageMatcher != ANY_MESSAGE) {
      description.appendText(" with message ");
      messageMatcher.describeTo(description);
    }
    if (causeMatcher != ANY_CAUSE) {
      description.appendText(" with cause ");
      causeMatcher.describeTo(description);
    }
  }

  @Override
  protected boolean matchesSafely(Closure item) {
    try {
      item.run();
      return false;
    } catch (Throwable e) {
      failure = e;
      boolean match = typeMatcher.matches(e) && messageMatcher.matches(e.getMessage()) && causeMatcher.matches(e.getCause());
      if (!match) {
        e.printStackTrace();
      }
      return match;
    }
  }

  @Override
  protected void describeMismatchSafely(Closure item, Description mismatchDescription) {
    if (failure != null) {
      super.describeMismatchSafely(new ToStringClosure(failure.toString()), mismatchDescription);
    } else {
      super.describeMismatchSafely(new ToStringClosure("no exception was thrown"), mismatchDescription);
    }
  }

  public static ExceptionMatcher throwing() {
    return throwing(instanceOf(Throwable.class));
  }

  public static ExceptionMatcher throwing(Matcher<? super Class<? extends Throwable>> err) {
    return new ExceptionMatcher(err);
  }

  public ExceptionMatcher andMessage(Matcher<? super String> messageMatcher) {
    this.messageMatcher = requireNonNull(messageMatcher);
    return this;
  }

  public ExceptionMatcher andCause(Matcher<? super Class<? extends Throwable>> causeMatcher) {
    this.causeMatcher = requireNonNull(causeMatcher);
    return this;
  }

  private static class ToStringClosure implements Closure {
    private final String toString;

    public ToStringClosure(String toString) {
      this.toString = toString;
    }

    @Override
    public void run() {
    }

    @Override
    public String toString() {
      return toString;
    }
  }

  @FunctionalInterface
  public interface Closure {
    void run() throws Throwable;
  }

}
