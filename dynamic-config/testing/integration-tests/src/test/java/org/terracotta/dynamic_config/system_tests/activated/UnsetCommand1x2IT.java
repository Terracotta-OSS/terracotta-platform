/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class UnsetCommand1x2IT extends DynamicConfigIT {

  @Test
  public void unset_public_hostname_port() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.public-hostname=127.0.0.1",
            "-c", "stripe.1.node.1.public-port=" + getNodePort(1, 1),
            "-c", "stripe.1.node.2.public-hostname=127.0.0.1",
            "-c", "stripe.1.node.2.public-port=" + getNodePort(1, 2)),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.public-hostname=127.0.0.1"),
            containsOutput("stripe.1.node.2.public-hostname=127.0.0.1"),
            containsOutput("stripe.1.node.1.public-port=" + getNodePort(1, 1)),
            containsOutput("stripe.1.node.2.public-port=" + getNodePort(1, 2))
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.public-hostname",
            "-c", "stripe.1.node.1.public-port",
            "-c", "stripe.1.node.2.public-hostname",
            "-c", "stripe.1.node.2.public-port"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            not(containsOutput("public-hostname=")),
            not(containsOutput("public-port="))
        ));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "public-hostname=127.0.0.1",
            "-c", "stripe.1.node.1.public-port=" + getNodePort(1, 1),
            "-c", "stripe.1.node.2.public-port=" + getNodePort(1, 2)),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.public-hostname=127.0.0.1"),
            containsOutput("stripe.1.node.2.public-hostname=127.0.0.1"),
            containsOutput("stripe.1.node.1.public-port=" + getNodePort(1, 1)),
            containsOutput("stripe.1.node.2.public-port=" + getNodePort(1, 2))
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "public-hostname",
            "-c", "public-port"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            not(containsOutput("public-hostname=")),
            not(containsOutput("public-port="))
        ));
  }

  @Test
  public void test_unset_relay_source() {
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-source=193.123.5.4:7878"),
      allOf(
        not(successful()),
        containsOutput("Invalid input"),
        containsOutput("relay-source"),
        containsOutput("cannot be set when node is activated")
      ));

    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-source=193.123.5.4:7878"),
      allOf(
        not(successful()),
        containsOutput("Invalid input"),
        containsOutput("relay-source"),
        containsOutput("cannot be unset when node is activated")
      ));

    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
      not(containsOutput("relay-source=")));
  }

    @Test
  public void unset_relay_destination() {
    assertThat(
      configTool("set", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-destination="+"193.123.5.4:" + 7878),
      is(successful()));

    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("stripe.1.node.1.relay-destination="+"193.123.5.4\\:" + 7878)
      );

    assertThat(
      configTool("unset", "-s", "localhost:" + getNodePort(),
        "-c", "stripe.1.node.1.relay-destination"),
      is(successful()));

    assertThat(
      configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("relay-destination=")));
  }

  @Test
  public void unset_tc_properties() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:1,bar:2"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.tc-properties=bar\\:2,foo\\:1"),
            containsOutput("stripe.1.node.2.tc-properties=bar\\:2,foo\\:1")
        ));

    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:2,baz:3"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.tc-properties=bar\\:2,baz\\:3,foo\\:2"),
            containsOutput("stripe.1.node.2.tc-properties=bar\\:2,baz\\:3,foo\\:2")
        ));

    // removing a specific property
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.tc-properties.bar",
            "-c", "stripe.1.node.2.tc-properties.baz"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.tc-properties=baz\\:3,foo\\:2"),
            containsOutput("stripe.1.node.2.tc-properties=bar\\:2,foo\\:2")
        ));

    // global removal of a whole map
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.tc-properties",
            "-c", "stripe.1.node.2.tc-properties.baz"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            not(containsOutput("stripe.1.node.1.tc-properties=")), // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            containsOutput("stripe.1.node.2.tc-properties=bar\\:2,foo\\:2")
        ));

    // global removal on cluster
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties=bar:2,foo:1"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.tc-properties=bar\\:2,foo\\:1"),
            containsOutput("stripe.1.node.2.tc-properties=bar\\:2,foo\\:2")
        ));
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            // these entries are in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            not(containsOutput("stripe.1.node.1.tc-properties=")),
            not(containsOutput("stripe.1.node.2.tc-properties="))
        ));
  }

  @Test
  public void unset_logger_overrides() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=foo:DEBUG,bar:INFO"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.logger-overrides=bar\\:INFO,foo\\:DEBUG"),
            containsOutput("stripe.1.node.2.logger-overrides=bar\\:INFO,foo\\:DEBUG")
        ));

    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=foo:INFO,baz:WARN"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.logger-overrides=bar\\:INFO,baz\\:WARN,foo\\:INFO"),
            containsOutput("stripe.1.node.2.logger-overrides=bar\\:INFO,baz\\:WARN,foo\\:INFO")
        ));

    // removing a specific property
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.logger-overrides.bar",
            "-c", "stripe.1.node.2.logger-overrides.baz"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.logger-overrides=baz\\:WARN,foo\\:INFO"),
            containsOutput("stripe.1.node.2.logger-overrides=bar\\:INFO,foo\\:INFO")
        ));

    // global removal of a whole map
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.logger-overrides",
            "-c", "stripe.1.node.2.logger-overrides.baz"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            not(containsOutput("stripe.1.node.1.logger-overrides=")), // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            containsOutput("stripe.1.node.2.logger-overrides=bar\\:INFO,foo\\:INFO")
        ));

    // global removal on cluster
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.logger-overrides=bar:INFO,foo:DEBUG"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.logger-overrides=bar\\:INFO,foo\\:DEBUG"),
            containsOutput("stripe.1.node.2.logger-overrides=bar\\:INFO,foo\\:INFO")
        ));
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            // these entries are in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            not(containsOutput("stripe.1.node.1.logger-overrides=")),
            not(containsOutput("stripe.1.node.2.logger-overrides="))
        ));
  }

  @Test
  public void unset_offheap_resources() {
    assertThat(configTool("export", "-s", "localhost:" + getNodePort()), is(successful()));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=boo:64MB,bar:128MB"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=bar\\:128MB,boo\\:64MB,foo\\:1GB,main\\:512MB"));

    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=boo:128MB,baz:200MB"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=bar\\:128MB,baz\\:200MB,boo\\:128MB,foo\\:1GB,main\\:512MB"));

    // removing a specific property
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.bar"),
        is(not(successful())));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=bar\\:128MB,baz\\:200MB,boo\\:128MB,foo\\:1GB,main\\:512MB"));

    // global removal of a whole map
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        is(not(successful())));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=bar\\:128MB,baz\\:200MB,boo\\:128MB,foo\\:1GB,main\\:512MB"));
  }

  @Test
  public void unset_node_log_dir() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.log-dir=a/b",
            "-c", "stripe.1.node.2.log-dir=c/d"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.log-dir=a/b"),
            containsOutput("stripe.1.node.2.log-dir=c/d")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.log-dir",
            "-c", "stripe.1.node.2.log-dir"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("log-dir=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "log-dir=e/f"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.log-dir=e/f"),
            containsOutput("stripe.1.node.2.log-dir=e/f")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "log-dir"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("log-dir=")));
  }

  @Test
  public void unset_client_reconnect() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=1s"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("client-reconnect-window=1s"));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("client-reconnect-window=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=120s"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("client-reconnect-window=120s"));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("client-reconnect-window=")));
  }

  @Test
  public void unset_client_lease() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration=1s"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("client-lease-duration=1s"));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("client-lease-duration=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration=150s"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("client-lease-duration=150s"));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("client-lease-duration=")));
  }

  @Test
  public void unset_security_log_dir() {

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.security-log-dir=a/b",
            "-c", "stripe.1.node.2.security-log-dir=c/d"),
        is(successful())
    );

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.security-log-dir=a/b"),
            containsOutput("stripe.1.node.2.security-log-dir=c/d")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.security-log-dir",
            "-c", "stripe.1.node.2.security-log-dir"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("security-log-dir=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "security-log-dir=e/f"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.security-log-dir=e/f"),
            containsOutput("stripe.1.node.2.security-log-dir=e/f")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "security-log-dir"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("security-log-dir=")));
  }
}
