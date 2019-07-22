/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

// Copy of org.terracotta.config.util.ParameterSubstitutor to avoid a client-side dependency on tc-config-parser
public class ParameterSubstitutor {
  private static String uniqueTempDirectory = null;

  public static boolean containsSubstitutionParams(String source) {
    return !Objects.equals(substitute(source), source);
  }

  public static boolean containsSubstitutionParams(Path source) {
    return !Objects.equals(substitute(source), source);
  }

  public static Path substitute(Path source) {
    if (source == null) return null;
    return Paths.get(substitute(source.toString()));
  }

  public static String substitute(String source) {
    if (source == null) return null;

    StringBuilder out = new StringBuilder();
    char[] sourceChars = source.toCharArray();

    for (int i = 0; i < sourceChars.length; ++i) {
      if (sourceChars[i] == '%') {
        char nextChar = sourceChars[++i];
        String value;

        switch (nextChar) {
          case 'd':
            value = getUniqueTempDirectory();
            break;

          case 'D':
            value = getDatestamp();
            break;

          case 'h':
            value = getHostName();
            break;
          case 'c':
            value = getCanonicalHostName();
            break;

          case 'i':
            value = getIpAddress();
            break;

          case 'H':
            value = System.getProperty("user.home");
            break;

          case 'n':
            value = System.getProperty("user.name");
            break;

          case 'o':
            value = System.getProperty("os.name");
            break;

          case 'a':
            value = System.getProperty("os.arch");
            break;

          case 'v':
            value = System.getProperty("os.version");
            break;

          case 't':
            value = System.getProperty("java.io.tmpdir");
            break;

          case '(':
            StringBuilder propertyName = new StringBuilder();
            boolean foundEnd = false;

            while (++i < sourceChars.length) {
              if (sourceChars[i] == ')') {
                foundEnd = true;
                break;
              }
              propertyName.append(sourceChars[i]);
            }

            if (foundEnd) {
              String prop = propertyName.toString();
              String defaultValue = "%(" + prop + ")";
              int index = prop.lastIndexOf(":");

              if (index > 0) {
                prop = prop.substring(0, index);
                defaultValue = prop.substring(index + 1);
              }

              value = System.getProperty(prop);
              if (value == null) value = defaultValue;
            } else {
              value = "%(" + propertyName.toString();
            }
            break;

          default:
            // don't do any substitution and preserve the original chars
            value = "%" + nextChar;
            break;
        }

        out.append(value);
      } else {
        out.append(sourceChars[i]);
      }
    }

    return out.toString();
  }

  private static synchronized String getUniqueTempDirectory() {
    if (uniqueTempDirectory == null) {
      try {
        uniqueTempDirectory = Files.createTempDirectory("terracotta").toAbsolutePath().toString();
      } catch (IOException e) {
        uniqueTempDirectory = System.getProperty("java.io.tmpdir");
      }
    }

    return uniqueTempDirectory;
  }

  private static synchronized String getDatestamp() {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    return format.format(new Date(System.currentTimeMillis()));
  }

  public static String getCanonicalHostName() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException uhe) {
      throw new RuntimeException(uhe);
    }
  }

  public static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException uhe) {
      throw new RuntimeException(uhe);
    }
  }

  public static String getIpAddress() {
    InetAddress address;
    try {
      try {
        address = InetAddress.getLocalHost();
      } catch (ArrayIndexOutOfBoundsException e) {
        // EHC-861
        address = InetAddress.getByName(null);
      }
      return address.getHostAddress();
    } catch (UnknownHostException uhe) {
      throw new RuntimeException(uhe);
    }
  }

}
