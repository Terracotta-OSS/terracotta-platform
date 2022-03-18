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

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Testing;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ConfigToolScriptExecutorTest {
  private final Node node1 = Testing.newTestNode("node-1", "localhost", 19410);
  private final Node node2 = Testing.newTestNode("node-2", "localhost", 9411);
  private StringBuilder sb;
  private Consumer<String> consumer;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void before() {
    sb = new StringBuilder();
    consumer = (s) -> sb.append(s);
  }

  @After
  public void after() {
    System.clearProperty("tc.install-root");
  }

  @Test
  public void testKitPathAsNull() {
    System.setProperty("tc.install-root", "");
    ConfigToolScriptExecutor configToolScriptExecutor = new ConfigToolScriptExecutor();
    String[] cmd = new String[]{"detach", "-t", "stripe", "-d", "localhost:" + node1.getPublicPort(), "-s",
        "localhost:" + node2.getPublicPort()};
    try {
      configToolScriptExecutor.execCommand(cmd, consumer);
      fail();
    } catch (Throwable t) {
      String msg = "Cannot run config-tool script automatically as config-tool path is null";
      Assert.assertEquals(msg, t.getMessage());
    }
  }

  @Test
  public void testWithRandomConfigToolPath() {
    System.setProperty("tc.install-root", System.getProperty("user.dir"));
    ConfigToolScriptExecutor configToolScriptExecutor = new ConfigToolScriptExecutor();
    String[] cmd = new String[]{"detach", "-t", "stripe", "-d", "localhost:" + node1.getPublicPort(), "-s",
        "localhost:" + node2.getPublicPort()};
    try {
      configToolScriptExecutor.execCommand(cmd, consumer);
      fail();
    } catch (Exception e) {
      //do nothing
    }
  }

  @Test
  public void testDummyScriptExecutionAndLoggerConsumer() throws Exception {
    Path toolsDir = temporaryFolder.newFolder().toPath().resolve("tools");
    Path binDir = toolsDir.resolve("bin");
    Files.createDirectories(binDir);
    installScript("/tools/bin/config-tool.bat", binDir);
    installScript("/tools/bin/config-tool.sh", binDir);

    System.setProperty("tc.install-root", toolsDir.toString());
    ConfigToolScriptExecutor configToolScriptExecutor = new ConfigToolScriptExecutor();
    String[] cmd = new String[]{"detach", "-t", "stripe", "-d", "localhost:" + node1.getInternalAddress().toString(),
        "-s", node2.getInternalAddress().toString()};
    int exitStatus = configToolScriptExecutor.execCommand(cmd, consumer);

    assertThat(sb.toString(), containsString("starting the script"));
    assertThat(sb.toString(), containsString("error:"));
    assertThat(exitStatus, is(2));
  }

  private Path installScript(String scriptResource, Path binPath) throws IOException {
    URL scriptFile = this.getClass().getResource(scriptResource);
    if (scriptFile == null) {
      throw new AssertionError("Unable to locate " + scriptResource + " as a resource");
    }

    URI uri;
    try {
      uri = scriptFile.toURI();
    } catch (Exception e) {
      throw new AssertionError("Unexpected error converting \"" + scriptFile + "\" to a Path", e);
    }

    Path scriptPath;
    try {
      scriptPath = Paths.get(uri);
    } catch (Exception e) {
      throw new AssertionError("Unexpected error converting \"" + scriptFile + "\" to a Path; uri=" + uri, e);
    }
    Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.GROUP_READ,
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.OWNER_READ,
        PosixFilePermission.OWNER_WRITE
    );
    Path f = Files.copy(scriptPath, binPath.resolve(scriptPath.getFileName()), StandardCopyOption.COPY_ATTRIBUTES);
    try {
      Files.setPosixFilePermissions(f, perms);
    } catch (UnsupportedOperationException ignored) {
    }
    return f;
  }

}
