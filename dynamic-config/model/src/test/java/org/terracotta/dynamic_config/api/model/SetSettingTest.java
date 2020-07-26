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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;
import org.terracotta.common.struct.Measure;

import java.nio.file.Paths;

import static java.io.File.separator;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.MemoryUnit.MB;
import static org.terracotta.dynamic_config.api.model.Setting.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.Setting.LICENSE_FILE;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.TC_PROPERTIES;

/**
 * only test the necessary setters having some logic
 *
 * @author Mathieu Carbou
 */
public class SetSettingTest {

  @Test
  public void test_setProperty_NODE_BACKUP_DIR() {
    Node node = Testing.newTestNode("localhost");
    NODE_BACKUP_DIR.setProperty(node, ".");
    assertThat(node.getBackupDir(), is(equalTo(Paths.get("."))));
    NODE_BACKUP_DIR.setProperty(node, null);
    assertNull(node.getBackupDir());
  }

  @Test
  public void test_setProperty_SECURITY_DIR() {
    Node node = Testing.newTestNode("localhost");
    SECURITY_DIR.setProperty(node, ".");
    assertThat(node.getSecurityDir(), is(equalTo(Paths.get("."))));
    SECURITY_DIR.setProperty(node, null);
    assertNull(node.getSecurityDir());
  }

  @Test
  public void test_setProperty_SECURITY_AUDIT_LOG_DIR() {
    Node node = Testing.newTestNode("localhost");
    SECURITY_AUDIT_LOG_DIR.setProperty(node, ".");
    assertThat(node.getSecurityAuditLogDir(), is(equalTo(Paths.get("."))));
    SECURITY_AUDIT_LOG_DIR.setProperty(node, null);
    assertNull(node.getSecurityAuditLogDir());
  }

  @Test
  public void test_setProperty_LICENSE_FILE() {
    Node node = Testing.newTestNode("localhost");

    // not throwing - noop
    LICENSE_FILE.setProperty(node, null);
    LICENSE_FILE.setProperty(node, "a.xml");
  }

  @Test
  public void test_setProperty_OFFHEAP_RESOURCES() {
    Cluster cluster = Testing.newTestCluster();
    assertThat(cluster.getOffheapResources().size(), is(equalTo(1)));
    assertThat(cluster.getOffheapResources().get("main"), is(equalTo(Measure.of(512, MB))));

    OFFHEAP_RESOURCES.setProperty(cluster, null);
    assertThat(cluster.getOffheapResources().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster("localhost");
    OFFHEAP_RESOURCES.setProperty(cluster, null, null);
    assertThat(cluster.getOffheapResources().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster("localhost");
    OFFHEAP_RESOURCES.setProperty(cluster, "main", null);
    assertThat(cluster.getOffheapResources().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster("localhost");
    OFFHEAP_RESOURCES.setProperty(cluster, "main", "1GB");
    assertThat(cluster.getOffheapResources().size(), is(equalTo(1)));
    assertThat(cluster.getOffheapResources().get("main"), is(equalTo(Measure.of(1, GB))));

    cluster = Testing.newTestCluster("localhost");
    OFFHEAP_RESOURCES.setProperty(cluster, null, "main:1GB");
    assertThat(cluster.getOffheapResources().size(), is(equalTo(1)));
    assertThat(cluster.getOffheapResources().get("main"), is(equalTo(Measure.of(1, GB))));

    cluster = Testing.newTestCluster("localhost");
    OFFHEAP_RESOURCES.setProperty(cluster, null, "main:1GB,second:2GB");
    assertThat(cluster.getOffheapResources().size(), is(equalTo(2)));
    assertThat(cluster.getOffheapResources().get("main"), is(equalTo(Measure.of(1, GB))));
    assertThat(cluster.getOffheapResources().get("second"), is(equalTo(Measure.of(2, GB))));
  }

  @Test
  public void test_setProperty_DATA_DIRS() {
    Node node = Testing.newTestNode("localhost");
    assertThat(node.getDataDirs().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("%H" + separator + "terracotta" + separator + "user-data" + separator + "main"))));

    DATA_DIRS.setProperty(node, null);
    assertThat(node.getDataDirs().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost");
    DATA_DIRS.setProperty(node, null, null);
    assertThat(node.getDataDirs().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost");
    DATA_DIRS.setProperty(node, "main", null);
    assertThat(node.getDataDirs().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost");
    DATA_DIRS.setProperty(node, "main", "foo/bar");
    assertThat(node.getDataDirs().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("foo/bar"))));

    node = Testing.newTestNode("localhost");
    DATA_DIRS.setProperty(node, null, "main:foo/bar");
    assertThat(node.getDataDirs().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("foo/bar"))));

    node = Testing.newTestNode("localhost");
    DATA_DIRS.setProperty(node, null, "main:foo/bar,second:foo/baz");
    assertThat(node.getDataDirs().size(), is(equalTo(2)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("foo/bar"))));
    assertThat(node.getDataDirs().get("second"), is(equalTo(Paths.get("foo/baz"))));
  }

  @Test
  public void test_setProperty_TC_PROPERTIES() {
    Node node = Testing.newTestNode("localhost").putTcProperty("foo", "bar");
    assertThat(node.getTcProperties().size(), is(equalTo(1)));

    TC_PROPERTIES.setProperty(node, null);
    assertThat(node.getTcProperties().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost").putTcProperty("foo", "bar");
    TC_PROPERTIES.setProperty(node, null, null);
    assertThat(node.getTcProperties().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, "main", null);
    assertThat(node.getTcProperties().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, "main", "baz");
    assertThat(node.getTcProperties().size(), is(equalTo(1)));
    assertThat(node.getTcProperties().get("main"), is(equalTo("baz")));

    node = Testing.newTestNode("localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, null, "main:baz");
    assertThat(node.getTcProperties().size(), is(equalTo(1)));
    assertThat(node.getTcProperties().get("main"), is(equalTo("baz")));

    node = Testing.newTestNode("localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, null, "main:baz1,second:baz1");
    assertThat(node.getTcProperties().size(), is(equalTo(2)));
    assertThat(node.getTcProperties().get("main"), is(equalTo("baz1")));
    assertThat(node.getTcProperties().get("second"), is(equalTo("baz1")));
  }

  @Test
  public void test_setProperty_NODE_LOGGER_OVERRIDES() {
    Node node = Testing.newTestNode("localhost").putLoggerOverride("com.foo", "TRACE");
    assertThat(node.getLoggerOverrides().size(), is(equalTo(1)));

    NODE_LOGGER_OVERRIDES.setProperty(node, null);
    assertThat(node.getLoggerOverrides().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost").putLoggerOverride("foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, null, null);
    assertThat(node.getLoggerOverrides().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, "com.foo", null);
    assertThat(node.getLoggerOverrides().size(), is(equalTo(0)));

    node = Testing.newTestNode("localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, "com.foo", "INFO");
    assertThat(node.getLoggerOverrides().size(), is(equalTo(1)));
    assertThat(node.getLoggerOverrides().get("com.foo"), is("INFO"));

    node = Testing.newTestNode("localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, null, "com.foo:INFO");
    assertThat(node.getLoggerOverrides().size(), is(equalTo(1)));
    assertThat(node.getLoggerOverrides().get("com.foo"), is("INFO"));

    node = Testing.newTestNode("localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, null, "com.foo:INFO,com.bar:WARN");
    assertThat(node.getLoggerOverrides().size(), is(equalTo(2)));
    assertThat(node.getLoggerOverrides().get("com.foo"), is("INFO"));
    assertThat(node.getLoggerOverrides().get("com.bar"), is("WARN"));
  }
}