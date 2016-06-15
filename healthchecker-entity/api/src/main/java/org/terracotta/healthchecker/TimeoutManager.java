/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

/**
 *
 * @author mscott
 */
public interface TimeoutManager {
  boolean addTimeoutListener(TimeoutListener timout);
  boolean isConnected();
}
