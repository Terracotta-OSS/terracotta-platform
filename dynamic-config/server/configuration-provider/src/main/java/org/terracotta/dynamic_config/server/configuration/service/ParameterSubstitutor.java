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
package org.terracotta.dynamic_config.server.configuration.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Copy from platform ParameterSubstitutor
 */
public class ParameterSubstitutor implements IParameterSubstitutor {

  private static String uniqueTempDirectory = null;

  @Override
  public String substitute(String source) {
    if (source == null) return null;

    StringBuilder out = new StringBuilder();
    char[] sourceChars = source.toCharArray();

    for (int i = 0; i < sourceChars.length; ++i) {
      if (sourceChars[i] == '%') {
        char nextChar = sourceChars[++i];
        String value = "" + nextChar;

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

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  private static synchronized String getUniqueTempDirectory() {
    if (uniqueTempDirectory == null) {
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
