/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.voter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.voter.cli.Options;
import org.terracotta.voter.cli.TCVoterMain;

import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.terracotta.dynamic_config.api.model.UID.newUID;

public class TCVoterMainTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testOverrideVote() {
    TCVoter voter = mock(TCVoter.class);
    TCVoterMain voterMain = new TCVoterMain() {
      @Override
      protected TCVoter getVoter(Optional<Properties> connectionProps) {
        return voter;
      }
    };

    String overrideTarget = "foo:1234";
    String[] args = new String[]{"-o", overrideTarget};
    voterMain.processArgs(args);

    verify(voter).overrideVote(overrideTarget);
  }

  @Test
  public void testServerOpt() {
    String hostPort = "foo:1234";
    TCVoterMain voterMain = new TCVoterMain() {
      @Override
      protected void startVoter(Optional<Properties> connectionProps, String... hostPorts) {
        assertThat(hostPorts, arrayContaining(hostPort));
      }

      @Override
      protected Cluster fetchTopology(Options options) {
        return new Cluster(new Stripe().setUID(newUID())
            .addNode(new Node().setUID(newUID()).setName("foo").setHostname("foo").setPort(1234))
        );
      }
    };

    String[] args = new String[]{"-s", hostPort};
    voterMain.processArgs(args);
  }

  @Test
  public void testMultipleServerOptArgs() {
    String hostPort1 = "foo:1234";
    String hostPort2 = "bar:2345";
    TCVoterMain voterMain = new TCVoterMain() {
      @Override
      protected void startVoter(Optional<Properties> connectionProps, String... hostPorts) {
        assertThat(hostPorts, arrayContaining(hostPort1, hostPort2));
      }

      @Override
      protected Cluster fetchTopology(Options options) {
        return new Cluster(new Stripe().setUID(newUID())
            .addNode(new Node().setUID(newUID()).setName("foo").setHostname("foo").setPort(1234))
            .addNode(new Node().setUID(newUID()).setName("bar").setHostname("bar").setPort(2345))
        );
      }
    };

    String[] args = new String[]{"-s", hostPort1 + "," + hostPort2};
    voterMain.processArgs(args);
  }

  @Test
  public void testMultipleServerOpts() {
    TCVoterMain voterMain = new TCVoterMain() {
      @Override
      protected Cluster fetchTopology(Options options) {
        return new Cluster(
            new Stripe().setUID(newUID()).addNode(new Node().setUID(newUID()).setName("foo").setHostname("foo").setPort(1234)),
            new Stripe().setUID(newUID()).addNode(new Node().setUID(newUID()).setName("bar").setHostname("bar").setPort(2345))
        );
      }
    };

    String[] args = new String[]{"-s", "foo:1234", "-s", "bar:2345"};

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Usage of multiple stripes is not supported");
    voterMain.processArgs(args);
  }

  @Test
  public void testZeroArguments() {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[0];

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Neither the -vote-for option nor the regular -connect-to option provided");
    voterMain.processArgs(args);
  }

  @Test
  public void testInvalidTargetHostPort() {
    TCVoterMain voterMain = new TCVoterMain();
    String[] args = new String[]{"-s", "bar:baz"};

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Invalid host:port combination provided");
    voterMain.processArgs(args);
  }
}