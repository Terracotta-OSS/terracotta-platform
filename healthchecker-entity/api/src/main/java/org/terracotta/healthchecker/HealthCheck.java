


package org.terracotta.healthchecker;

import java.util.Timer;
import java.util.concurrent.Future;
import org.terracotta.connection.entity.Entity;

interface HealthCheck extends Entity {
  static final long VERSION = 1;

  /**
   * Similar to echoAsResponse, above, but internally waits on the given ack, prior to returning.
   * 
   * @param message The message to echo.
   * @return The Future to access the asynchronous response.
   */
   Future<String> ping(String message);
   
   Timer getTimer();
}
