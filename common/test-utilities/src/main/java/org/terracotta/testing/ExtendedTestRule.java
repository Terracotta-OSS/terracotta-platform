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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public abstract class ExtendedTestRule implements TestRule {
  @Override
  public final Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        before(description);
        List<Throwable> errors = new ArrayList<>();
        try {
          base.evaluate();
        } catch (Throwable t) {
          errors.add(t);
        } finally {
          try {
            after(description);
          } catch (Throwable t) {
            errors.add(t);
          }
        }
        MultipleFailureException.assertEmpty(errors);
      }
    };
  }

  protected void before(Description description) throws Throwable {
  }

  protected void after(Description description) throws Throwable {
  }
}