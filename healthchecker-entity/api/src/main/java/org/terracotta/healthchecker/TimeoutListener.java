/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

import org.terracotta.connection.Connection;

/**
 *
 * @author mscott
 */
public interface TimeoutListener {
  void connectionClosed(Connection target);
  void probeFailed(Connection target);
}
