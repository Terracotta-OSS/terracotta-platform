package org.terracotta.dynamic_config.cli.config_tool;

import org.junit.Test;

public class ConfigToolTest {
  @Test
  public void testBasicCommand() {
    ConfigTool.main("activate", "-h");
  }
}