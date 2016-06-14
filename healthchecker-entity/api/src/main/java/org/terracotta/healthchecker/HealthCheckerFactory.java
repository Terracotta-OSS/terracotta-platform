/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

import java.io.IOException;
import java.util.Collections;
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
    private final Set<TimeoutListener> listeners = Collections.synchronizedSet(new LinkedHashSet<TimeoutListener>());
    private final Timer driver;
    private long iteration;
    private String currentMsg;
    private Future<String> currentProbe;

    public HealthCheckTimeoutManager(Connection conn, HealthCheck checker) {
      this.root = conn;
      this.checker = checker;
      driver = checker.getTimer();
    }
    
    public synchronized boolean probe(long timeout) throws InterruptedException, ExecutionException {
      if (currentProbe == null || currentProbe.isDone()) {
        currentMsg = "ping-" + (iteration++);
        currentProbe = checker.ping(currentMsg);
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
                try {
                  root.close();
                } catch (IOException ioe) {
                  driver.cancel();
                }
                fireTimeoutListeners();
              }
            }
          } catch (ExecutionException ee) {
            driver.cancel();
          } catch (InterruptedException ie) {
            driver.cancel();
          }
        }
      };
      driver.scheduleAtFixedRate(task, 0, 60000/cyclesPerMin);
      return this;
    }
    
    private void fireProbeListeners() {
      for (TimeoutListener l : listeners) {
        l.probeFailed(root);
      }
    }
    
    private void fireTimeoutListeners() {
      for (TimeoutListener l : listeners) {
        l.connectionClosed(root);
      }
    }

    @Override
    public boolean addTimeoutListener(TimeoutListener timeout) {
      return listeners.add(timeout);
    }
    
    public boolean removeTimeoutListener(TimeoutListener timeout) {
      return listeners.remove(timeout);
    }
  }
}
