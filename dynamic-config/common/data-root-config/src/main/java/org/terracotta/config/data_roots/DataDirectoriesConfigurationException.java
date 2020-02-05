/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package org.terracotta.config.data_roots;

/**
 * @author vmad
 */
public class DataDirectoriesConfigurationException extends RuntimeException {

  private static final long serialVersionUID = 6738773592638813440L;

  /**
   * Constructs a new exception with the specified detail message.  The cause is not initialized, and may subsequently be
   * initialized by a call to {@link #initCause}.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
   *                method.
   */
  public DataDirectoriesConfigurationException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.  <p>Note that the detail message associated
   * with {@code cause} is <i>not</i> automatically incorporated in this exception's detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
   * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A <tt>null</tt>
   *                value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @since 1.4
   */
  public DataDirectoriesConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}