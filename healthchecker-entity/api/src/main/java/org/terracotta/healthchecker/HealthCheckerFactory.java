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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.healthchecker;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

/**
 *
 * @author mscott
 */
public class HealthCheckerFactory {
  
  private static final String NAME = "staticHealthChecker";
  /**
   * Start a health checker on a connection.  Adding a timeout manager to a connection 
   * starts a new thread that will periodically ping the server to make sure it is up and running
   * 
   * @param connection the connection to be monitored
   * @param probeFrequencyPerMinute the frequency which to ping a server per minute
   * @param probeTimeoutInMillis timeout the connection and close if the timeout amount of time passes
   *   before a proper response is received
   * @return A TimeoutManager to attach listeners to and monitor the connection
   */
  public static TimeoutManager startHealthChecker(Connection connection, int probeFrequencyPerMinute, long probeTimeoutInMillis) {
    try {
      EntityRef<HealthCheck, Properties> check = connection.getEntityRef(HealthCheck.class, HealthCheck.VERSION, NAME);
      HealthCheck hc = check.fetchEntity();
      if (probeFrequencyPerMinute < 1 || probeFrequencyPerMinute > 120) {
        throw new IllegalArgumentException("probe frequency must be greater than zero and less than 120");
      }
      return new HealthCheckTimeoutManager(connection, hc).start(probeTimeoutInMillis, probeFrequencyPerMinute);
    } catch (EntityNotProvidedException notvalid) {
      throw new IllegalStateException("healthchecker entity is not installed with the name " + NAME, notvalid);
    } catch (EntityNotFoundException notfound) {
      throw new IllegalStateException("healthchecker entity is not installed with the name " + NAME, notfound);
    } catch (EntityVersionMismatchException version) {
      throw new IllegalStateException("healthchecker entity is not the right version on the name " + NAME, version);
    }
  }
  
  private static class HealthCheckTimeoutManager implements TimeoutManager {
    
    private final Connection root;
    private final HealthCheck checker;
    private final Set<TimeoutListener> listeners = new LinkedHashSet<TimeoutListener>();
    private final Timer driver;
    private long iteration;
    private String currentMsg;
    private Future<String> currentProbe;
    private boolean closed;

    public HealthCheckTimeoutManager(Connection conn, HealthCheck checker) {
      this.root = conn;
      this.checker = checker;
      driver = checker.getTimer();
    }
    
    public synchronized boolean probe(long timeout) throws InterruptedException, ExecutionException {
      if (currentProbe == null || currentProbe.isDone()) {
        currentMsg = "ping-" + (iteration++);
        try {
          currentProbe = checker.ping(currentMsg);
        } catch (Throwable t) {
          throw new ExecutionException(t);
        }
      }

      try {
        return currentMsg.equals(currentProbe.get(timeout, TimeUnit.MILLISECONDS));
      } catch (TimeoutException te) {
        return false;
      }
    }
    
    public TimeoutManager start(final long timeout, final long cyclesPerMin) {
      TimerTask task = new TimerTask() {
        @Override
        public void run() {
          long period = (timeout < 60000/cyclesPerMin) ? timeout : 60000/cyclesPerMin;
          long start = System.currentTimeMillis();
          try {
            while (!probe(period)) {
              long lapse = System.currentTimeMillis() - start;
              if (lapse < timeout) {
                fireProbeListeners();
                if (timeout - lapse < period) {
                  period = timeout - lapse;
                }
              } else {
                closeConnection();
              }
            }
          } catch (ExecutionException ee) {
            closeConnection();
          } catch (InterruptedException ie) {
            closeConnection();
          }
        }
      };
      driver.scheduleAtFixedRate(task, 0, 60000/cyclesPerMin);
      return this;
    }
    
    private synchronized void closeConnection() {
      try {
        closed = true;
        root.close();
      } catch (IOException ioe) {
//  anything todo here?
      }
      fireTimeoutListeners();
    }
    
    private synchronized void fireProbeListeners() {
      for (TimeoutListener l : listeners) {
        l.probeFailed(root);
      }
    }
    
    private synchronized void fireTimeoutListeners() {
      for (TimeoutListener l : listeners) {
        l.connectionClosed(root);
      }
    }

    @Override
    public synchronized boolean addTimeoutListener(TimeoutListener timeout) {
      if (closed) {
        throw new IllegalStateException();
      }
      return listeners.add(timeout);
    }
    
    public synchronized boolean removeTimeoutListener(TimeoutListener timeout) {
      return listeners.remove(timeout);
    }
    
    @Override
    public synchronized boolean isConnected() {
      return !closed;
    }
  }
}
