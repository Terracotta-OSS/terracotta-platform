/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Utility class used to write a property file without the date header and with the properties sorted
 *
 * @author Mathieu Carbou
 */
public class Props {

  public static Properties load(Path propertiesFile) {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propertiesFile)) {
      props.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read config file: %s. Make sure the file exists and is readable" + propertiesFile.getFileName(), e);
    }
    return props;
  }

  public static void store(Writer out, Properties properties, String comment) {
    try {
      StringWriter tmp = new StringWriter();
      Properties copy = new Properties() {
        private static final long serialVersionUID = 1L;

        // used to sort the lines in the output
        @Override
        public synchronized Enumeration<Object> keys() {
          return Collections.enumeration(new TreeSet<>(properties.keySet()));
        }
      };
      copy.putAll(properties);
      copy.store(tmp, comment);
      String content = tmp.toString();
      final int secondLineStart = content.indexOf('\n') + 1;
      if (secondLineStart != 0) {
        if (comment == null) {
          content = content.substring(secondLineStart);
        } else {
          content = content.substring(0, secondLineStart) + content.substring(content.indexOf('\n', secondLineStart) + 1);
        }
      }
      out.write(content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
