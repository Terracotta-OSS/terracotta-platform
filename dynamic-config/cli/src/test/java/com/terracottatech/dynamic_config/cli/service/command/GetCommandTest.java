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

public class GetCommandTest {
  private final GetCommand command = new GetCommand();

  /*<--Single node validation tests*/
  @Test
  public void testNodeScopeValidation_invalidScopeIdentifier() {
    command.setConfigs(singletonList("blah.1.node.1:offheap-resources.main"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_invalidScopeIdentifier2() {
    command.setConfigs(singletonList("stripe.1.blah.1:offheap-resources.main"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_invalidStripeId() {
    command.setConfigs(singletonList("stripe.0.node.1:offheap-resources.main"));
    assertValidationFailureWithMessage("Expected stripe id to be greater than 0");
  }

  @Test
  public void testNodeScopeValidation_invalidNodeId() {
    command.setConfigs(singletonList("stripe.1.node.0:offheap-resources.main"));
    assertValidationFailureWithMessage("Expected node id to be greater than 0");
  }

  @Test
  public void testNodeScopeValidation_invalidScope() {
    command.setConfigs(singletonList("stripe.1.node.1.blah.1:offheap-resources.main"));
    assertValidationFailureWithMessage("Expected scope to be either in");
  }

  @Test
  public void testNodeScopeValidation_invalidScopeSeparator() {
    command.setConfigs(singletonList("stripe-1.node-1:offheap-resources.main"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testNodeScopeValidation_invalidNumberOfScopeSeparators() {
    command.setConfigs(singletonList("stripe-1.node-1:offheap-resources:main"));
    assertValidationFailureWithMessage("Expected 0 or 1 scope-resolution colon characters");
  }

  @Test
  public void testNodeScopeValidation_invalidProperty() {
    command.setConfigs(singletonList("stripe.1.node.1:blah.main"));
    assertValidationFailureWithMessage("Unknown property: blah");
  }

  @Test
  public void testNodeScopeValidation_invalidPropertyNames() {
    command.setConfigs(singletonList("stripe.1.node.1:data-dirs.main.foo"));
    assertValidationFailureWithMessage("Expected property in the format");
  }


  /*<--Stripe validation tests*/
  @Test
  public void testStripeScopeValidation_invalidScopeIdentifier() {
    command.setConfigs(singletonList("blah.1:offheap-resources.main"));
    assertValidationFailureWithMessage("Config property is of the format");
  }

  @Test
  public void testStripeScopeValidation_invalidStripeId() {
    command.setConfigs(singletonList("stripe.0:offheap-resources.main"));
    assertValidationFailureWithMessage("Expected stripe id to be greater than 0");
  }

  @Test
  public void testStripeScopeValidation_invalidScopeSeparator() {
    command.setConfigs(singletonList("stripe-1:offheap-resources.main"));
    assertValidationFailureWithMessage("Expected scope to be either");
  }

  @Test
  public void testStripeScopeValidation_invalidNumberOfScopeResolutionOperators() {
    command.setConfigs(singletonList("stripe-1:offheap-resources:main"));
    assertValidationFailureWithMessage("Expected 0 or 1 scope-resolution colon characters");
  }

  @Test
  public void testStripeScopeValidation_invalidProperty() {
    command.setConfigs(singletonList("stripe.1:blah.main"));
    assertValidationFailureWithMessage("Unknown property: blah");
  }


  /*<--Cluster validation tests*/
  @Test
  public void testClusterScopeValidation_invalidNumberOfScopeResolutionOperators() {
    command.setConfigs(singletonList("offheap-resources:main"));
    assertValidationFailureWithMessage("Expected scope to be either in");
  }

  @Test
  public void testClusterScopeValidation_invalidProperty() {
    command.setConfigs(singletonList("blah.main"));
    assertValidationFailureWithMessage("Unknown property: blah");
  }

  private void assertValidationFailureWithMessage(String message) {
    assertThat(
        command::validate,
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(containsString(message)))
    );
  }
}