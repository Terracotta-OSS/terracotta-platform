/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.system_tests.util;

import org.terracotta.ipceventbus.event.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Scans a stream, as it is produced, to produce events for an event bus (note that the events are triggered on the same
 * thread doing the stream processing).
 */
public class SimpleEventingStream extends OutputStream {
  private final EventBus outputBus;
  private final Map<String, String> eventMap;
  private final OutputStream nextConsumer;
  private final ByteArrayOutputStream stream;

  private final boolean twoByteLineSeparator;
  private final byte eol;
  private final byte eolLeader;

  private boolean haveLeader = false;

  public SimpleEventingStream(EventBus outputBus, Map<String, String> eventMap, OutputStream nextConsumer) {
    this.outputBus = outputBus;
    this.eventMap = eventMap;
    this.nextConsumer = nextConsumer;
    this.stream = new ByteArrayOutputStream();

    String lineSeparator = System.lineSeparator();
    this.twoByteLineSeparator = lineSeparator.length() == 2;
    this.eol = (byte) lineSeparator.charAt(lineSeparator.length() - 1);
    this.eolLeader = (byte) (this.twoByteLineSeparator ? lineSeparator.charAt(0) : '\0');
  }

  @Override
  public void write(int b) throws IOException {
    if (twoByteLineSeparator && eolLeader == (byte) b) {
      haveLeader = true;
    } else {
      if (eol == (byte) b) {
        // End of line so we process the bytes to find matches and then replace it.
        // NOTE:  This will use the platform's default encoding.
        String oneLine = this.stream.toString("UTF-8");
        // Determine what events to trigger by scraping the string for keys.
        for (Map.Entry<String, String> pair : this.eventMap.entrySet()) {
          if (oneLine.contains(pair.getKey())) {
            this.outputBus.trigger(pair.getValue(), oneLine);
          }
        }
        // Start the next line.
        this.stream.reset();
      } else {
        if (haveLeader) {
          this.stream.write(eolLeader);
        }
        this.stream.write(b);
      }
      haveLeader = false;
      this.nextConsumer.write(b);
    }
  }

  @Override
  public void close() throws IOException {
    this.nextConsumer.close();
  }
}