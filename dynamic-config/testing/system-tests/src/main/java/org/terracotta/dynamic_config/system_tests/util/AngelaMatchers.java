/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.util;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.terracotta.angela.common.ConfigToolExecutionResult;

/**
 * @author Mathieu Carbou
 */
public class AngelaMatchers {

  public static Matcher<ConfigToolExecutionResult> containsOutput(String text) {
    return new CustomTypeSafeMatcher<ConfigToolExecutionResult>(" contains " + text) {
      @Override
      protected boolean matchesSafely(ConfigToolExecutionResult result) {
        return result.toString().contains(text);
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

}
