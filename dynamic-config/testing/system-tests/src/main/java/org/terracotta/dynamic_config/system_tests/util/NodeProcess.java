/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.LogicalServerState;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.proc.AnyProcess;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

/**
 * @author Mathieu Carbou
 */
public class NodeProcess implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeProcess.class);

  private final int stripeId;
  private final int nodeId;

  private volatile AnyProcess process;
  private volatile long pid = -1;
  private volatile LogicalServerState state = LogicalServerState.UNKNOWN;

  public NodeProcess(int stripeId, int nodeId) {
    this.stripeId = stripeId;
    this.nodeId = nodeId;
  }

  @Override
  public String toString() {
    return getID();
  }

  public String getID() {
    return "Node[" + stripeId + "-" + nodeId + "]";
  }

  public int getStripeId() {
    return stripeId;
  }

  public int getNodeId() {
    return nodeId;
  }

  public long getPid() {
    while (pid == -1 && process.isRunning()) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    return pid;
  }

  public LogicalServerState getServerState() {
    while (state == LogicalServerState.UNKNOWN && process.isRunning()) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    return state;
  }

  @Override
  public void close() {
    if (process.isRunning()) {
      kill();
      try {
        process.waitFor();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
  }

  private void kill() {
    long pid = getPid();
    LOGGER.info("{} Killing PID: {}", this, pid);
    try {
      if (Env.isWindows()) {
        Runtime.getRuntime().exec("taskkill /F /t /pid " + pid);
      } else {
        Runtime.getRuntime().exec("kill " + pid);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static NodeProcess startNode(int stripeId, int nodeId, Path kitInstallationPath, Path workingDir, String... cli) {
    Path script = kitInstallationPath
        .resolve("server")
        .resolve("bin")
        .resolve("start-node." + (Env.isWindows() ? "bat" : "sh"));
    return start(stripeId, nodeId, script, workingDir, cli);
  }

  private static NodeProcess start(int stripeId, int nodeId, Path scriptPath, Path workingDir, String... args) {
    if (!Files.exists(scriptPath)) {
      throw new IllegalArgumentException("Terracotta server start script does not exist: " + scriptPath);
    }

    NodeProcess nodeProcess = new NodeProcess(stripeId, nodeId);
    EventBus serverBus = new EventBus.Builder().id("server-bus").build();

    // event triggered when a log line contains PID
    serverBus.on("PID is", event -> {
      String line = event.getData(String.class);
      Matcher m = Pattern.compile("PID is ([0-9]*)").matcher(line);
      if (m.find()) {
        try {
          nodeProcess.pid = Long.parseLong(m.group(1));
          nodeProcess.state = LogicalServerState.UNKNOWN;
          LOGGER.info("{} Discovered PID: {}", nodeProcess, nodeProcess.pid);
        } catch (NumberFormatException ignored) {
        }
      } else {
        throw new AssertionError("please refine regex to not match: " + line);
      }
    });

    // event triggered when server moves to a state
    serverBus.on("Moved to State", event -> {
      String line = event.getData(String.class);
      Matcher m = Pattern.compile("Moved to State\\[ ([A-Z\\-_]+) \\]").matcher(line);
      if (m.find()) {
        nodeProcess.state = LogicalServerState.parse(m.group(1));
        LOGGER.info("{} Discovered state: {}", nodeProcess, nodeProcess.state);
      } else {
        throw new AssertionError("please refine regex to not match: " + line);
      }
    });

    try {
      Files.createDirectories(workingDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    LOGGER.info("{} Starting...", nodeProcess);

    nodeProcess.process = AnyProcess.newBuilder()
        .workingDir(workingDir.toFile())
        .command(concat(of(scriptPath.toString()), of(args)).toArray(String[]::new))
        .pipeStdout(new SimpleEventingStream(System.out, 4096, serverBus,
            "Terracotta Server instance has started up as ACTIVE node on",
            "PID is",
            "Moved to State"))
        .pipeStderr()
        .build();

    return nodeProcess;
  }
}
