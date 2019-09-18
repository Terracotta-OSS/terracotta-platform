/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Validator;

import java.util.HashMap;
import java.util.Map;

import static com.terracottatech.dynamic_config.model.Setting.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.Setting.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.Setting.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.model.Setting.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.Setting.NODE_NAME;
import static com.terracottatech.dynamic_config.model.Setting.NODE_PORT;
import static com.terracottatech.dynamic_config.model.Setting.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_DIR;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_WHITELIST;

public class NodeParamsValidator implements Validator {
  private final Map<Setting, String> paramValueMap;
  private final IParameterSubstitutor parameterSubstitutor;

  public NodeParamsValidator(Map<Setting, String> paramValueMap, IParameterSubstitutor parameterSubstitutor) {
    this.paramValueMap = new HashMap<>(paramValueMap);
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public void validate() throws IllegalArgumentException {
    validateNodeName();
    validateIfPresent(NODE_PORT);
    validateIfPresent(NODE_GROUP_PORT);
    validateIfPresentAndNoPlaceholder(NODE_HOSTNAME);
    validateIfPresentAndNoPlaceholder(NODE_BIND_ADDRESS);
    validateIfPresentAndNoPlaceholder(NODE_GROUP_BIND_ADDRESS);
    validateIfPresentAndNoPlaceholder(OFFHEAP_RESOURCES);
    validateIfPresent(FAILOVER_PRIORITY);
    validateIfPresent(CLIENT_LEASE_DURATION);
    validateIfPresent(CLIENT_RECONNECT_WINDOW);
    // security
    validateIfPresent(SECURITY_SSL_TLS);
    validateIfPresent(SECURITY_WHITELIST);
    validateIfPresent(SECURITY_AUTHC);
    validateSecurityDir();
  }

  private void validateSecurityDir() {
    String authc = paramValueMap.get(SECURITY_AUTHC);
    String securityDir = paramValueMap.get(SECURITY_DIR);
    String audirLogDir = paramValueMap.get(SECURITY_AUDIT_LOG_DIR);
    String sslTls = paramValueMap.get(SECURITY_SSL_TLS);
    String whitelist = paramValueMap.get(SECURITY_WHITELIST);

    if (authc != null) {
      if (authc.equals("certificate") && !Boolean.parseBoolean(sslTls)) {
        throw new IllegalArgumentException(SECURITY_SSL_TLS + " is required for " + SECURITY_AUTHC + "=certificate");
      }
    }

    if ((authc != null && securityDir == null) || (audirLogDir != null && securityDir == null) ||
        (Boolean.parseBoolean(sslTls) && securityDir == null) || (Boolean.parseBoolean(whitelist) && securityDir == null)) {
      throw new IllegalArgumentException(Setting.SECURITY_DIR + " is mandatory for any of the security configuration");
    }

    if (securityDir != null && !Boolean.parseBoolean(sslTls) && authc == null && !Boolean.parseBoolean(whitelist)) {
      throw new IllegalArgumentException("One of " + Setting.SECURITY_SSL_TLS + ", " + Setting.SECURITY_AUTHC + ", or " + Setting.SECURITY_WHITELIST + " is required for security configuration");
    }
  }

  private void validateIfPresentAndNoPlaceholder(Setting setting) {
    String value = paramValueMap.get(setting);
    if (value == null || parameterSubstitutor.containsSubstitutionParams(value)) {
      return;
    }
    setting.validate(value);
  }

  private void validateNodeName() {
    Setting param = NODE_NAME;
    String value = paramValueMap.get(param);
    if (value == null) {
      return;
    }

    if (parameterSubstitutor.containsSubstitutionParams(value)) {
      throw new IllegalArgumentException("<node-name> specified in " + param + "=<name> cannot contain substitution parameters");
    }
  }

  private void validateIfPresent(Setting setting) {
    String value = paramValueMap.get(setting);
    if (value == null) {
      return;
    }
    setting.validate(value);
  }
}
