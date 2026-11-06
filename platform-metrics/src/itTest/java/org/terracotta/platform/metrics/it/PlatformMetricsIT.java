/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.platform.metrics.it;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * Starts a 1x2 cluster in diagnostic mode, sets a short metrics logging period through a tc-property,
 * activates the cluster, triggers a failover and validates the metrics lines found in the server logs.
 */
@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class PlatformMetricsIT extends DynamicConfigIT {
  private static final String PERIOD_PROPERTY = "org.terracotta.platform.metrics.logging.period";
  private static final int PERIOD_SECONDS = 5;

  // gives the metric logger the time to log (period is 5s)
  private static final Duration PAUSE = Duration.ofSeconds(10);

  // STATS:1792673554:5|T:P|A,P|P:0001,+01,-01|S:0001,+00,-00|V:0000,+00,-00|C:0000,+00,-00|F:0000,+00,-00|EIC:+00|CIC:+00|SIC:+00|QIC:+00
  private static final String GAUGE = "\\d{4,},\\+\\d{2,},-\\d{2,}";
  private static final Pattern STATS = Pattern.compile("STATS:(\\d+):(\\d+)\\|T:([PTFD])\\|([A-Z?]+(?:,[A-Z?]+)*)" +
    "\\|P:" + GAUGE +
    "\\|S:" + GAUGE +
    "\\|V:" + GAUGE +
    "\\|C:" + GAUGE +
    "\\|F:" + GAUGE +
    "\\|EIC:\\+\\d{2,}\\|CIC:\\+\\d{2,}\\|SIC:\\+\\d{2,}\\|QIC:\\+\\d{2,}");

  public PlatformMetricsIT() {
    super(Duration.ofMinutes(5));
  }

  @Test
  public void test_metrics_logged_across_failover() throws Exception {
    // start 2 nodes in diagnostic mode
    startNode(1, 1);
    startNode(1, 2);

    // form the stripe
    attachAll();

    // reduce the logging interval
    waitUntil(
      () -> configTool("set", "-connect-to", "localhost:" + getNodePort(), "-setting", "tc-properties." + PERIOD_PROPERTY + "=" + PERIOD_SECONDS),
      is(successful()));

    // activate
    activateCluster();
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    // let the periodic logger run on both nodes
    pause();
    pause();

    for (int nodeId : new int[]{activeId, passiveId}) {
      List<Stats> stats = stats(nodeId);
      assertThat("No metrics logged on node " + nodeId, stats, not(empty()));
      // with a 5s period, we must see several periodic lines in ~20s (default period is 300s)
      assertThat("Not enough periodic metrics logged on node " + nodeId, count(stats, "P"), greaterThanOrEqualTo(2L));
    }

    // active sees both server states (A and P) in the logs when passive is there
    Stats activeStats = last(stats(activeId));
    assertThat(activeStats.topology.contains("A"), is(true));
    assertThat(activeStats.topology.contains("P"), is(true));

    // passive sees itself as passive
    assertThat(last(stats(passiveId)).topology.contains("P"), is(true));

    // failover: stop the active
    int linesBeforeFailover = stats(passiveId).size();
    pause();
    stopNode(1, activeId);
    waitForActive(1, passiveId);

    // let the new active log
    pause();

    List<Stats> afterFailover = stats(passiveId);
    assertThat("No metrics logged after failover", afterFailover.size(), greaterThanOrEqualTo(linesBeforeFailover + 1));

    // all lines logged after failover come from the former passive: the last one must show it as the active
    Stats latest = last(afterFailover);
    assertThat(latest.topology.contains("A"), is(true));
    // state changes (passive -> active) trigger an immediate log
    assertThat("No topology change line logged after failover", count(afterFailover.subList(linesBeforeFailover, afterFailover.size()), "T"), greaterThanOrEqualTo(1L));

    // restart the old active node after failover (joins as passive)
    int linesBeforeRestart = stats(passiveId).size();
    startNode(1, activeId);
    waitForPassive(1, activeId);

    // let the logger run
    pause();

    List<Stats> afterRestart = stats(passiveId);
    assertThat("No metrics logged on active after restarting passive", afterRestart.size(), greaterThanOrEqualTo(linesBeforeRestart + 1));

    // active log line is updated and sees both server states (A and P) again
    Stats latestAfterRestart = last(afterRestart);
    assertThat(latestAfterRestart.topology.contains("A"), is(true));
    assertThat(latestAfterRestart.topology.contains("P"), is(true));

    // restarted node (now passive) also logs metrics and sees itself as passive
    List<Stats> restartedStats = stats(activeId);
    assertThat("No metrics logged on restarted node", restartedStats, not(empty()));
    assertThat(last(restartedStats).topology.contains("P"), is(true));
  }

  private void pause() throws InterruptedException {
    Thread.sleep(PAUSE.toMillis());
  }

  private List<Stats> stats(int nodeId) {
    return angela.serverLogs(1, nodeId).stream()
      .map(STATS::matcher)
      .filter(Matcher::find)
      .map(m -> new Stats(m.group(3), Long.parseLong(m.group(1)), Long.parseLong(m.group(2)), m.group(4)))
      .collect(Collectors.toList());
  }

  private static long count(List<Stats> stats, String trigger) {
    return stats.stream().filter(s -> s.trigger.equals(trigger)).count();
  }

  private static Stats last(List<Stats> stats) {
    return stats.get(stats.size() - 1);
  }

  private static class Stats {
    final String trigger;
    final long timestamp;
    final long secondsSinceLast;
    final String topology;

    Stats(String trigger, long timestamp, long secondsSinceLast, String topology) {
      this.trigger = trigger;
      this.timestamp = timestamp;
      this.secondsSinceLast = secondsSinceLast;
      this.topology = topology;
    }

    @Override
    public String toString() {
      return "T=" + trigger + ":" + timestamp + ":" + secondsSinceLast + "|" + topology;
    }
  }
}
