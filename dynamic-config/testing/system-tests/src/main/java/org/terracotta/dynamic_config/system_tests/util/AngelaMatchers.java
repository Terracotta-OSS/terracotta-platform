/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.util;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.terracotta.angela.common.ConfigToolExecutionResult;

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
