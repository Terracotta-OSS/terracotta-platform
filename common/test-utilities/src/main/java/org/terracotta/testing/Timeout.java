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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Enhanced Junit Timeout rule  which is able to do a thread dump of all running java processes in case of a timeout
 *
 * @author Mathieu Carbou
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
public class Timeout extends org.junit.rules.Timeout {

  private static final Logger LOGGER = LoggerFactory.getLogger(Timeout.class);

  private final Path threadDump;
  private Duration timeout;

  public Timeout(Builder builder) {
    super(builder);
    this.threadDump = builder.threadDump;
    this.timeout = builder.timeout;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    try {
      return createFailOnTimeoutStatement(base, description);
    } catch (final Exception e) {
      return new Statement() {
        @Override
        public void evaluate() {
          throw new RuntimeException("Invalid parameters for Timeout", e);
        }
      };
    }
  }

  protected Statement createFailOnTimeoutStatement(Statement statement, Description description) throws Exception {
    Statement base = super.createFailOnTimeoutStatement(statement);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } catch (Throwable throwable) {
          if (threadDump != null && (throwable instanceof MultipleFailureException || throwable instanceof TestTimedOutException)) {
            try {
              executeThreadDump(description);
            } catch (RuntimeException e) {
              LOGGER.warn("Error occurred when trying to take thread dumps after timeout of test: " + description, e);
            }
          }
          throw throwable;
        }
      }
    };
  }

  protected void executeThreadDump(Description description) {
    Path output = threadDump.resolve(description.getTestClass().getSimpleName()).resolve(description.getMethodName() == null ? "class" : description.getMethodName());
    LOGGER.info("Taking thread dumps after timeout of test: {} into: {}", description, output);
    ThreadDump.dumpAll(output, timeout);
  }

  @SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends org.junit.rules.Timeout.Builder {

    private Path threadDump;
    private Duration timeout;

    @Override
    public Builder withTimeout(long timeout, TimeUnit unit) {
      super.withTimeout(timeout, unit);
      return this;
    }

    @Override
    public Builder withLookingForStuckThread(boolean enable) {
      super.withLookingForStuckThread(enable);
      return this;
    }

    public Builder withThreadDump(Path outputDir) {
      this.threadDump = requireNonNull(outputDir);
      return this;
    }

    public Builder withThreadDump(Path outputDir, Duration timeout) {
      this.threadDump = requireNonNull(outputDir);
      this.timeout = timeout;
      return this;
    }

    @Override
    public Timeout build() {
      return new Timeout(this);
    }
  }
}
