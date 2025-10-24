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
package org.terracotta.stats.integration.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all stats entity integration tests
 */
public abstract class AbstractTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTest.class);

  @BeforeEach
  public void setUp() throws Exception {
    LOGGER.info("Setting up test: {}", getClass().getSimpleName());
    // Common setup code would go here
  }

  @AfterEach
  public void tearDown() throws Exception {
    LOGGER.info("Tearing down test: {}", getClass().getSimpleName());
    // Common teardown code would go here
  }
}
