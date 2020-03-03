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
package org.terracotta.dynamic_config.cli.command;

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mathieu Carbou
 */
public abstract class Command implements Runnable {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(names = {"-h", "--help"}, description = "Help", help = true)
  private boolean help;

  public boolean isHelp() {
    return help;
  }

  /**
   * Run extra global validation after each parameter has been parsed, converted, injected and validated
   * <p>
   * Optional implementation.
   */
  public void validate() {}
}
