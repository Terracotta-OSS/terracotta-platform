/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import org.junit.Test;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class SetCommandTest {
  private final SetCommand command = new SetCommand();

  /*<--Single node validation tests*/
  @Test
  public void testNodeScopeValidation_invalidScopeIdentifier() {
    command.setConfigs(singletonList("blah.1.node.1:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_invalidScopeIdentifier2() {
    command.setConfigs(singletonList("stripe.1.blah.1:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_invalidStripeId() {
    command.setConfigs(singletonList("stripe.0.node.1:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Expected stripe id to be greater than 0");
  }

  @Test
  public void testNodeScopeValidation_invalidNodeId() {
    command.setConfigs(singletonList("stripe.1.node.0:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Expected node id to be greater than 0");
  }

  @Test
  public void testNodeScopeValidation_invalidScope() {
    command.setConfigs(singletonList("stripe.1.node.1.blah.1:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Expected scope to be either in");
  }

  @Test
  public void testNodeScopeValidation_invalidScopeSeparator() {
    command.setConfigs(singletonList("stripe-1.node-1:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_invalidNumberOfScopeSeparators() {
    command.setConfigs(singletonList("stripe-1.node-1:offheap-resources:main=512MB"));
    assertValidationFailureWithMessage("Expected 0 or 1 scope-resolution colon characters");
  }

  @Test
  public void testNodeScopeValidation_invalidProperty() {
    command.setConfigs(singletonList("stripe.1.node.1:blah.main=512MB"));
    assertValidationFailureWithMessage("Unknown property: blah");
  }

  @Test
  public void testNodeScopeValidation_invalidPropertyNames() {
    command.setConfigs(singletonList("stripe.1.node.1:data-dirs.main.foo=512MB"));
    assertValidationFailureWithMessage("Expected property in the format");
  }

  @Test
  public void testNodeScopeValidation_emptyValue() {
    command.setConfigs(singletonList("stripe.1.node.1:data-dirs.main.foo="));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_noValue() {
    command.setConfigs(singletonList("stripe.1.node.1:data-dirs.main.foo"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_invalidValueForNodePort() {
    command.setConfigs(singletonList("stripe.1.node.1:node-port=-100"));
    assertValidationFailureWithMessage("must be an integer between 1 and 65535");
  }

  @Test
  public void testNodeScopeValidation_invalidValueForNodeHostname() {
    command.setConfigs(singletonList("stripe.1.node.1:node-hostname=3:3:3"));
    assertValidationFailureWithMessage("must be a valid hostname or IP address");
  }

  @Test
  public void testNodeScopeValidation_invalidValueForSecurityAuthc() {
    command.setConfigs(singletonList("stripe.1.node.1:security-authc=blah"));
    assertValidationFailureWithMessage("security-authc should be one of: [file, ldap, certificate");
  }

  @Test
  public void testNodeScopeValidation_securityAuthcWithoutSecurityDir() {
    command.setConfigs(singletonList("stripe.1.node.1:security-authc=file"));
    assertValidationFailureWithMessage("security-dir is mandatory");
  }

  @Test
  public void testNode_setInvalidValueForFailoverPriority() {
    command.setConfigs(singletonList("stripe.1.node.1:failover-priority=blah"));
    assertValidationFailureWithMessage("failover-priority should be one of: [availability, consistency]");
  }

  @Test
  public void testNode_setInvalidValueForSecuritySslTls() {
    command.setConfigs(singletonList("stripe.1.node.1:security-ssl-tls=blah"));
    assertValidationFailureWithMessage("security-ssl-tls should be one of: [true, false]");
  }

  @Test
  public void testNode_setInvalidValueForSecurityWhitelist() {
    command.setConfigs(singletonList("stripe.1.node.1:security-whitelist=blah"));
    assertValidationFailureWithMessage("security-whitelist should be one of: [true, false]");
  }

  @Test
  public void testNode_setInvalidValueForOffheap() {
    command.setConfigs(singletonList("stripe.1.node.1:offheap-resources.main=blah"));
    assertValidationFailureWithMessage("Invalid measure: 'blah'");
  }

  @Test
  public void testNode_setInvalidValueForClientLeaseDuration() {
    command.setConfigs(singletonList("stripe.1.node.1:client-lease-duration=blah"));
    assertValidationFailureWithMessage("Invalid measure: 'blah'");
  }

  @Test
  public void testNode_setInvalidValueForClientReconnectWindow() {
    command.setConfigs(singletonList("stripe.1.node.1:client-reconnect-window=blah"));
    assertValidationFailureWithMessage("Invalid measure: 'blah'");
  }


  /*<--Stripe validation tests*/
  @Test
  public void testStripeScopeValidation_invalidScopeIdentifier() {
    command.setConfigs(singletonList("blah.1:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testStripeScopeValidation_invalidStripeId() {
    command.setConfigs(singletonList("stripe.0:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Expected stripe id to be greater than 0");
  }

  @Test
  public void testStripeScopeValidation_invalidScopeSeparator() {
    command.setConfigs(singletonList("stripe-1:offheap-resources.main=512MB"));
    assertValidationFailureWithMessage("Expected scope to be either");
  }

  @Test
  public void testStripeScopeValidation_invalidNumberOfScopeResolutionOperators() {
    command.setConfigs(singletonList("stripe-1:offheap-resources:main=512MB"));
    assertValidationFailureWithMessage("Expected 0 or 1 scope-resolution colon characters");
  }

  @Test
  public void testStripeScopeValidation_invalidProperty() {
    command.setConfigs(singletonList("stripe.1:blah.main=512MB"));
    assertValidationFailureWithMessage("Unknown property: blah");
  }


  /*<--Cluster validation tests*/
  @Test
  public void testClusterScopeValidation_invalidNumberOfScopeResolutionOperators() {
    command.setConfigs(singletonList("offheap-resources:main=512MB"));
    assertValidationFailureWithMessage("Expected scope to be either in");
  }

  @Test
  public void testClusterScopeValidation_invalidProperty() {
    command.setConfigs(singletonList("blah.main=512MB"));
    assertValidationFailureWithMessage("Unknown property: blah");
  }

  private void assertValidationFailureWithMessage(String message) {
    assertThat(
        command::validate,
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString(message)))
    );
  }
}