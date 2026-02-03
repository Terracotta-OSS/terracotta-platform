/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
package org.terracotta.dynamic_config.api.service;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.terracotta.dynamic_config.api.service.ConfigPropertiesTranslator.ERR_NODE_NAMES_PROPERTY_MISSING;
import static org.terracotta.dynamic_config.api.service.ConfigPropertiesTranslator.ERR_NO_NODES_IDENTIFIED;
import static org.terracotta.dynamic_config.api.service.ConfigPropertiesTranslator.ERR_NO_STRIPES_IDENTIFIED;
import static org.terracotta.dynamic_config.api.service.ConfigPropertiesTranslator.ERR_STRIPE_NAMES_PROPERTY_MISSING;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class ConfigPropertiesTranslatorTest {


  @Test
  public void test_2_way_conversions() throws IOException {

    // Stripe and node names separated with no spaces, alphabetically ordered

    Reader r = getReader(new String[]{
      "stripe-names=s1,s2",
      "s1:node-names=n1,n2",
      "s2:node-names=n3,n4",
      "n1:port=1",
      "n2:port=2",
      "n3:port=3",
      "n4:port=4",
    });
    Properties props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.stripe-name"), is("s1"));
    assertThat(props.getProperty("stripe.2.stripe-name"), is("s2"));
    assertThat(props.getProperty("stripe.1.node.1.name"), is("n1"));
    assertThat(props.getProperty("stripe.1.node.2.name"), is("n2"));
    assertThat(props.getProperty("stripe.2.node.1.name"), is("n3"));
    assertThat(props.getProperty("stripe.2.node.2.name"), is("n4"));
    assertThat(props.getProperty("stripe.1.node.1.port"), is("1"));
    assertThat(props.getProperty("stripe.1.node.2.port"), is("2"));
    assertThat(props.getProperty("stripe.2.node.1.port"), is("3"));
    assertThat(props.getProperty("stripe.2.node.2.port"), is("4"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("stripe-names=s1,s2"),
      containsString("stripe:s1:node-names=n1,n2"),
      containsString("stripe:s2:node-names=n3,n4")
    ));

    // Names separated by spaces, non-alphabetical ordering

    r = getReader(new String[]{
      "stripe-names=s1,  s3,s2    ,",
      "s1:node-names=n1,  n2",
      "s2:node-names=n4  ,n5",
      "s3:node-names=n7, n6, n3",
      "n1:port=1",
      "n2:port=2",
      "n3:port=3",
      "n4:port=4",
      "n5:port=5",
      "n6:port=6",
      "n7:port=7",
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.stripe-name"), is("s1"));
    assertThat(props.getProperty("stripe.2.stripe-name"), is("s3"));
    assertThat(props.getProperty("stripe.3.stripe-name"), is("s2"));
    assertThat(props.getProperty("stripe.1.node.1.name"), is("n1"));
    assertThat(props.getProperty("stripe.1.node.2.name"), is("n2"));
    assertThat(props.getProperty("stripe.2.node.1.name"), is("n7"));
    assertThat(props.getProperty("stripe.2.node.2.name"), is("n6"));
    assertThat(props.getProperty("stripe.2.node.3.name"), is("n3"));
    assertThat(props.getProperty("stripe.3.node.1.name"), is("n4"));
    assertThat(props.getProperty("stripe.3.node.2.name"), is("n5"));
    assertThat(props.getProperty("stripe.1.node.1.port"), is("1"));
    assertThat(props.getProperty("stripe.1.node.2.port"), is("2"));
    assertThat(props.getProperty("stripe.2.node.1.port"), is("7"));
    assertThat(props.getProperty("stripe.2.node.2.port"), is("6"));
    assertThat(props.getProperty("stripe.2.node.3.port"), is("3"));
    assertThat(props.getProperty("stripe.3.node.1.port"), is("4"));
    assertThat(props.getProperty("stripe.3.node.2.port"), is("5"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("stripe-names=s1,s3,s2"),
      containsString("stripe:s1:node-names=n1,n2"),
      containsString("stripe:s2:node-names=n4,n5"),
      containsString("stripe:s3:node-names=n7,n6,n3")
    ));

    // cluster-level node settings with node overrides

    r = getReader(new String[]{
        "stripe-names=s1,s2,s3",
        "s1:node-names=n1,n2",
        "s2:node-names=n3,n4",
        "s3:node-names=n5,n6",
        "n1:backup-dir=dir1111",
        "backup-dir=dir1",
        "n4:backup-dir=dir4444",
        "stripe:s2:backup-dir=dir2",
        "s3:backup-dir=dir3",
        "node:n6:backup-dir=dir3333"
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.node.1.backup-dir"), is("dir1111"));
    assertThat(props.getProperty("stripe.1.node.2.backup-dir"), is("dir1"));
    assertThat(props.getProperty("stripe.2.node.1.backup-dir"), is("dir2"));
    assertThat(props.getProperty("stripe.2.node.2.backup-dir"), is("dir4444"));
    assertThat(props.getProperty("stripe.3.node.1.backup-dir"), is("dir3"));
    assertThat(props.getProperty("stripe.3.node.2.backup-dir"), is("dir3333"));
    assertThat(translateToConfigFormat(props), allOf(
        containsString("node:n1:backup-dir=dir1111"),
        containsString("node:n2:backup-dir=dir1"),
        containsString("node:n3:backup-dir=dir2"),
        containsString("node:n4:backup-dir=dir4444"),
        containsString("node:n5:backup-dir=dir3"),
        containsString("node:n6:backup-dir=dir3333")
    ));

    // relay settings
    r = getReader(new String[]{
      "stripe-names=s1",
      "s1:node-names=n1,n2",
      "n1:relay-source-hostname=localhost",
      "n1:relay-source-port=9410",
      "n2:relay-source-hostname",
      "n2:relay-destination-hostname=localhost",
      "n2:relay-destination-port=9410",
      "n2:relay-destination-group-port=9430"
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.node.1.relay-source-hostname"), is("localhost"));
    assertThat(props.getProperty("stripe.1.node.1.relay-source-port"), is("9410"));
    assertThat(props.getProperty("stripe.1.node.2.relay-source-hostname"), is(""));
    assertThat(props.getProperty("stripe.1.node.2.relay-destination-hostname"), is("localhost"));
    assertThat(props.getProperty("stripe.1.node.2.relay-destination-port"), is("9410"));
    assertThat(props.getProperty("stripe.1.node.2.relay-destination-group-port"), is("9430"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("node:n1:relay-source-hostname=localhost"),
      containsString("node:n1:relay-source-port=9410"),
      containsString("node:n2:relay-source-hostname="),
      containsString("node:n2:relay-destination-hostname=localhost"),
      containsString("node:n2:relay-destination-port=9410"),
      containsString("node:n2:relay-destination-group-port=9430")
    ));

    // composite properties

    r = getReader(new String[]{
      "stripe-names=s1",
      "s1:node-names=n1,n2,n3",
      "n1:tc-properties=a.b.c:Z",
      "n1:data-dirs.a=A",
      "n1:data-dirs.b=B",
      "n2:data-dirs.a.b.c=ABC",
      "n3:data-dirs=a:A,b:B,abc:ABC",
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.node.1.tc-properties"), is("a.b.c:Z"));
    assertThat(props.getProperty("stripe.1.node.1.data-dirs.a"), is("A"));
    assertThat(props.getProperty("stripe.1.node.1.data-dirs.b"), is("B"));
    assertThat(props.getProperty("stripe.1.node.2.data-dirs.a.b.c"), is("ABC"));
    assertThat(props.getProperty("stripe.1.node.3.data-dirs"), is("a:A,b:B,abc:ABC"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("node:n1:tc-properties=a.b.c:Z"),
      containsString("node:n1:data-dirs.a=A"),
      containsString("node:n1:data-dirs.b=B"),
      containsString("node:n2:data-dirs.a.b.c=ABC"),
      containsString("node:n3:data-dirs=a:A,b:B,abc:ABC")
    ));

    // unrealistic stripe and node names

    r = getReader(new String[]{
      "stripe-names=stripe, stripe.2, stripe.1.",
      "stripe:node-names=node, node.1, node.2.",
      "stripe.2:node-names=.node.3., .node.2",
      "stripe.1.:node-names=node.4, node.5.",
      "node:port=1",
      "node.1:port=2",
      "node.2.:port=3",
      ".node.3.:port=4",
      ".node.2:port=5",
      "node.4:port=6",
      "node.5.:port=7",
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.node.1.port"), is("1"));
    assertThat(props.getProperty("stripe.1.node.2.port"), is("2"));
    assertThat(props.getProperty("stripe.1.node.3.port"), is("3"));
    assertThat(props.getProperty("stripe.2.node.1.port"), is("4"));
    assertThat(props.getProperty("stripe.2.node.2.port"), is("5"));
    assertThat(props.getProperty("stripe.3.node.1.port"), is("6"));
    assertThat(props.getProperty("stripe.3.node.2.port"), is("7"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("stripe-names=stripe,stripe.2,stripe.1."),
      containsString("stripe:stripe:node-names=node,node.1,node.2."),
      containsString("stripe:stripe.2:node-names=.node.3.,.node.2"),
      containsString("stripe:stripe.1.:node-names=node.4,node.5.")
    ));

    // blank setting values

    r = getReader(new String[]{
        "stripe-names=s1",
        "s1:node-names=n1,n2",
        "n1:public-port",
        "n2:public-port=",
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.node.1.public-port"), is(""));
    assertThat(props.getProperty("stripe.1.node.1.public-port"), is(""));
    assertThat(translateToConfigFormat(props), allOf(
        containsString("node:n1:public-port="),
        containsString("node:n2:public-port=")
    ));

    // ignoring 'stripe-name' and 'name' settings

    r = getReader(new String[]{
        "port=111",
        "stripe-names=s1,s2",
        "s1:node-names=n1,n2",
        "s2:node-names=n3,n4",
        "n1:name=n111",
        "n2:name=n222",
        "n3:name=n333",
        "n4:name=n444",
        "stripe:s1:stripe-name=s111",
        "stripe:s2:stripe-name=s222",
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.stripe-name"), is("s1"));
    assertThat(props.getProperty("stripe.2.stripe-name"), is("s2"));
    assertThat(props.getProperty("stripe.1.node.1.name"), is("n1"));
    assertThat(props.getProperty("stripe.1.node.2.name"), is("n2"));
    assertThat(props.getProperty("stripe.2.node.1.name"), is("n3"));
    assertThat(props.getProperty("stripe.2.node.2.name"), is("n4"));
    assertThat(translateToConfigFormat(props), allOf(
        containsString("stripe-names=s1,s2"),
        containsString("s1:node-names=n1,n2"),
        containsString("s2:node-names=n3,n4")
    ));
  }

  @Test
  public void test_2_way_conversions_mixed_scope() throws IOException {

    Reader r = getReader(new String[]{
      "stripe-names=s1,s2",
      "stripe:s1:node-names=n1,n2",
      "cluster-name=my-cluster",
      "s2:node-names=n3,n4",
      "node:n1:port=1",
      "n2:port=2",
      "node:n3:port=3",
      "n4:port=4",
      "node:n2:tc-properties.a.b.c=ABC",
    });
    Properties props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.stripe-name"), is("s1"));
    assertThat(props.getProperty("stripe.2.stripe-name"), is("s2"));
    assertThat(props.getProperty("stripe.1.node.1.name"), is("n1"));
    assertThat(props.getProperty("stripe.1.node.2.name"), is("n2"));
    assertThat(props.getProperty("stripe.2.node.1.name"), is("n3"));
    assertThat(props.getProperty("stripe.2.node.2.name"), is("n4"));
    assertThat(props.getProperty("stripe.1.node.1.port"), is("1"));
    assertThat(props.getProperty("stripe.1.node.2.port"), is("2"));
    assertThat(props.getProperty("stripe.2.node.1.port"), is("3"));
    assertThat(props.getProperty("stripe.2.node.2.port"), is("4"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("stripe-names=s1,s2"),
      containsString("stripe:s1:node-names=n1,n2"),
      containsString("stripe:s2:node-names=n3,n4"),
      containsString("node:n1:port=1"),
      containsString("node:n2:port=2"),
      containsString("cluster-name=my-cluster")
    ));

    r = getReader(new String[]{
      "stripe-names=s1,s2",
      "stripe:s1:node-names=n1,n2",
      "s2:node-names=n3,n4",
      "failover-priority=consistency:2",
      "backup-dir=same",
      "node:n2:tc-properties.a.b.c=ABC",
      "stripe:s2:stripe-uid=YLQguzhRSdS6y5M9vnA5mw",
      "node:n4:node-uid=5Zv3uphiRLavoGZthy7JNg",
      "n3:node-uid=6Zv3uphiRLavoGZthy7JNg"
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("failover-priority"), is("consistency:2"));
    assertThat(props.getProperty("stripe.2.stripe-uid"), is("YLQguzhRSdS6y5M9vnA5mw"));
    assertThat(props.getProperty("stripe.2.node.2.node-uid"), is("5Zv3uphiRLavoGZthy7JNg"));
    assertThat(props.getProperty("stripe.2.node.1.node-uid"), is("6Zv3uphiRLavoGZthy7JNg"));
    assertThat(props.getProperty("stripe.1.node.2.tc-properties.a.b.c"), is("ABC"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("failover-priority=consistency:2"),
      containsString("node:n2:tc-properties.a.b.c=ABC"),
      containsString("stripe:s2:stripe-uid=YLQguzhRSdS6y5M9vnA5mw"),
      containsString("node:n4:node-uid=5Zv3uphiRLavoGZthy7JNg"),
      containsString("node:n3:node-uid=6Zv3uphiRLavoGZthy7JNg")
    ));

    // same named stripes and nodes

    r = getReader(new String[]{
      "stripe-names=x1,x2",
      "x1:node-names=x1,x2",
      "x2:node-names=n3,n4",
      "port=1111",
      "stripe:x1:port=2222",
      "node:n4:port=4444"
    });
    props = translateToProperties(r);
    assertThat(props.getProperty("stripe.1.node.1.port"), is("2222"));
    assertThat(props.getProperty("stripe.1.node.2.port"), is("2222"));
    assertThat(props.getProperty("stripe.2.node.1.port"), is("1111"));
    assertThat(props.getProperty("stripe.2.node.2.port"), is("4444"));
    assertThat(translateToConfigFormat(props), allOf(
      containsString("node:x1:port=2222"),
      containsString("node:x2:port=2222"),
      containsString("node:n3:port=1111"),
      containsString("node:n4:port=4444")
    ));

    r.close();
  }

  @Test
  public void test_bad_stripe_names_config() throws IOException {

    assertException(new String[]{
      ""
    }, ERR_STRIPE_NAMES_PROPERTY_MISSING);

    assertException(new String[]{
      "stripe-namessss"
    }, ERR_STRIPE_NAMES_PROPERTY_MISSING);

    assertException(new String[]{
      "stripe-names"
    }, ERR_NO_STRIPES_IDENTIFIED);

    assertException(new String[]{
      "stripe-names="
    }, ERR_NO_STRIPES_IDENTIFIED);

    assertException(new String[]{
      "stripe-names=,,,"
    }, ERR_NO_STRIPES_IDENTIFIED);
  }

  @Test
  public void test_bad_node_names_config() throws IOException {

    assertException(new String[]{
      "node-names",
    }, ERR_STRIPE_NAMES_PROPERTY_MISSING);

    assertException(new String[]{
      "stripe-names=s1,s2",
      "node-names",
    }, ERR_NODE_NAMES_PROPERTY_MISSING);

    assertException(new String[]{
      "stripe-names=s1,s2",
      "node-names=",
    }, ERR_NODE_NAMES_PROPERTY_MISSING);

    assertException(new String[]{
      "stripe-names=s1,s2",
      ":node-name=",
    }, ERR_NODE_NAMES_PROPERTY_MISSING);

    assertException(new String[]{
      "stripe-names=s1,s2,s3",
      ":node-names=",
      "s2:node-names=",
      "s4:node-names=a"
    }, "Blank stripe name specified in property ':node-names'",
      "No nodes were specified for property :node-names",
      "No nodes were specified for property s2:node-names",
      "Stripe 's4' referenced in 's4:node-names' not found in 'stripe-names'",
      ERR_NO_NODES_IDENTIFIED);

    assertException(new String[]{
      "stripe-names=s1,s2",
      "s3:node-names=",
    }, "Stripe 's3' referenced in 's3:node-names' not found in 'stripe-names'",
      "No nodes were specified for property s3:node-names",
      ERR_NO_NODES_IDENTIFIED);

    assertException(new String[]{
      "stripe-names=s1,s2",
      "stripeeee:s1:node-names=",
    }, "Invalid scope for 'stripeeee:s1:node-names. Expected 'stripe' but found 'stripeeee'",
      "No nodes were specified for property stripeeee:s1:node-names",
      ERR_NO_NODES_IDENTIFIED);

    assertException(new String[]{
      "stripe-names=s1,s2",
      "stripe:s3:node-names=",
    }, "Stripe 's3' referenced in 'stripe:s3:node-names' not found in 'stripe-names'",
      "No nodes were specified for property stripe:s3:node-names",
      ERR_NO_NODES_IDENTIFIED);

    assertException(new String[]{
      "stripe-names=s1,s2",
      "stuff:more-stuff:stripe:s3:node-names=",
    }, "Invalid syntax for property 'stuff:more-stuff:stripe:s3:node-names'",
      "No nodes were specified for property stuff:more-stuff:stripe:s3:node-names",
      ERR_NO_NODES_IDENTIFIED);

    assertException(new String[]{
      "stripe-names=s1,s2",
      "s3:node-names=",
      "stripe:s3:node-names=",
    }, "Stripe 's3' referenced in 's3:node-names' not found in 'stripe-names'",
      "No nodes were specified for property s3:node-names",
      "Stripe 's3' referenced in 'stripe:s3:node-names' not found in 'stripe-names'",
      "No nodes were specified for property stripe:s3:node-names",
      ERR_NO_NODES_IDENTIFIED
    );
  }

  @Test
  public void test_bad_same_node_stripe_names_config() throws IOException {

    // same stripe and node names
    assertException(new String[]{
      "stripe-names=x1,x2",
      "x1:node-names=x1,x2",
      "x2:node-names=n3,4n",
      "port=1111",
      "x1:port=1",
      "stripe:s1:tc-properties=ABC",
      "node:n1:hostname=hostname",
      "zzzz:hostname=hostname",
      "scope:n3:port=1111",
    }, "Name 'x1' in property 'x1:port' is both a stripe name and node name'. It must be qualified with either 'stripe:' or 'node:'",
      "Stripe 's1' in property 'stripe:s1:tc-properties' is not a recognized stripe",
      "Node 'n1' in property 'node:n1:hostname' is not a recognized node",
      "Name 'zzzz' in property 'zzzz:hostname' is not a recognized stripe or node name",
      "Scope 'scope:' specified in property 'scope:n3:port' is invalid. Scope must be one of 'stripe:' or 'node:'"
    );
  }

  @Test
  public void test_bad_properties_config() throws IOException {

    // bad property names

    assertException(new String[]{
      "stripe-names=s1,s2",
      "s1:node-names=n1,n2",
      "s2:node-names=n3,n4",
      "port=111",
      "backup-dirrrr=same",
      "n1:hostnameee=host1",
      "n2:tc-propertiesss.a.b.c=ABC"
    }, "Illegal setting name: backup-dirrrr",
      "Illegal setting name: hostnameee",
      "Illegal setting name: tc-propertiesss"
    );

    // bad property namespaces

    assertException(new String[]{
      "stripe-names=s1,s2",
      "s1:node-names=n1,n2",
      "s2:node-names=n3,n4",
      "port=111",
      ":backup-dir=same",
      "n1111:hostname=host1",
      "n2222:tc-properties.a.b.c=ABC"
    }, "Name '' in property ':backup-dir' is not a recognized stripe or node name",
      "Name 'n1111' in property 'n1111:hostname' is not a recognized stripe or node name",
      "Name 'n2222' in property 'n2222:tc-properties.a.b.c' is not a recognized stripe or node name"
    );
  }

  @Test
  public void test_unreferenced_node_or_stripe_names() throws IOException {
    assertException(new String[]{
      "stripe-names=s1,s2",
      "s1:node-names=n1,n2",
      "s2:node-names=n3,n4",
    }, "Stripe 's1' is not used (none of its nodes have any settings assigned). Consider removing it from the 'stripe-names' property.",
      "Stripe 's2' is not used (none of its nodes have any settings assigned). Consider removing it from the 'stripe-names' property.",
      "Node 'n1' has no settings assigned to it. Add at least one setting or remove it from the appropriate '<stripe>:node-names' property",
      "Node 'n2' has no settings assigned to it. Add at least one setting or remove it from the appropriate '<stripe>:node-names' property",
      "Node 'n3' has no settings assigned to it. Add at least one setting or remove it from the appropriate '<stripe>:node-names' property",
      "Node 'n4' has no settings assigned to it. Add at least one setting or remove it from the appropriate '<stripe>:node-names' property"
    );

    assertException(new String[]{
      "stripe-names=s1,s2",
      "s1:node-names=n1,n2",
      "stripe:s2:node-names=n3,n4",
      "s1:port=111",
    }, "Stripe 's2' is not used (none of its nodes have any settings assigned). Consider removing it from the 'stripe-names' property.",
      "Node 'n3' has no settings assigned to it. Add at least one setting or remove it from the appropriate '<stripe>:node-names' property",
      "Node 'n4' has no settings assigned to it. Add at least one setting or remove it from the appropriate '<stripe>:node-names' property"
    );

    assertException(new String[]{
      "stripe-names=s1,s2, s3",
      "s1:node-names=n1,n2",
      "s2:node-names=n3,4n",
      "port=111",
    }, "Stripe 's3' is not used (none of its nodes have any settings assigned). Consider removing it from the 'stripe-names' property.");
  }

  @Test
  public void test_write_output() throws URISyntaxException, IOException {
    // generate a cluster from a .properties file
    Properties properties = Props.load(Paths.get(getClass().getResource("/config2x2.properties").toURI()));
    Cluster cluster = new ClusterFactory().create(properties);
    Properties userDefined = cluster.toProperties(false, false, false);
    Properties hidden = cluster.toProperties(false, false, true);
    hidden.keySet().removeAll(userDefined.keySet());
    Properties defaults = cluster.toProperties(false, true, false);
    defaults.keySet().removeAll(userDefined.keySet());

    String fileHeader = "File Header";
    String userDefinedHeader = "User Defined Properties Header";
    String defaultHeader = "Default Properties Header";
    String hiddenHeader = "Hidden Properties Header";

    ConfigPropertiesTranslator translator = new ConfigPropertiesTranslator();

    // no headers, just user-defined props

    String output = translator.writeConfigOutput(userDefined);
    assertThat(output, not(containsString(fileHeader)));
    assertThat(output, not(containsString(userDefinedHeader)));
    assertThat(output, not(containsString(defaultHeader)));
    assertThat(output, not(containsString(hiddenHeader)));
    assertThat(output, containsString("stripe-names=stripe1,stripe"));

    // only a main header with user-defined props

    output = translator.writeConfigOutput(userDefined, fileHeader);
    assertThat(output, containsString(fileHeader));
    assertThat(output, not(containsString(userDefinedHeader)));
    assertThat(output, not(containsString(defaultHeader)));
    assertThat(output, not(containsString(hiddenHeader)));
    assertThat(output, containsString("stripe-names=stripe1,stripe"));

    // again, only a main header with user-defined props

    output = translator.writeConfigOutput(fileHeader,
        userDefined, userDefinedHeader, null, defaultHeader, null, hiddenHeader);
    assertThat(output, containsString(fileHeader));
    assertThat(output, containsString(userDefinedHeader));
    assertThat(output, not(containsString(defaultHeader)));
    assertThat(output, not(containsString(hiddenHeader)));
    assertThat(output, containsString("stripe-names=stripe1,stripe"));
    assertThat(output, not(containsString("whitelist=false"))); //default
    assertThat(output, not(containsString("cluster-uid=ZTYguzhRSdS6y5M9vnA5mw"))); //hidden

    // add default properties

    output = translator.writeConfigOutput(fileHeader,
        userDefined, userDefinedHeader, defaults, defaultHeader, null, hiddenHeader);
    assertThat(output, containsString(fileHeader));
    assertThat(output, containsString(userDefinedHeader));
    assertThat(output, containsString(defaultHeader));
    assertThat(output, not(containsString(hiddenHeader)));
    assertThat(output, containsString("stripe-names=stripe1,stripe"));
    assertThat(output, containsString("whitelist=false")); //default
    assertThat(output, not(containsString("cluster-uid=ZTYguzhRSdS6y5M9vnA5mw"))); //hidden

    // add hidden properties

    output = translator.writeConfigOutput(fileHeader,
        userDefined, userDefinedHeader, defaults, defaultHeader, hidden, hiddenHeader);
    assertThat(output, containsString(fileHeader));
    assertThat(output, containsString(userDefinedHeader));
    assertThat(output, containsString(defaultHeader));
    assertThat(output, containsString(hiddenHeader));
    assertThat(output, containsString("stripe-names=stripe1,stripe"));
    assertThat(output, containsString("whitelist=false")); //default
    assertThat(output, containsString("cluster-uid=ZTYguzhRSdS6y5M9vnA5mw")); //hidden

    // Load a file which has no defaults values remaining (all have been set)

    properties = Props.load(Paths.get(getClass().getResource("/config2x2_no_defaults.properties").toURI()));
    cluster = new ClusterFactory().create(properties);
    userDefined = cluster.toProperties(false, false, false);
    hidden = cluster.toProperties(false, false, true);
    hidden.keySet().removeAll(userDefined.keySet());
    defaults = cluster.toProperties(false, true, false);
    defaults.keySet().removeAll(userDefined.keySet());

    output = translator.writeConfigOutput(fileHeader,
        userDefined, userDefinedHeader, defaults, defaultHeader, hidden, hiddenHeader);
    assertThat(output, containsString("No default properties in use."));
  }

  @Test
  public void test_end_to_end_conversion_equality() throws URISyntaxException, IOException {

    // This test loads a 2x2 cluster, using a .properties file (based on the existing index-based namespace format)
    // and generates a Properties object and Cluster object. Then, these index-based Properties
    // are translated into their new name-based equivalents and a .cfg output string is generated.
    // The generated .cfg output is then re-translated back into a fresh set of Properties
    // and a new Cluster object is created again. This new cluster and the original cluster must be identical.

    // generate a cluster from a .properties file
    Properties propsFromPropertiesFile = Props.load(Paths.get(getClass().getResource("/config2x2.properties").toURI()));
    Cluster clusterFromPropertiesFile = new ClusterFactory().create(propsFromPropertiesFile);

    // convert the cluster Properties to .cfg string output
    ConfigPropertiesTranslator translator = new ConfigPropertiesTranslator();
    String configFileOutput = translator.writeConfigOutput(propsFromPropertiesFile);

    // re-convert the .cfg-based file back into .properties-based Properties
    InputStream inputStream = new ByteArrayInputStream(configFileOutput.getBytes(StandardCharsets.UTF_8));
    Properties propsFromConfigFile = translator.convert(new InputStreamReader(inputStream));

    // generate a cluster from the re-converted Properties
    Cluster clusterFromConfigFile = new ClusterFactory().create(propsFromConfigFile);

    // these clusters need to be identical
    assertEquals(clusterFromPropertiesFile, clusterFromConfigFile);
  }

  @Test
  public void test_cluster_from_cfg_properties() throws URISyntaxException {
    Cluster cluster = new ClusterFactory().create(Paths.get(getClass().getResource("/tc-config.cfg").toURI()));
    assertThat(cluster.getStripes().get(0).getNodeCount(), is(4));
    for (int i = 0; i < 4; i++) {
      Map<String, String> properties = cluster.getStripes().get(0).getNodes().get(i).getTcProperties().get();
      assert(properties != null);
      assertThat(properties.size(), is(25));
      assertThat(properties.get("prop-01"), is("true"));
      assertThat(properties.get("prop-02"), is("5000"));
      assertThat(properties.get("prop-03"), is("1000"));
      assertThat(properties.get("prop-04"), is("3"));
      assertThat(properties.get("prop-05"), is("true"));
      assertThat(properties.get("prop-06"), is("2"));
      assertThat(properties.get("prop-07"), is("5"));
      assertThat(properties.get("prop-08"), is("true"));
      assertThat(properties.get("prop-09"), is("5000"));
      assertThat(properties.get("prop-10"), is("1000"));
      assertThat(properties.get("prop-11"), is("3"));
      assertThat(properties.get("prop-12"), is("true"));
      assertThat(properties.get("prop-13"), is("2"));
      assertThat(properties.get("prop-14"), is("5"));
      assertThat(properties.get("prop-15"), is("true"));
      assertThat(properties.get("prop-16"), is("5000"));
      assertThat(properties.get("prop-17"), is("1000"));
      assertThat(properties.get("prop-18"), is("3"));
      assertThat(properties.get("prop-19"), is("true"));
      assertThat(properties.get("prop-20"), is("2"));
      assertThat(properties.get("prop-21"), is("5"));
      assertThat(properties.get("prop-22"), is("true"));
      assertThat(properties.get("prop-23"), is("15000"));
      assertThat(properties.get("prop-24"), is("true"));
      assertThat(properties.get("prop-25"), is("15000"));
    }
  }

  private Reader getReader(String[] params) {
    InputStream inputStream = new ByteArrayInputStream(String.join(System.lineSeparator(),params).getBytes(StandardCharsets.UTF_8));
    return new InputStreamReader(inputStream);
  }

  private Properties translateToProperties(Reader reader) throws IOException {
    ConfigPropertiesTranslator translator = new ConfigPropertiesTranslator();
    return translator.convert(reader);
  }

  private String translateToConfigFormat(Properties properties) {
    ConfigPropertiesTranslator translator = new ConfigPropertiesTranslator();
    return translator.writeConfigOutput(properties);
  }

  private void assertException(String[] config, String ... errors) {
    String[] local = new String[6]; // hamcrest supports a max of 6 matchers
    Arrays.fill(local, "");
    System.arraycopy(errors, 0, local, 0, errors.length);
    assertThat(
      () -> translateToProperties(getReader(config)),
      is(throwing(instanceOf(IllegalArgumentException .class)).andMessage(allOf(
        containsString(local[0]),
        containsString(local[1]),
        containsString(local[2]),
        containsString(local[3]),
        containsString(local[4]),
        containsString(local[5])
      ))));
  }
}
