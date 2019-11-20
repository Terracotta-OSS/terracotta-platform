/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsEntity;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Properties;

import static java.lang.System.lineSeparator;

public class ReadNotifications {
  public static void main(String[] args) throws ConnectionException, EntityConfigurationException, IOException, InterruptedException {
    String connectionName = ReadNotifications.class.getSimpleName();
    String entityName = ReadNotifications.class.getSimpleName();
    String uri = args.length == 1 ? args[0] : "stripe://localhost:9410";

    // we connect to stripe 1
    System.out.println("Connecting to " + uri);
    try (Connection connection = createConnection(connectionName, uri)) {
      NmsService nmsService = createNmsService(connection, entityName, 1);
      System.out.println("Reading messages...");
      while (!Thread.currentThread().isInterrupted()) {
        Message message = nmsService.waitForMessage();
        switch (message.getType()) {
          case "NOTIFICATION":
            message.unwrap(ContextualNotification.class).forEach(notification ->
                System.out.println("NOTIFICATION RECEIVED:" + lineSeparator() +
                    " - TIME: " + Instant.ofEpochMilli(message.getTimestamp()) + lineSeparator() +
                    " - SEQ: " + message.getSequence() + lineSeparator() +
                    " - TYPE: " + notification.getType() + lineSeparator() +
                    " - FROM: " + notification.getContext() + lineSeparator() +
                    " - DATA: " + notification.getAttributes()));
            break;
          default:
            System.out.println("Unsupported message: " + message);
        }
      }
    }
  }

  private static Connection createConnection(String name, String uri) throws ConnectionException {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, name);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    return ConnectionFactory.connect(URI.create(uri), properties);
  }

  private static NmsService createNmsService(Connection connection, String entityName, int stripeId) throws EntityConfigurationException {
    NmsEntityFactory factory = new NmsEntityFactory(connection, entityName);
    NmsEntity nmsEntity = factory.retrieveOrCreate(new NmsConfig().setStripeName("stripe-" + stripeId));
    return new DefaultNmsService(nmsEntity);
  }
}
