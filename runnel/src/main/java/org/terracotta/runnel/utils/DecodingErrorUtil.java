/*
 * Copyright (c) 2011-2018 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.runnel.utils;

import java.io.PrintStream;

public class DecodingErrorUtil {
  public static boolean write(PrintStream out, RunnelDecodingException e) {
    String message = e.getMessage();
    boolean hasMessage = message != null && !message.isEmpty();

    out.append(" decoding failed: ");
    out.append(e.getClass().getSimpleName());
    if (hasMessage) {
      out.append(": ");
      out.append(message);
    }

    return false;
  }
}
