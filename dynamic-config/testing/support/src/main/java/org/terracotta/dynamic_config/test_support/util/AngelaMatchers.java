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
package org.terracotta.dynamic_config.test_support.util;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.terracotta.angela.common.ConfigToolExecutionResult;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public class AngelaMatchers {

  public static Matcher<NodeOutputRule.NodeLog> containsLog(String text) {
    return new CustomTypeSafeMatcher<NodeOutputRule.NodeLog>("contains " + text) {
      @Override
      protected boolean matchesSafely(NodeOutputRule.NodeLog result) {
        return result.streamLogsDescending().anyMatch(line -> line.contains(text));
      }
    };
  }

  public static Matcher<ConfigToolExecutionResult> successful() {
    return new CustomTypeSafeMatcher<ConfigToolExecutionResult>("successful") {
      @Override
      protected boolean matchesSafely(ConfigToolExecutionResult result) {
        return result.getExitStatus() == 0 && find(result.getOutput(), "Command successful");
      }
    };
  }

  public static Matcher<ConfigToolExecutionResult> containsOutput(String text) {
    return new CustomTypeSafeMatcher<ConfigToolExecutionResult>("contains " + text) {
      @Override
      protected boolean matchesSafely(ConfigToolExecutionResult result) {
        return find(result.getOutput(), text);
      }
    };
  }

  public static Matcher<ConfigToolExecutionResult> containsLinesInOrderStartingWith(Collection<String> expectedLines) {
    return new CustomTypeSafeMatcher<ConfigToolExecutionResult>("contains lines starting with:\n" + String.join("\n", expectedLines)) {
      @Override
      protected boolean matchesSafely(ConfigToolExecutionResult result) {
        LinkedList<String> lines = new LinkedList<>(expectedLines);
        for (String out : result.getOutput()) {
          if (out.startsWith(lines.getFirst())) {
            lines.removeFirst();
          }
        }
        return lines.isEmpty();
      }

      @Override
      protected void describeMismatchSafely(ConfigToolExecutionResult result, Description mismatchDescription) {
        LinkedList<String> lines = new LinkedList<>(expectedLines);
        for (String out : result.getOutput()) {
          if (out.startsWith(lines.getFirst())) {
            lines.removeFirst();
          }
        }
        mismatchDescription.appendText("these lines were not found:\n" + String.join("\n", lines) + "\n in output:\n" + String.join("\n", result.getOutput()));
      }
    };
  }

  public static Matcher<ConfigToolExecutionResult> hasExitStatus(int exitStatus) {
    return new CustomTypeSafeMatcher<ConfigToolExecutionResult>(" exist status " + exitStatus) {
      @Override
      protected boolean matchesSafely(ConfigToolExecutionResult result) {
        return result.getExitStatus() == exitStatus;
      }
    };
  }

  private static boolean find(List<String> lines, String text) {
    // reverse search because chances are that the string we are searching for is more at the end than at the beginning
    for (int i = lines.size() - 1; i >= 0; i--) {
      if (lines.get(i).contains(text)) {
        return true;
      }
    }
    return false;
  }
}
