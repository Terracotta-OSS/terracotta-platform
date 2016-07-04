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
package org.terracotta.voltron.management.consumer;

/**
 * A contingency exception thrown when a message is posted to the consumer,
 * but the consumer has still not posted or received an acknowledgement
 * for the previous message.
 *
 * @author RKAV
 */
public class PreviousMessageAckPendingException extends Exception {
  /**
   * Creates a new exception wrapping the {@link Throwable cause} passed in.
   *
   * @param cause the cause of this exception
   */
  public PreviousMessageAckPendingException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new exception wrapping the {@link Throwable cause} passed in and with the provided message.
   *
   * @param message information about the exception
   * @param cause the cause of this exception
   */
  public PreviousMessageAckPendingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new exception with the provided message.
   *
   * @param message information about the exception
   */
  public PreviousMessageAckPendingException(String message) {
    super(message);
  }
}
