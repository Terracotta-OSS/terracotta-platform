/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.server.configuration.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.terracotta.common.struct.Tuple2.tuple2;

/**
 * Copy from platform ParameterSubstitutor
 */
public class ParameterSubstitutor implements IParameterSubstitutor {
  private static final Map<Character, Tuple2<Supplier<String>, String>> ALL_PARAMS = new LinkedHashMap<>();

  static {
    ALL_PARAMS.put('D', tuple2(ParameterSubstitutor::getDatestamp, "date stamp corresponding to current time"));
    ALL_PARAMS.put('H', tuple2(() -> System.getProperty("user.home"), "home directory of the user. Same as java 'user.home' property"));
    ALL_PARAMS.put('a', tuple2(() -> System.getProperty("os.arch"), "architecture of the machine. Same as java 'os.arch' property"));
    ALL_PARAMS.put('c', tuple2(ParameterSubstitutor::getCanonicalHostName, "canonical host name of the machine"));
    ALL_PARAMS.put('d', tuple2(ParameterSubstitutor::getUniqueTempDirectory, "unique temporary directory"));
    ALL_PARAMS.put('h', tuple2(ParameterSubstitutor::getHostName, "host name of the machine"));
    ALL_PARAMS.put('i', tuple2(ParameterSubstitutor::getIpAddress, "IP address of the machine corresponding to localhost"));
    ALL_PARAMS.put('n', tuple2(() -> System.getProperty("user.name"), "username of the user. Same as java 'user.name' property"));
    ALL_PARAMS.put('o', tuple2(() -> System.getProperty("os.name"), "name of the operating system. Same as java 'os.name' property"));
    ALL_PARAMS.put('v', tuple2(() -> System.getProperty("os.version"), "version of the operating system. Same as java 'os.version' property"));
    ALL_PARAMS.put('t', tuple2(() -> System.getProperty("java.io.tmpdir"), "temporary directory of the machine. Same as java 'java.io.tmpdir' property"));
  }

  private static String uniqueTempDirectory = null;

  @Override
  public String substitute(String source) {
    if (source == null) return null;

    StringBuilder out = new StringBuilder();
    char[] sourceChars = source.toCharArray();

    for (int i = 0; i < sourceChars.length; ++i) {
      if (sourceChars[i] == '%') {
        char nextChar = sourceChars[++i];
        String value;

        switch (nextChar) {
          case 'd':
          case 'v':
          case 'a':
          case 'o':
          case 'n':
          case 'H':
          case 'i':
          case 'c':
          case 'h':
          case 'D':
          case 't':
            value = ALL_PARAMS.get(nextChar).getT1().get();
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
              value = "%(" + propertyName;
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

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private static String getUniqueTempDirectory() {
    if (uniqueTempDirectory != null) {
      return uniqueTempDirectory;
    }
    synchronized (ParameterSubstitutor.class) {
      if (uniqueTempDirectory != null) {
        return uniqueTempDirectory;
      }
      try {
        File theFile = File.createTempFile("terracotta", "data");
        theFile.delete();
        if (!theFile.mkdir()) {
          uniqueTempDirectory = System.getProperty("java.io.tmpdir");
        } else {
          uniqueTempDirectory = theFile.getAbsolutePath();
        }
      } catch (IOException ioe) {
        uniqueTempDirectory = System.getProperty("java.io.tmpdir");
      }
      return uniqueTempDirectory;
    }
  }

  private static String getDatestamp() {
    return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(System.currentTimeMillis()));
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

  public static Map<String, String> getAllParams() {
    return ALL_PARAMS.entrySet().stream()
        .collect(Collectors.toMap(
            keyMapper -> "%" + keyMapper.getKey(),
            valueMapper -> valueMapper.getValue().getT2()
        ));
  }
}
