/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.system_tests.util;

import com.terracottatech.tools.detailed.state.LogicalServerState;
import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.proc.AnyProcess;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

/**
 * @author Mathieu Carbou
 */
public class NodeProcess implements Closeable {

  private final AnyProcess process;
  private final PidListener pidListener;
  private final ServerStateListener serverStateListener;

  public NodeProcess(AnyProcess process, PidListener pidListener, ServerStateListener serverStateListener) {
    this.process = process;
    this.pidListener = pidListener;
    this.serverStateListener = serverStateListener;
  }

  @Override
  public void close() {
    kill();
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private void kill() {
    while (pidListener.pid == -1 && process.isRunning()) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    try {
      if (Env.isWindows()) {
        Runtime.getRuntime().exec("taskkill /F /t /pid " + pidListener.pid);
      } else {
        Runtime.getRuntime().exec("kill " + pidListener.pid);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public LogicalServerState getServerState() {
    return serverStateListener.state;
  }

  public static NodeProcess startNode(Path kitInstallationPath, Path workingDir, String... cli) {
    Path script = kitInstallationPath
        .resolve("server")
        .resolve("bin")
        .resolve("start-node." + (Env.isWindows() ? "bat" : "sh"));
    return start(script, workingDir, cli);
  }

  public static NodeProcess startTcServer(Path kitInstallationPath, Path workingDir, String... cli) {
    Path script = kitInstallationPath
        .resolve("server")
        .resolve("bin")
        .resolve("start-tc-server." + (Env.isWindows() ? "bat" : "sh"));
    return start(script, workingDir, cli);
  }

  private static NodeProcess start(Path scriptPath, Path workingDir, String... args) {
    if (!Files.exists(scriptPath)) {
      throw new IllegalArgumentException("Terracotta server start script does not exist: " + scriptPath);
    }
    PidListener pidListener = new PidListener();
    ServerStateListener serverStateListener = new ServerStateListener();
    EventBus serverBus = new EventBus.Builder().id("server-bus").build();
    serverBus.on("PID", pidListener);
    serverBus.on("STATE", serverStateListener);

    Map<String, String> eventMap = new HashMap<String, String>();
    eventMap.put("PID is", "PID");
    eventMap.put("Moved to State", "STATE");

    try {
      Files.createDirectories(workingDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    AnyProcess process = AnyProcess.newBuilder()
        .workingDir(workingDir.toFile())
        .command(concat(of(scriptPath.toString()), of(args)).toArray(String[]::new))
        .pipeStdout(new SimpleEventingStream(serverBus, eventMap, System.out))
        .pipeStderr()
        .build();

    return new NodeProcess(process, pidListener, serverStateListener);
  }

  private static class PidListener implements EventListener {
    private volatile long pid = -1;

    @Override
    public void onEvent(Event event) {
      String line = event.getData(String.class);
      Matcher m = Pattern.compile("PID is ([0-9]*)").matcher(line);
      if (m.find()) {
        try {
          PidListener.this.pid = Long.parseLong(m.group(1));
        } catch (NumberFormatException ignored) {
        }
      }
    }
  }

  private static class ServerStateListener implements EventListener {
    private volatile LogicalServerState state = LogicalServerState.UNKNOWN;

    @Override
    public void onEvent(Event event) {
      String line = event.getData(String.class);
      Matcher m = Pattern.compile("Moved to State\\[ ([A-Z\\-_]+) \\]").matcher(line);
      if (m.find()) {
        ServerStateListener.this.state = LogicalServerState.parse(m.group(1));
      }
    }
  }

}
