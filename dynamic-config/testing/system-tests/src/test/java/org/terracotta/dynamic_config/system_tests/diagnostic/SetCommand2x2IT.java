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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class SetCommand2x2IT extends DynamicConfigIT {

  private String connection;

  @Before
  public void before() {
    connection = "localhost:" + getNodePort();
    assertThat(configTool("attach", "-to-stripe", "localhost:" + getNodePort(), "-node", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configTool("attach", "-to-stripe", "localhost:" + getNodePort(2,1), "-node", "localhost:" + getNodePort(2, 2)), is(successful()));
    assertThat(configTool("attach", "-to-cluster", "localhost:" + getNodePort(1,1), "-stripe", "localhost:" + getNodePort(2, 1)), is(successful()));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripe.1.node.1.name=node1",
      "-setting", "stripe.1.node.2.name=node2",
      "-setting", "stripe.2.node.1.name=node3",
      "-setting", "stripe.2.node.2.name=node4",
      "-setting", "stripe.1.stripe-name=stripeA",
      "-setting", "stripe.2.stripe-name=stripeB"
      ), is(successful()));

    assertThat(configTool("get", "-connect-to", connection,
      "-setting", "name",
      "-setting", "stripe-name"
      ), allOf(containsOutput("node:node1:name=node1"),
        containsOutput("node:node2:name=node2"),
        containsOutput("node:node3:name=node3"),
        containsOutput("node:node4:name=node4"),
        containsOutput("stripe:stripeA:stripe-name=stripeA"),
        containsOutput("stripe:stripeB:stripe-name=stripeB")));

    assertThat(configTool("get", "-connect-to", connection, "-outputformat", "index",
      "-setting", "name",
      "-setting", "stripe-name"
      ), allOf(containsOutput("stripe.1.node.1.name=node1"),
        containsOutput("stripe.1.node.2.name=node2"),
        containsOutput("stripe.2.node.1.name=node3"),
        containsOutput("stripe.2.node.2.name=node4"),
        containsOutput("stripe.1.stripe-name=stripeA"),
        containsOutput("stripe.2.stripe-name=stripeB")));
  }

  @Test
  public void test_get_set_scope_overrides() {

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "backup-dir=X"
      ), is(successful()));

    assertThat(configTool("get", "-connect-to", connection,
      "-setting", "backup-dir"
      ), allOf(containsOutput("node:node1:backup-dir=X"),
        containsOutput("node:node2:backup-dir=X"),
        containsOutput("node:node3:backup-dir=X"),
        containsOutput("node:node4:backup-dir=X")));

    assertThat(configTool("get", "-connect-to", connection, "-outputformat", "index",
      "-setting", "backup-dir"
      ), allOf(containsOutput("stripe.1.node.1.backup-dir=X"),
        containsOutput("stripe.1.node.2.backup-dir=X"),
        containsOutput("stripe.2.node.1.backup-dir=X"),
        containsOutput("stripe.2.node.2.backup-dir=X")));

    // Node overrides stripe and stripe overrides cluster-level settings

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeB:backup-dir=YY",
      "-setting", "backup-dir=XX",
      "-setting", "node2:backup-dir=ZZ",
      "-setting", "node4:backup-dir=ZZZ"
      ), is(successful()));

    assertThat(configTool("get", "-connect-to", connection,
      "-setting", "backup-dir"
      ), allOf(containsOutput("node:node1:backup-dir=XX"),
        containsOutput("node:node2:backup-dir=ZZ"),
        containsOutput("node:node3:backup-dir=YY"),
        containsOutput("node:node4:backup-dir=ZZZ")));

    assertThat(configTool("get", "-connect-to", connection, "-outputformat", "index",
      "-setting", "backup-dir"
      ), allOf(containsOutput("stripe.1.node.1.backup-dir=XX"),
        containsOutput("stripe.1.node.2.backup-dir=ZZ"),
        containsOutput("stripe.2.node.1.backup-dir=YY"),
        containsOutput("stripe.2.node.2.backup-dir=ZZZ")));

    // Stripe overrides cluster

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "backup-dir=Z",
      "-setting", "stripeA:backup-dir=YY",
      "-setting", "stripeB:backup-dir=YY"
      ), is(successful()));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "backup-dir=ZZZZZ",
      "-setting", "stripeA:backup-dir=YY",
      "-setting", "stripeB:backup-dir=YY"
      ), containsOutput("The requested update will not result in any change to the cluster configuration"));
  }

  @Test
  public void test_get_set_settings() {

    // Other settings

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeB:tc-properties.a=A",
      "-setting", "tc-properties.a.b=AB",
      "-setting", "node2:tc-properties.a.b=AABB",
      "-setting", "node4:tc-properties.a.b.c=ABC"
      ), is(successful()));

    assertThat(configTool("get", "-connect-to", connection,
      "-setting", "tc-properties"
      ), allOf(containsOutput("node:node1:tc-properties=a.b:AB"),
        containsOutput("node:node2:tc-properties=a.b:AABB"),
        containsOutput("node:node3:tc-properties=a:A,a.b:AB"),
        containsOutput("node:node4:tc-properties=a:A,a.b:AB,a.b.c:ABC")));

    assertThat(configTool("get", "-connect-to", connection, "-outputformat", "index",
      "-setting", "tc-properties"
      ), allOf(containsOutput("stripe.1.node.1.tc-properties=a.b:AB"),
        containsOutput("stripe.1.node.2.tc-properties=a.b:AABB"),
        containsOutput("stripe.2.node.1.tc-properties=a:A,a.b:AB"),
        containsOutput("stripe.2.node.2.tc-properties=a:A,a.b:AB,a.b.c:ABC")));
 }

  @Test
  public void test_get_set_more_settings() {

    // Other settings
    assertThat(configTool("get", "-connect-to", connection,
        "-setting", "client-reconnect-window",
        "-setting", "offheap-resources.main"
    ), allOf(containsOutput("client-reconnect-window=10s"),
        containsOutput("offheap-resources.main=512MB")));

    assertThat(configTool("set", "-connect-to", connection,
        "-setting", "client-reconnect-window=120s",
        "-setting", "offheap-resources.main=512MB"
    ), is(successful()));

    assertThat(configTool("get", "-connect-to", connection,
        "-setting", "client-reconnect-window",
        "-setting", "offheap-resources"
    ), allOf(containsOutput("client-reconnect-window=120s"),
        containsOutput("offheap-resources=foo:1GB,main:512MB")));

    assertThat(configTool("get", "-connect-to", connection, "-outputformat", "index",
        "-setting", "client-reconnect-window",
        "-setting", "offheap-resources"
    ), allOf(containsOutput("client-reconnect-window=120s"),
        containsOutput("offheap-resources=foo:1GB,main:512MB")));
  }

  @Test
  public void test_set_errors() {
    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripe.1.node.2.backup-dir=Z",
      "-setting", "node2:backup-dir=Z"
      ), containsOutput("Error: Duplicate configurations found: stripe.1.node.2.backup-dir=Z and node2:backup-dir=Z"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeeee:stripeA:backup-dir=Y"
      ), containsOutput("Error: Scope 'stripeeee:' specified in property 'stripeeee:stripeA:backup-dir=Y' is invalid. " +
        "Scope must be one of 'stripe:' or 'node:'"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripe:stripeAAA:backup-dir=Y"
      ), containsOutput("Error: Name 'stripeAAA' in setting 'stripe:stripeAAA:backup-dir=Y' is not a recognized stripe."));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeAAA:backup-dir=Y"
      ), containsOutput("Error: Name 'stripeAAA' in setting 'stripeAAA:backup-dir=Y' is not a recognized stripe or node."));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node:node111:backup-dir=Y"
      ), containsOutput("Error: Name 'node111' in setting 'node:node111:backup-dir=Y' is not a recognized node."));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node111:backup-dir=Y"
      ), containsOutput("Error: Name 'node111' in setting 'node111:backup-dir=Y' is not a recognized stripe or node."));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeA:stripe-name=node1"
      ), is(successful()));
    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node1:backup-dir=Y"
      ), containsOutput("Error: Name 'node1' in setting 'node1:backup-dir=Y' is both a stripe name and node name. " +
        "It must be qualified with either 'stripe:' or 'node:'"));
  }
  @Test
  public void test_forbidden_chars_in_names() {

    // Invalid Character sets from ClusterValidator
    Character[] FORBIDDEN_FILE_CHARS = {':', '/', '\\', '<', '>', '"', '|', '*', '?'};
    Character[] FORBIDDEN_DC_CHARS_1 = {' ', ':', '{', '}', ','};
    Character[] FORBIDDEN_DC_CHARS_2 = {'=', '%'};
    Character[] ALLOWED_SPECIAL_CHARACTERS = {'~', '`', '!', '@', '#', '$', '^', '&', '(', ')', '-',  +
        '_', '+', '.', ';', '\'', ']', '['};

    Stream.of(FORBIDDEN_FILE_CHARS).forEach(c ->
      assertThat(configTool("set", "-connect-to", connection,
        "-setting", "node1:name=bad" + c + "name"
        ), allOf(containsOutput("Error: Invalid character in node name"),
          containsOutput("'" + c + "'"))));

    Stream.of(FORBIDDEN_DC_CHARS_1).forEach(c ->
      assertThat(configTool("set", "-connect-to", connection,
        "-setting", "node1:name=bad" + c + "name"
        ), allOf(containsOutput("Error: Invalid character in node name"),
          containsOutput("'" + c + "'"))));

    // These invalid chars are identified and thrown before ClusterValidator.validateName() sees them
    Stream.of(FORBIDDEN_DC_CHARS_2).forEach(c ->
      assertThat(configTool("set", "-connect-to", connection,
        "-setting", "node1:name=bad" + c + "name"
        ), containsOutput("Error: Invalid input")));

    // A few more examples but using 'stripe' and displaying full error messages

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeA:stripe-name=[bad{name}"
      ), containsOutput("Error: Invalid character in stripe name: '{'"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeB:stripe-name=[bad|name"
      ), containsOutput("Error: Invalid character in stripe name: '|'"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node1:name=]bad?name"
      ), containsOutput("Error: Invalid character in node name: '?'"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node1:name=#bad name"
      ), containsOutput("Error: Invalid character in node name: ' '"));

    // Test each of the allowed special characters (i.e. chars that do not appear in the FORBIDDEN lists)
    Stream.of(ALLOWED_SPECIAL_CHARACTERS).forEach(c ->
      assertThat(configTool("set", "-connect-to", connection,
        "-setting", "stripe.1.node.1.name=good" + c + "name"), is(successful())));

    // Test as a single ridiculous node name
    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripe.1.node.1.name=~`!@#$^&()-_+.;']["), is(successful()));
  }

  @Test
  public void test_set_missing_value_errors() {

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeB:backup-dir="
      ), containsOutput("Error: Invalid input: 'stripeB:backup-dir='. Reason: Operation set requires a value"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripe:stripeB:backup-dir="
      ), containsOutput("Error: Invalid input: 'stripe:stripeB:backup-dir='. Reason: Operation set requires a value"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripeB:backup-dir"
      ), containsOutput("Error: Invalid input: 'stripeB:backup-dir'. Reason: Operation set requires a value"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "stripe:stripeB:backup-dir"
      ), containsOutput("Error: Invalid input: 'stripe:stripeB:backup-dir'. Reason: Operation set requires a value"));


    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node2:backup-dir="
      ), containsOutput("Error: Invalid input: 'node2:backup-dir='. Reason: Operation set requires a value"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node:node2:backup-dir="
      ), containsOutput("Error: Invalid input: 'node:node2:backup-dir='. Reason: Operation set requires a value"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node2:backup-dir"
      ), containsOutput("Error: Invalid input: 'node2:backup-dir'. Reason: Operation set requires a value"));

    assertThat(configTool("set", "-connect-to", connection,
      "-setting", "node:node2:backup-dir"
      ), containsOutput("Error: Invalid input: 'node:node2:backup-dir'. Reason: Operation set requires a value"));
  }
}
