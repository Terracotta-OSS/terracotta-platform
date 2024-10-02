/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, failoverPriority = "")
public class UnsetCommand1x2IT extends DynamicConfigIT {

  @Before
  public void setUp() throws Exception {
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
  }

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
  public void unset_cluster_name() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=foo"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("cluster-name=foo"));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "cluster-name"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("cluster-name=")));
  }

  @Test
  public void unset_backup_dir() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.backup-dir=foo",
            "-c", "stripe.1.node.2.backup-dir=bar"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.backup-dir=foo"),
            containsOutput("stripe.1.node.2.backup-dir=bar")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.backup-dir",
            "-c", "stripe.1.node.2.backup-dir"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("backup-dir=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "backup-dir=foo"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.backup-dir=foo"),
            containsOutput("stripe.1.node.2.backup-dir=foo")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "backup-dir"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("backup-dir=")));
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
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=foo:64MB,bar:128MB"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=bar\\:128MB,foo\\:64MB,main\\:512MB"));

    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=foo:128MB,baz:200MB"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=bar\\:128MB,baz\\:200MB,foo\\:128MB,main\\:512MB"));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=bar:128MB,baz:200MB,foo:128MB,main:512MB"));

    // removing a specific property
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.bar"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=baz\\:200MB,foo\\:128MB,main\\:512MB"));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=baz:200MB,foo:128MB,main:512MB"));

    // global removal of a whole map
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("offheap-resources=")); // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=")); // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
  }

  @Test
  public void unset_data_dirs() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=foo:a/b,bar:c/d"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("data-dirs=bar\\:c/d,foo\\:a/b"));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        containsOutput("data-dirs=bar:c/d,foo:a/b"));
    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=foo:c/d,baz:e/f"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("data-dirs=bar\\:c/d,baz\\:e/f,foo\\:c/d"));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        containsOutput("data-dirs=bar:c/d,baz:e/f,foo:c/d"));

    // removing a specific property
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.bar"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("data-dirs=baz\\:e/f,foo\\:c/d"));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        containsOutput("data-dirs=baz:e/f,foo:c/d"));

    // global removal of a whole map
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        is(successful()));
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("data-dirs=")); // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs"),
        containsOutput("data-dirs=")); // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
  }

  @Test
  public void unset_node_group_port() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.group-port=9112",
            "-c", "stripe.1.node.2.group-port=9113"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.group-port=9112"),
            containsOutput("stripe.1.node.2.group-port=9113")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.group-port",
            "-c", "stripe.1.node.2.group-port"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("group-port=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "group-port=9111"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.group-port=9111"),
            containsOutput("stripe.1.node.2.group-port=9111")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "group-port"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("group-port=")));
  }

  @Test
  public void unset_node_bind_address() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.bind-address=127.0.0.1",
            "-c", "stripe.1.node.2.bind-address=127.0.0.1"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.bind-address=127.0.0.1"),
            containsOutput("stripe.1.node.2.bind-address=127.0.0.1")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.bind-address",
            "-c", "stripe.1.node.2.bind-address"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("bind-address=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "bind-address=127.0.0.1"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.bind-address=127.0.0.1"),
            containsOutput("stripe.1.node.2.bind-address=127.0.0.1")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "bind-address"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("bind-address=")));
  }

  @Test
  public void unset_node_group_bind_address() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.group-bind-address=127.0.0.1",
            "-c", "stripe.1.node.2.group-bind-address=127.0.0.1"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.group-bind-address=127.0.0.1"),
            containsOutput("stripe.1.node.2.group-bind-address=127.0.0.1")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.group-bind-address",
            "-c", "stripe.1.node.2.group-bind-address"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("group-bind-address=")));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "group-bind-address=127.0.0.1"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        allOf(
            containsOutput("stripe.1.node.1.group-bind-address=127.0.0.1"),
            containsOutput("stripe.1.node.2.group-bind-address=127.0.0.1")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "group-bind-address"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        not(containsOutput("group-bind-address=")));
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
        configTool("export", "-s", "localhost:" + getNodePort()),
        containsOutput("client-lease-duration=150s"));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "client-lease-duration"),
        is(successful()));

    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort()),
        not(containsOutput("client-lease-duration=")));
  }

  @Test
  public void unset_tc_properties_new_cfg() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:1,bar:2"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(
            containsOutput("node-1-1:tc-properties=bar:2,foo:1"),
            containsOutput("node-1-2:tc-properties=bar:2,foo:1")
        ));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties=foo:2,baz:3"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(
            containsOutput("node-1-1:tc-properties=bar:2,baz:3,foo:2"),
            containsOutput("node-1-2:tc-properties=bar:2,baz:3,foo:2")
        ));

    // removing a specific property
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "node-1-1:tc-properties.bar",
            "-c", "node-1-2:tc-properties.baz"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(
            containsOutput("node-1-1:tc-properties=baz:3,foo:2"),
            containsOutput("node-1-2:tc-properties=bar:2,foo:2")
        ));

    // global removal of a whole map
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "node-1-1:tc-properties",
            "-c", "node-1-2:tc-properties.baz"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(
            not(containsOutput("node-1-1:tc-properties=")), // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            containsOutput("node-1-2:tc-properties=bar:2,foo:2")
        ));

    // global removal on cluster
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "node-1-1:tc-properties=bar:2,foo:1"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(
            containsOutput("node:node-1-1:tc-properties=bar:2,foo:1"),
            containsOutput("node:node-1-2:tc-properties=bar:2,foo:2")
        ));

    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(
            // these entries are in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            not(containsOutput("node:node-1-1:tc-properties=")),
            not(containsOutput("node:node-1-2:tc-properties="))
        ));
  }

  @Test
  public void unset_logger_overrides_new_cfg() {
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=foo:DEBUG,bar:INFO"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(
            containsOutput("node:node-1-1:logger-overrides=bar:INFO,foo:DEBUG"),
            containsOutput("node:node-1-2:logger-overrides=bar:INFO,foo:DEBUG")
        ));

    // ===
    // IMPORTANT: we do not support to globally replace a map by another map, to prevent any mistake from the user
    // ===
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=foo:INFO,baz:WARN"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(
            containsOutput("node:node-1-1:logger-overrides=bar:INFO,baz:WARN,foo:INFO"),
            containsOutput("node:node-1-2:logger-overrides=bar:INFO,baz:WARN,foo:INFO")
        ));

    // removing a specific property
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "node-1-1:logger-overrides.bar",
            "-c", "node-1-2:logger-overrides.baz"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(
            containsOutput("node:node-1-1:logger-overrides=baz:WARN,foo:INFO"),
            containsOutput("node:node-1-2:logger-overrides=bar:INFO,foo:INFO")
        ));

    // global removal of a whole map
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(),
            "-c", "node-1-1:logger-overrides",
            "-c", "node:node-1-2:logger-overrides.baz"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(
            not(containsOutput("node-1-1:logger-overrides=")), // this entry is in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            containsOutput("node-1-2:logger-overrides=bar:INFO,foo:INFO")
        ));

    // global removal on cluster
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "node:node-1-1:logger-overrides=bar:INFO,foo:DEBUG"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(
            containsOutput("node-1-1:logger-overrides=bar:INFO,foo:DEBUG"),
            containsOutput("node-1-2:logger-overrides=bar:INFO,foo:INFO")
        ));
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        is(successful()));
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides"),
        allOf(
            // these entries are in the output because the user has explicitly set the map to "empty" So it is exported in the config.
            not(containsOutput("node-1-1:logger-overrides=")),
            not(containsOutput("node-1-2:logger-overrides="))
        ));
  }
}
