/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

public class NodeParamsValidatorTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = IParameterSubstitutor.identity();

  @Test
  public void testBadOffheap_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah");
    testThrowsWithMessage(paramValueMap, "should be specified in <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>... format");
  }

  @Test
  public void testBadOffheap_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah:foo");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'foo'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadOffheap_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah:200blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: '200blah'. <unit> must be one of [B, KB, MB, GB, TB, PB].");
  }

  @Test
  public void testBadOffheap_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.OFFHEAP_RESOURCES, "blah:200MB;blah-2:200MB");
    testThrowsWithMessage(paramValueMap, "should be specified in <resource-name>:<quantity><unit>,<resource-name>:<quantity><unit>... format");
  }

  @Test
  public void testBadNodePort_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_PORT, "blah");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodePort_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_PORT, "0");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodePort_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_PORT, "100000");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodeGroupPort_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_PORT, "blah");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodeGroupPort_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_PORT, "0");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadNodeGroupPort_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_PORT, "100000");
    testThrowsWithMessage(paramValueMap, "must be an integer between 1 and 65535");
  }

  @Test
  public void testBadHostname_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_HOSTNAME, "$$$$$$$$$$$");
    testThrowsWithMessage(paramValueMap, "must be a valid hostname or IP address");
  }

  @Test
  public void testBadHostname_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_HOSTNAME, "10..10..10..10");
    testThrowsWithMessage(paramValueMap, "must be a valid hostname or IP address");
  }

  @Test
  public void testBadHostname_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_HOSTNAME, "10:10::10:zz");
    testThrowsWithMessage(paramValueMap, "must be a valid hostname or IP address");
  }

  @Test
  public void testBadNodeBindAddresses_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_BIND_ADDRESS, "10:10::10:zz");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadNodeBindAddresses_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_BIND_ADDRESS, "localhost");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadNodeGroupBindAddresses_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_BIND_ADDRESS, "10:10::10:zz");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadNodeGroupBindAddresses_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.NODE_GROUP_BIND_ADDRESS, "localhost");
    testThrowsWithMessage(paramValueMap, "must be a valid IP address");
  }

  @Test
  public void testBadSecurity_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_AUTHC, "blah");
    paramValueMap.put(Setting.SECURITY_DIR, "security-root-dir");
    testThrowsWithMessage(paramValueMap, "should be one of: " + Setting.SECURITY_AUTHC.getAllowedValues());
  }

  @Test
  public void testBadSecurity_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "true");
    testThrowsWithMessage(paramValueMap, Setting.SECURITY_DIR + " is mandatory");
  }

  @Test
  public void testBadSecurity_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_DIR, "security-root-dir");
    testThrowsWithMessage(paramValueMap, "One of " + Setting.SECURITY_SSL_TLS + ", " + Setting.SECURITY_AUTHC + ", or " + Setting.SECURITY_WHITELIST + " is required for security configuration");
  }

  @Test
  public void testBadSecurity_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_DIR, "security-root-dir");
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "false");
    testThrowsWithMessage(paramValueMap, "One of " + Setting.SECURITY_SSL_TLS + ", " + Setting.SECURITY_AUTHC + ", or " + Setting.SECURITY_WHITELIST + " is required for security configuration");
  }

  @Test
  public void testBadSecurity_6() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_AUTHC, "certificate");
    paramValueMap.put(Setting.SECURITY_DIR, "security-root-dir");

    testThrowsWithMessage(paramValueMap, Setting.SECURITY_SSL_TLS + " is required");
  }

  @Test
  public void testBadSecurity_8() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_AUTHC, "certificate");
    paramValueMap.put(Setting.SECURITY_DIR, "security-root-dir");
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "false");

    testThrowsWithMessage(paramValueMap, Setting.SECURITY_SSL_TLS + " is required");
  }

  @Test
  public void testBadSecurity_9() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_WHITELIST, "blah");

    testThrowsWithMessage(paramValueMap, Setting.SECURITY_WHITELIST + " should be one of");
  }

  @Test
  public void testBadSecurity_10() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "blah");

    testThrowsWithMessage(paramValueMap, Setting.SECURITY_SSL_TLS + " should be one of");
  }

  @Test
  public void testGoodSecurity_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "false");
    paramValueMap.put(Setting.SECURITY_WHITELIST, "true");
    paramValueMap.put(Setting.SECURITY_DIR, "security-dir");
    paramValueMap.put(Setting.SECURITY_AUDIT_LOG_DIR, "security-audit-dir");
    new NodeParamsValidator(paramValueMap, PARAMETER_SUBSTITUTOR).validate();
  }

  @Test
  public void testGoodSecurity_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_WHITELIST, "true");
    paramValueMap.put(Setting.SECURITY_DIR, "security-dir");
    paramValueMap.put(Setting.SECURITY_AUDIT_LOG_DIR, "security-audit-dir");
    new NodeParamsValidator(paramValueMap, PARAMETER_SUBSTITUTOR).validate();
  }

  @Test
  public void testGoodSecurity_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    new NodeParamsValidator(paramValueMap, PARAMETER_SUBSTITUTOR).validate();
  }

  @Test
  public void testGoodSecurity_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "true");
    paramValueMap.put(Setting.SECURITY_AUTHC, "certificate");
    paramValueMap.put(Setting.SECURITY_DIR, "security-root-dir");
    new NodeParamsValidator(paramValueMap, PARAMETER_SUBSTITUTOR).validate();
  }

  @Test
  public void testGoodSecurity_5() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "true");
    paramValueMap.put(Setting.SECURITY_AUTHC, "certificate");
    paramValueMap.put(Setting.SECURITY_WHITELIST, "true");
    paramValueMap.put(Setting.SECURITY_DIR, "security-root-dir");
    paramValueMap.put(Setting.SECURITY_AUDIT_LOG_DIR, "security-audit-dir");
    new NodeParamsValidator(paramValueMap, PARAMETER_SUBSTITUTOR).validate();
  }

  @Test
  public void testGoodSecurity_6() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.SECURITY_SSL_TLS, "false");
    new NodeParamsValidator(paramValueMap, PARAMETER_SUBSTITUTOR).validate();
  }

  @Test
  public void testBadFailoverSettings_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "blah");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadFailoverSettings_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "availability:3");
    testThrowsWithMessage(paramValueMap, "should be either 'availability', 'consistency', or 'consistency:N'");
  }

  @Test
  public void testBadFailoverSettings_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "availability:blah");
    testThrowsWithMessage(paramValueMap, "should be either 'availability', 'consistency', or 'consistency:N'");
  }

  @Test
  public void testBadFailoverSettings_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "consistency:blah");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadFailoverSettings_5() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "consistency;4");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadFailoverSettings_6() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.FAILOVER_PRIORITY, "consistency:0");
    testThrowsWithMessage(paramValueMap, "failover-priority should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }

  @Test
  public void testBadClientReconnectWindow_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'blah'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientReconnectWindow_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "20");
    testThrowsWithMessage(paramValueMap, "should be specified in <quantity><unit> format");
  }

  @Test
  public void testBadClientReconnectWindow_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "MB");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'MB'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientReconnectWindow_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_RECONNECT_WINDOW, "100blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: '100blah'. <unit> must be one of [s, m, h].");
  }

  @Test
  public void testBadClientLeaseDuration_1() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'blah'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientLeaseDuration_2() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "20");
    testThrowsWithMessage(paramValueMap, "should be specified in <quantity><unit> format");
  }

  @Test
  public void testBadClientLeaseDuration_3() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "MB");
    testThrowsWithMessage(paramValueMap, "Invalid measure: 'MB'. <quantity> is missing. Measure should be specified in <quantity><unit> format.");
  }

  @Test
  public void testBadClientLeaseDuration_4() {
    Map<Setting, String> paramValueMap = new HashMap<>();
    paramValueMap.put(Setting.CLIENT_LEASE_DURATION, "100blah");
    testThrowsWithMessage(paramValueMap, "Invalid measure: '100blah'. <unit> must be one of [ms, s, m, h].");
  }

  private void testThrowsWithMessage(Map<Setting, String> paramValueMap, String message) {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage(message);
    new NodeParamsValidator(paramValueMap, PARAMETER_SUBSTITUTOR).validate();
  }
}