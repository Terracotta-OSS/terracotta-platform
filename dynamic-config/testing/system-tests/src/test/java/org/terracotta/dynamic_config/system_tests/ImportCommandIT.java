/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Props;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;

import java.nio.file.Path;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@ClusterDefinition(stripes = 2)
public class ImportCommandIT extends DynamicConfigIT {
  @Test
  public void test_import() throws Exception {
    TreeMap<Object, Object> before = new TreeMap<>(getUpcomingCluster("localhost", getNodePort()).toProperties());
    Path path = copyConfigProperty("/config-property-files/import.properties");
    ConfigTool.start("import", "-f", path.toString());
    assertCommandSuccessful();
    TreeMap<Object, Object> after = new TreeMap<>(getUpcomingCluster("localhost", getNodePort()).toProperties());
    TreeMap<Object, Object> expected = new TreeMap<>(Props.load(path));
    assertThat(after, is(equalTo(expected)));
    assertThat(before, is(not(equalTo(expected))));
  }
}
