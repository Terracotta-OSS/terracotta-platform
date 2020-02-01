/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.util;

import org.terracotta.ipceventbus.event.EventBus;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Scans a stream, as it is produced, to produce events for an event bus (note that the events are triggered on the same
 * thread doing the stream processing).
 */
public class SimpleEventingStream extends OutputStream {

  private static final byte EOL = (byte) '\n';

  private final EventBus eventBus;
  private final String[] triggers;
  private final OutputStream nextConsumer;
  private final ByteBuffer buffer;

  public SimpleEventingStream(OutputStream nextConsumer, int bufferSize, EventBus eventBus, String... triggers) {
    this.eventBus = eventBus;
    this.triggers = triggers;
    this.nextConsumer = nextConsumer;
    this.buffer = ByteBuffer.allocate(bufferSize);
  }

  @Override
  public void write(int b) throws IOException {
    nextConsumer.write(b);
    // we only continue filling the buffer if we have some space left
    if (buffer.hasRemaining()) {
      buffer.put((byte) b);
    }
    // when we reach EOL, we check the buffer
    if (b == EOL) {
      String line = new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
      for (String trigger : triggers) {
        if (line.contains(trigger)) {
          eventBus.trigger(trigger, line);
        }
      }
      buffer.clear();
    }
  }

  @Override
  public void close() throws IOException {
    this.nextConsumer.close();
  }
}