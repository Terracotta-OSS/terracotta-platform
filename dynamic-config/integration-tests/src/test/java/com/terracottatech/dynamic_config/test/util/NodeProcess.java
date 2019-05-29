/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import org.terracotta.ipceventbus.event.Event;
import org.terracotta.ipceventbus.event.EventBus;
import org.terracotta.ipceventbus.event.EventListener;
import org.terracotta.ipceventbus.proc.AnyProcess;
import org.terracotta.testing.common.SimpleEventingStream;

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
  private final ProcessListener listener;

  public NodeProcess(AnyProcess process, ProcessListener listener) {
    this.process = process;
    this.listener = listener;
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
    if (listener.pid != -1) {
      try {
        if (Env.isWindows()) {
          Runtime.getRuntime().exec("taskkill /F /t /pid " + listener.pid);
        } else {
          Runtime.getRuntime().exec("kill " + listener.pid);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

  }

  public static NodeProcess startNode(Path kitInstallationPath, String... cli) {
    Path script = kitInstallationPath
        .resolve("server")
        .resolve("bin")
        .resolve("start-node." + (Env.isWindows() ? "bat" : "sh"));
    return start(script, cli);
  }

  public static NodeProcess startTcServer(Path kitInstallationPath, String... cli) {
    Path script = kitInstallationPath
        .resolve("server")
        .resolve("bin")
        .resolve("start-tc-server." + (Env.isWindows() ? "bat" : "sh"));
    return start(script, cli);
  }

  private static NodeProcess start(Path scriptPath, String... args) {
    if (!Files.exists(scriptPath)) {
      throw new IllegalArgumentException("Terracotta server start script does not exist: " + scriptPath);
    }

    ProcessListener listener = new ProcessListener();

    EventBus serverBus = new EventBus.Builder().id("server-bus").build();
    serverBus.on("PID", listener);

    Map<String, String> eventMap = new HashMap<String, String>();
    eventMap.put("PID is", "PID");

    AnyProcess process = AnyProcess.newBuilder()
        .command(concat(of(scriptPath.toString()), of(args)).toArray(String[]::new))
        .pipeStdout(new SimpleEventingStream(serverBus, eventMap, System.out))
        .pipeStderr()
        .build();

    return new NodeProcess(process, listener);
  }

  private static class ProcessListener implements EventListener {
    private volatile long pid = -1;

    @Override
    public void onEvent(Event event) {
      String line = event.getData(String.class);
      Matcher m = Pattern.compile("PID is ([0-9]*)").matcher(line);
      if (m.find()) {
        try {
          ProcessListener.this.pid = Long.parseLong(m.group(1));
        } catch (NumberFormatException ignored) {
        }
      }
    }
  }

}
