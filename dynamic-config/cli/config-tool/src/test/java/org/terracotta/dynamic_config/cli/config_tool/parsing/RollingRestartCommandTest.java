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
package org.terracotta.dynamic_config.cli.config_tool.parsing;

import com.beust.jcommander.JCommander;
import org.junit.Test;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.api.command.RollingRestartAction;
import org.terracotta.inet.HostPort;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RollingRestartCommandTest {

  @Test
  public void test_parsed_options_are_delegated_to_action() {
    RollingRestartAction action = mock(RollingRestartAction.class);
    RollingRestartCommand command = new RollingRestartCommand(action);

    JCommander.newBuilder().addObject(command).build().parse(
      "-connect-to", "node-1:9410",
      "-restart-wait-time", "37s",
      "-restart-delay", "5s");

    command.run();

    verify(action).setNodes(singletonList(HostPort.create("node-1", 9410)));
    verify(action).setRestartWaitTime(Measure.of(37, TimeUnit.SECONDS));
    verify(action).setRestartDelay(Measure.of(5, TimeUnit.SECONDS));
    verify(action).run();
  }
}
