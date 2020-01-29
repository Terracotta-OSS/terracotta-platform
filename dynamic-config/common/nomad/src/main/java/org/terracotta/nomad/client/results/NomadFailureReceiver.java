/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.results;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.lineSeparator;

/**
 * @author Mathieu Carbou
 */
public class NomadFailureReceiver<T> extends LoggingResultReceiver<T> {

  private final List<String> failures = new CopyOnWriteArrayList<>();

  @Override
  protected void error(String line) {
    failures.add(line);
  }

  public List<String> getFailures() {
    return failures;
  }

  public boolean isEmpty() {
    return failures.isEmpty();
  }

  public void reThrow() throws IllegalStateException {
    if (!isEmpty()) {
      StringBuilder msg = new StringBuilder("Two-Phase commit failed with " + failures.size() + " messages(s):" + lineSeparator() + lineSeparator());
      for (int i = 0; i < failures.size(); i++) {
        if (msg.charAt(msg.length() - 1) != '\n') {
          msg.append(lineSeparator());
        }
        msg.append("(").append(i + 1).append(") ").append(failures.get(i));
      }
      throw new IllegalStateException(msg.toString());
    }
  }
}
