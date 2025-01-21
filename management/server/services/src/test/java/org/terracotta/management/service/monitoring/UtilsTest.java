/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

package org.terracotta.management.service.monitoring;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mathieu Carbou
 */
@Ignore
public class UtilsTest {
  static {
    System.setProperty("terracotta.management.assert", "true");
  }

  @Test(expected = AssertionError.class)
  public void warnOrAssert() throws Exception {
    Logger logger = LoggerFactory.getLogger("toto");
    Utils.warnOrAssert(logger, "Hello {} and {}", "men", "women", new RuntimeException());
  }
}