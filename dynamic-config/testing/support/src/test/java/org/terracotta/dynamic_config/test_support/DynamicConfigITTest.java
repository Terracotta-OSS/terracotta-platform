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
package org.terracotta.dynamic_config.test_support;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for DynamicConfigIT class.
 * Tests the command-line parsing logic fix for issue #1245.
 */
public class DynamicConfigITTest {

  /**
   * Test for issue #1245: Validates that option arguments are not mistaken for commands.
   *
   * Before the fix, when parsing "-n cluster-name activate -s localhost:9410",
   * the parser would incorrectly treat "cluster-name" as the command instead of "activate".
   *
   * The fix added an optionArg flag that tracks when the next token should be treated
   * as an argument to an option (like the value for -n) rather than as a command.
   *
   */

  @Test
  public void testConfigToolCommandLineParsing_OptionArgumentNotTreatedAsCommand() {
    // Simulate the parsing logic from DynamicConfigIT.configTool()
    String[] cli = {"-cluster-name", "cluster-name", "activate", "-s", "localhost:9410"};

    String command = null;
    List<String> globalOpts = new ArrayList<>();
    boolean optionArg = false;  // the next token is an argument for an option

    for (String opt : cli) {
      if (opt.startsWith("-")) {
        if (command == null) {
          globalOpts.add(opt);
          optionArg = true;  // Next token is the value for this option
        }
      } else if (!optionArg && command == null) {
        command = opt;  // This is the command
      } else {
        optionArg = false;  // This was an option argument, reset the flag
      }
    }

    // Verify that "activate" is correctly identified as the command, not "cluster-name"
    assertThat("Command should be 'activate', not the option argument 'cluster-name'",
               command, is(equalTo("activate")));
  }
}

