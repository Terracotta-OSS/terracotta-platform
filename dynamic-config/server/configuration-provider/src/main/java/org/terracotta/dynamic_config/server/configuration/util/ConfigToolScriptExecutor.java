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
package org.terracotta.dynamic_config.server.configuration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

public class ConfigToolScriptExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigToolScriptExecutor.class);

  private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
  private static final Path CONFIG_TOOL = Paths.get("tools", "bin", "config-tool"
      + (IS_WINDOWS ? ".bat" : ".sh"));

  public int execCommand(String[] command, Consumer<String> consumer) throws InterruptedException, IOException {
    Process p = createProcess(command);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
      String line;
      while ((line = reader.readLine()) != null) {
          consumer.accept(line);
      }
      return p.waitFor();
    } finally {
      p.destroy();
    }
  }

  private Process createProcess(String[] command) throws IOException {
    ProcessBuilder builder = new ProcessBuilder(getConfigToolPath().toString());
    builder.command().addAll(asList(command));
    builder.redirectErrorStream(true);
    return builder.start();
  }

  private Path getConfigToolPath() {
    String path = System.getProperty("tc.install-root");
    if (path == null) {
      throw new AssertionError("Cannot run config-tool script automatically as tc.install-root property" +
          " is not set");
    }
    Path kitPath = Paths.get(path).getParent();
    if (kitPath == null) {
      throw new AssertionError("Cannot run config-tool script automatically as config-tool path is null");
    }
    Path configToolPath = kitPath.resolve(CONFIG_TOOL);
    LOGGER.debug("Config-tool path :" + configToolPath);
    return configToolPath;
  }
}

