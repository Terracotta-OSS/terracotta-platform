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
package org.terracotta.dynamic_config.test_support;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.runner.Description;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigRepoProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import static org.terracotta.utilities.io.Files.copy;

public class AutoActivatingAngelaRule extends AngelaRule {
  private final Supplier<Path> tmpDir;
  private final int[] autoActivateNodes;
  private final Path licensePath;

  public AutoActivatingAngelaRule(ConfigurationContext configurationContext, boolean autoStart, int[] autoActivateNodes, Supplier<Path> tmpDir) {
    super(configurationContext, autoStart && notAutoActivated(autoActivateNodes), false);
    if (autoActivateNodes.length == 0) {
      Topology topology = configurationContext.tsa().getTopology();
      List<List<TerracottaServer>> stripes = topology.getStripes();
      this.autoActivateNodes = new int[stripes.size()];
      for (int i = 0; i < this.autoActivateNodes.length; i++) {
        this.autoActivateNodes[i] = stripes.get(i).size();
      }
    } else {
      this.autoActivateNodes = autoActivateNodes.clone();
    }
    License license = configurationContext.tsa().getLicense();
    if (license != null) {
      licensePath = Paths.get(license.getFilename());
    } else {
      licensePath = null;
    }
    this.tmpDir = tmpDir;
  }

  @Override
  protected void before(Description description) throws Throwable {
    super.before(description);
    if (notAutoActivated(autoActivateNodes)) {
      return;
    }

    generateNodeConfigs();
    startAutoActivatedNodes();
  }

  private static boolean notAutoActivated(int[] autoActivateNodes) {
    return Arrays.equals(autoActivateNodes, new int[]{-1});
  }

  private void generateNodeConfigs() {
    Cluster cluster = createClusterConfig();

    Path generatedNodeConfigs = tmpDir.get().resolve("generated-node-configs");
    new ConfigRepoProcessor(generatedNodeConfigs).process(cluster);

    for (int stripeId = 1; stripeId <= autoActivateNodes.length; stripeId++) {
      for (int nodeId = 1; nodeId <= autoActivateNodes[stripeId - 1]; nodeId++) {
        copyLicense(generatedNodeConfigs, stripeId, nodeId);
        uploadConfigDir(generatedNodeConfigs, stripeId, nodeId);
      }
    }
  }

  private void uploadConfigDir(Path generatedConfigDir, int stripeId, int nodeId) {
    try {
      RemoteFolder remoteFolder = tsa().browse(getNode(stripeId, nodeId),
                                               Paths.get("node-" + stripeId + "-" + nodeId).resolve("config").toString());
      uploadFolder(remoteFolder,
                   ".",
                   generatedConfigDir.resolve("stripe-" + stripeId).resolve("node-" + stripeId + "-" + nodeId).toFile()
      );


    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private void uploadFolder(RemoteFolder remoteFolder, String parentName, File folder) throws IOException {
    File[] files = folder.listFiles();
    for (File f : files) {
      String currentName = parentName + "/" + f.getName();
      if (f.isDirectory()) {
        uploadFolder(remoteFolder, currentName, f);
      } else {
        // excluding lock file as its causing failures on windows machines
        if (f.getName().equals("lock")) {
          continue;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
          remoteFolder.upload(currentName, fis);
        } catch (IOException e) {
          throw new IOException("Unable to upload file " + f.getAbsolutePath(), e);
        }
      }
    }
  }

  private void copyLicense(Path generatedConfigDir, int stripeId, int nodeId) {
    try {
      Path targetDirectory = generatedConfigDir.resolve("stripe-" + stripeId)
                                               .resolve("node-" + stripeId + "-" + nodeId)
                                               .resolve("license");
      if (licensePath != null) {
        copy(licensePath, targetDirectory.resolve(licensePath.getFileName()));
      } else {
        // for uploading license directory file - empty directories are ignored by Angela
        Files.createFile(targetDirectory.resolve(".dummyLicense.txt"));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @SuppressWarnings({"rawtypes"})
  private void startAutoActivatedNodes() {
    List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
    for (int stripeId = 1; stripeId <= autoActivateNodes.length; stripeId++) {
      for (int nodeId = 1; nodeId <= autoActivateNodes[stripeId - 1]; nodeId++) {
        int finalStripeId = stripeId, finalNodeId = nodeId;
        completableFutures.add(CompletableFuture.runAsync(() -> startNode(finalStripeId, finalNodeId)));
      }
    }

    try {
      CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Cluster createClusterConfig() {
    Stripe[] stripes = new Stripe[autoActivateNodes.length];
    String offHeapResources = null;
    FailoverPriority failoverPriority = FailoverPriority.availability();
    for (int stripeId = 1; stripeId <= autoActivateNodes.length; stripeId++) {
      Stripe stripe = new Stripe();
      for (int nodeId = 1; nodeId <= autoActivateNodes[stripeId - 1]; nodeId++) {
        TerracottaServer server = getNode(stripeId, nodeId);
        failoverPriority = FailoverPriority.valueOf(server.getFailoverPriority());
        offHeapResources = server.getOffheap().get(0);

        Node node = new Node().setName(server.getServerSymbolicName().getSymbolicName()).setHostname(server.getHostname())
            .setPort(server.getTsaPort())
            .setGroupPort(server.getTsaGroupPort())
            .setLogDir(RawPath.valueOf(server.getLogs()))
            .setMetadataDir(RawPath.valueOf(server.getMetaData()));
        if (server.getDataDir() != null) {
          addDataDirConfig(node, server.getDataDir().get(0));
        }
        stripe.addNode(node);
      }
      stripes[stripeId - 1] = stripe;
    }

    Cluster cluster = new Cluster(stripes).setName("tc-cluster").setFailoverPriority(failoverPriority);
    if (offHeapResources != null) {
      addOffHeapResources(cluster, offHeapResources);
    }
    return cluster;
  }

  private static void addDataDirConfig(Node node, String dataDirConfig) {
    Map<String, RawPath> map = new HashMap<>();
    for (String dataDir : dataDirConfig.split(",")) {
      String[] tokens = dataDir.split(":");
      map.put(tokens[0], RawPath.valueOf(tokens[1]));
    }
    node.setDataDirs(map);
  }

  private static void addOffHeapResources(Cluster cluster, String offHeapResources) {
    Map<String, Measure<MemoryUnit>> map = new HashMap<>();
    for (String offHeap : offHeapResources.split(",")) {
      String[] tokens = offHeap.split(":");
      map.put(tokens[0], Measure.parse(tokens[1], MemoryUnit.class));
    }
    cluster.setOffheapResources(map);
  }
}
