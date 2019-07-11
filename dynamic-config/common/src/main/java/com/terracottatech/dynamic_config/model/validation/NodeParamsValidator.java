/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.DynamicConfigConstants;
import com.terracottatech.dynamic_config.model.config.AcceptableSettingUnits;
import com.terracottatech.dynamic_config.model.config.AcceptableSettingValues;
import com.terracottatech.dynamic_config.model.config.CommonOptions;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;

import java.util.Map;
import java.util.Set;

import static com.terracottatech.utilities.HostAndIpValidator.isValidHost;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv4;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv6;

public class NodeParamsValidator {
  public static void validate(Map<String, String> paramValueMap) {
    validateOffheap(paramValueMap);
    validatePorts(paramValueMap);
    validateNodeHostname(paramValueMap);
    validateBindAddresses(paramValueMap);
    validateFailoverPriority(paramValueMap);
    validateSecurity(paramValueMap);
    validateClientSettings(paramValueMap);
  }

  private static void validateFailoverPriority(Map<String, String> paramValueMap) {
    String param = CommonOptions.FAILOVER_PRIORITY;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    final String[] split = value.split(DynamicConfigConstants.PARAM_INTERNAL_SEP);
    Set<String> acceptableValues = AcceptableSettingValues.get(CommonOptions.FAILOVER_PRIORITY);
    if (split.length > 2) {
      throw new IllegalArgumentException(param + " should be one of: " + acceptableValues);
    }

    if (!acceptableValues.contains(split[0])) {
      throw new IllegalArgumentException(param + " should be one of: " + acceptableValues);
    }

    if (split.length == 2) {
      boolean throwException = false;
      if (split[0].equals("availability")) {
        throwException = true;
      } else {
        String voterString = split[1];
        try {
          int voterCount = Integer.parseInt(voterString);
          if (voterCount <= 0) {
            throwException = true;
          }
        } catch (NumberFormatException e) {
          throwException = true;
        }
      }

      if (throwException) {
        throw new IllegalArgumentException(param + " should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
      }
    }
  }

  private static void validateSecurity(Map<String, String> paramValueMap) {
    validateBooleanSetting(paramValueMap, CommonOptions.SECURITY_SSL_TLS);
    validateBooleanSetting(paramValueMap, CommonOptions.SECURITY_WHITELIST);
    validateSecurityAuthc(paramValueMap);
    validateSecurityDir(paramValueMap);
  }

  private static void validateBooleanSetting(Map<String, String> paramValueMap, String param) {
    String setting = paramValueMap.get(param);
    Set<String> acceptableValues = AcceptableSettingValues.get(param);
    if (setting != null && !acceptableValues.contains(setting)) {
      throw new IllegalArgumentException(param + " should be one of: " + acceptableValues);
    }
  }

  private static void validateSecurityDir(Map<String, String> paramValueMap) {
    String authc = paramValueMap.get(CommonOptions.SECURITY_AUTHC);
    String securityDir = paramValueMap.get(CommonOptions.SECURITY_DIR);
    String audirLogDir = paramValueMap.get(CommonOptions.SECURITY_AUDIT_LOG_DIR);
    String sslTls = paramValueMap.get(CommonOptions.SECURITY_SSL_TLS);
    String whitelist = paramValueMap.get(CommonOptions.SECURITY_WHITELIST);

    if ((authc != null && securityDir == null) || (audirLogDir != null && securityDir == null) ||
        (Boolean.parseBoolean(sslTls) && securityDir == null) || (Boolean.parseBoolean(whitelist) && securityDir == null)) {
      throw new IllegalArgumentException(CommonOptions.SECURITY_DIR + " is mandatory for any of the security configuration");
    }

    if (securityDir != null && !Boolean.parseBoolean(sslTls) && authc == null && !Boolean.parseBoolean(whitelist)) {
      throw new IllegalArgumentException("One of " + CommonOptions.SECURITY_SSL_TLS + ", " + CommonOptions.SECURITY_AUTHC + ", or " + CommonOptions.SECURITY_WHITELIST + " is required for security configuration");
    }
  }

  private static void validateSecurityAuthc(Map<String, String> paramValueMap) {
    String param = CommonOptions.SECURITY_AUTHC;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    Set<String> acceptableValues = AcceptableSettingValues.get(CommonOptions.SECURITY_AUTHC);
    if (!acceptableValues.contains(value)) {
      throw new IllegalArgumentException(param + " should be one of: " + acceptableValues);
    }

    String ssl = paramValueMap.get(CommonOptions.SECURITY_SSL_TLS);
    if (value.equals("certificate") && !Boolean.parseBoolean(ssl)) {
      throw new IllegalArgumentException(CommonOptions.SECURITY_SSL_TLS + " is required for " + CommonOptions.SECURITY_AUTHC + "=certificate");
    }
  }

  private static void validateClientSettings(Map<String, String> paramValueMap) {
    validateClientSetting(paramValueMap, CommonOptions.CLIENT_LEASE_DURATION);
    validateClientSetting(paramValueMap, CommonOptions.CLIENT_RECONNECT_WINDOW);
  }

  private static void validateClientSetting(Map<String, String> paramValueMap, String setting) {
    String value = paramValueMap.get(setting);
    if (value == null) {
      return;
    }

    Set<TimeUnit> acceptableUnits = AcceptableSettingUnits.get(setting);
    Measure.parse(value, TimeUnit.class, null, acceptableUnits);
  }

  private static void validateBindAddresses(Map<String, String> paramValueMap) {
    validateBindAddress(paramValueMap, CommonOptions.NODE_BIND_ADDRESS);
    validateBindAddress(paramValueMap, CommonOptions.NODE_GROUP_BIND_ADDRESS);
  }

  private static void validateBindAddress(Map<String, String> paramValueMap, String setting) {
    String value = paramValueMap.get(setting);
    if (value == null) {
      return;
    }

    if (!isValidIPv4(value) && !isValidIPv6(value)) {
      throw new IllegalArgumentException("<address> specified in " + setting + "=<address> must be a valid IP address");
    }
  }

  private static void validateNodeHostname(Map<String, String> paramValueMap) {
    String param = CommonOptions.NODE_HOSTNAME;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    if (!isValidIPv4(value) && !isValidIPv6(value) && !isValidHost(value)) {
      throw new IllegalArgumentException("<address> specified in " + param + "=<address> must be a valid hostname or IP address");
    }
  }

  private static void validatePorts(Map<String, String> paramValueMap) {
    validatePort(paramValueMap, CommonOptions.NODE_PORT);
    validatePort(paramValueMap, CommonOptions.NODE_GROUP_PORT);
  }

  private static void validatePort(Map<String, String> paramValueMap, String setting) {
    String value = paramValueMap.get(setting);
    if (value == null) {
      return;
    }

    int port;
    try {
      port = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535");
    }

    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("<port> specified in " + setting + "=<port> must be an integer between 1 and 65535");
    }
  }

  private static void validateOffheap(Map<String, String> paramValueMap) {
    final String param = CommonOptions.OFFHEAP_RESOURCES;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    final String[] offheapResources = value.split(DynamicConfigConstants.MULTI_VALUE_SEP);
    for (String offheapResource : offheapResources) {
      final String[] nameQuantity = offheapResource.split(DynamicConfigConstants.PARAM_INTERNAL_SEP);
      if (nameQuantity.length != 2) {
        throw new IllegalArgumentException(param + " should be specified in <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>... format");
      }
      Measure.parse(nameQuantity[1], MemoryUnit.class);
    }
  }
}
