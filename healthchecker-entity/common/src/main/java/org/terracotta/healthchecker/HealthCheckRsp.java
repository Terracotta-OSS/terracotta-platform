/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

import org.terracotta.entity.EntityResponse;

/**
 *
 * @author mscott
 */
public class HealthCheckRsp implements EntityResponse {
  
  private final String base;

  public HealthCheckRsp(String base) {
    this.base = base;
  }

  @Override
  public String toString() {
    return base;
  }
  
}
