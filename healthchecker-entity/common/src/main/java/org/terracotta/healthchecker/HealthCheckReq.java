/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

import org.terracotta.entity.EntityMessage;

/**
 *
 * @author mscott
 */
public class HealthCheckReq implements EntityMessage {
  
  private final String base;

  public HealthCheckReq(String base) {
    this.base = base;
  }

  @Override
  public String toString() {
    return base;
  }
  
}
