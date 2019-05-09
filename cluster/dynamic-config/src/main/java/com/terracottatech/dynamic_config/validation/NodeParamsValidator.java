/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.dynamic_config.config.AcceptableSettingUnits;
import com.terracottatech.dynamic_config.config.AcceptableSettingValues;
import com.terracottatech.utilities.MemoryUnit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.Constants.MULTI_VALUE_SEP;
import static com.terracottatech.dynamic_config.Constants.PARAM_INTERNAL_SEP;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.util.CommonParamsUtils.splitQuantityUnit;
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
    String param = FAILOVER_PRIORITY;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    final String[] split = value.split(PARAM_INTERNAL_SEP);
    Set<String> acceptableValues = AcceptableSettingValues.get(FAILOVER_PRIORITY);
    if (split.length > 2) {
      throw new IllegalArgumentException(param + " should be one of: " + acceptableValues);
    }

    if (!acceptableValues.contains(split[0])) {
      throw new IllegalArgumentException(param + " should be one of: " + acceptableValues);
    }

    if (split.length == 2) {
      if (split[0].equals("availability")) {
        throw new IllegalArgumentException(param + " should be either 'availability' or 'consistency:N', where is the voter count");
      } else {
        final String voterString = split[1];
        try {
          Integer.parseInt(voterString);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(param + " should be either 'availability' or 'consistency:N', where is the voter count");
        }
      }
    }
  }

  private static void validateSecurity(Map<String, String> paramValueMap) {
    validateSecurityDir(paramValueMap);
    validateAuditDir(paramValueMap);
    validateSecurityAuthc(paramValueMap);
  }

  private static void validateAuditDir(Map<String, String> paramValueMap) {
    if (paramValueMap.get(SECURITY_AUDIT_LOG_DIR) != null &&
        paramValueMap.get(SECURITY_SSL_TLS) == null &&
        paramValueMap.get(SECURITY_AUTHC) == null &&
        paramValueMap.get(SECURITY_WHITELIST) == null) {
      throw new IllegalArgumentException("One of " + SECURITY_SSL_TLS + ", " + SECURITY_AUTHC + ", or " + SECURITY_WHITELIST + " is required for security auditing configuration");
    }
  }

  private static void validateSecurityDir(Map<String, String> paramValueMap) {
    if ((paramValueMap.get(SECURITY_AUTHC) != null && paramValueMap.get(SECURITY_DIR) == null) ||
        (paramValueMap.get(SECURITY_AUDIT_LOG_DIR) != null && paramValueMap.get(SECURITY_DIR) == null) ||
        (paramValueMap.get(SECURITY_SSL_TLS) != null && paramValueMap.get(SECURITY_DIR) == null) ||
        (paramValueMap.get(SECURITY_WHITELIST) != null && paramValueMap.get(SECURITY_DIR) == null)) {
      throw new IllegalArgumentException(SECURITY_DIR + " is mandatory for any of the security-related configuration");
    }
  }

  private static void validateSecurityAuthc(Map<String, String> paramValueMap) {
    String param = SECURITY_AUTHC;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    Set<String> acceptableValues = AcceptableSettingValues.get(SECURITY_AUTHC);
    if (!acceptableValues.contains(value)) {
      throw new IllegalArgumentException(param + " should be one of: " + acceptableValues);
    }
  }

  private static void validateClientSettings(Map<String, String> paramValueMap) {
    validateClientSetting(paramValueMap, CLIENT_LEASE_DURATION);
    validateClientSetting(paramValueMap, CLIENT_RECONNECT_WINDOW);
  }

  private static void validateClientSetting(Map<String, String> paramValueMap, String setting) {
    String value = paramValueMap.get(setting);
    if (value == null) {
      return;
    }

    String[] quantityUnit = splitQuantityUnit(value);
    if (quantityUnit.length != 2) {
      throw new IllegalArgumentException(setting + " should be specified in <quantity><unit> format");
    }

    String quantity = quantityUnit[0];
    try {
      Long.parseLong(quantity);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("<quantity> specified in " + setting + "=<quantity><unit> must be a long digit");
    }

    String unit = quantityUnit[1];
    Set<String> acceptableUnits = AcceptableSettingUnits.get(setting);
    if (!acceptableUnits.contains(unit)) {
      throw new IllegalArgumentException("<unit> specified in " + setting + "=<quantity><unit> must be one of: " + acceptableUnits);
    }
  }

  private static void validateBindAddresses(Map<String, String> paramValueMap) {
    validateBindAddress(paramValueMap, NODE_BIND_ADDRESS);
    validateBindAddress(paramValueMap, NODE_GROUP_BIND_ADDRESS);
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
    String param = NODE_HOSTNAME;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    if (!isValidIPv4(value) && !isValidIPv6(value) && !isValidHost(value)) {
      throw new IllegalArgumentException("<address> specified in " + param + "=<address> must be a valid hostname or IP address");
    }
  }

  private static void validatePorts(Map<String, String> paramValueMap) {
    validatePort(paramValueMap, NODE_PORT);
    validatePort(paramValueMap, NODE_GROUP_PORT);
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
    final String param = OFFHEAP_RESOURCES;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    final String[] offheapResources = value.split(MULTI_VALUE_SEP);
    for (String offheapResource : offheapResources) {
      final String[] nameQuantity = offheapResource.split(PARAM_INTERNAL_SEP);
      if (nameQuantity.length != 2) {
        throw new IllegalArgumentException(param + " should be specified in <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>... format");
      }

      String[] quantityUnit = splitQuantityUnit(nameQuantity[1]);
      if (quantityUnit.length != 2) {
        throw new IllegalArgumentException(param + " should be specified in <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>... format");
      }

      String quantity = quantityUnit[0];
      try {
        Long.parseLong(quantity);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("<quantity> specified in " + param + "=<resource-name>:<quantity><unit> must be a long digit");
      }

      String unit = quantityUnit[1];
      List<String> memoryUnits = Arrays.stream(MemoryUnit.values()).map(memoryUnit -> memoryUnit.name()).collect(Collectors.toList());
      if (!memoryUnits.contains(unit)) {
        throw new IllegalArgumentException("<unit> specified in " + param + "=<resource-name>:<quantity><unit> must be one of: " + memoryUnits);
      }
    }
  }
}
