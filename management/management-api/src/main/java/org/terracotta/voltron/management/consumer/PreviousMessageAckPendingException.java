/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
