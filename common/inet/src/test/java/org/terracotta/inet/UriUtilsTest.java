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
package org.terracotta.inet;

import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;

/**
 * Created by Anthony Dahanne on 2015-12-01.
 */
public class UriUtilsTest {

  /*parseInetSocketAddresses tests*/
  @Test
  public void testParseAuthorities_missing_authority() {
    assertThat(UriUtils.parseHostPorts("localhost").size(), equalTo(0));
  }

  @Test
  public void testParseAuthorities_missing_port() {
    List<HostPort> socketAddresses = UriUtils.parseHostPorts("scheme://localhost");
    assertThat(socketAddresses.size(), equalTo(1));
    assertThat(socketAddresses.get(0).getHost(), equalTo("localhost"));
    assertThat(socketAddresses.get(0).getPort(), equalTo(9410));
  }

  @Test
  public void testParseAuthorities_all_explicit_port() {
    List<HostPort> socketAddresses = UriUtils.parseHostPorts("scheme://localhost:9510");
    assertThat(socketAddresses.size(), equalTo(1));
    assertThat(socketAddresses.get(0).getHost(), equalTo("localhost"));
    assertThat(socketAddresses.get(0).getPort(), equalTo(9510));
  }

  @Test
  public void generateHostPortsByServerNamesFromUriTest() {
    List<HostPort> hostPortsByServerNames = UriUtils.parseHostPorts("terracotta://server1:9510,server2:9511/anEntity");
    assertEquals(hostPortsByServerNames, Arrays.asList(
        HostPort.create("server1", 9510),
        HostPort.create("server2", 9511)
    ));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testParseAuthorities_too_many_colons() {
    UriUtils.parseHostPorts("scheme://localhost:9510:110");
  }

  /*parseScheme tests*/
  @Test
  public void testParseScheme_emptyInput() {
    String input = "";
    String scheme = UriUtils.parseScheme(input);
    String expectedScheme = URI.create(input).getScheme();
    assertThat(input, scheme, equalTo(expectedScheme));
  }

  @Test
  public void testParseScheme_noScheme() {
    String input = "scheme";
    String scheme = UriUtils.parseScheme(input);
    String expectedScheme = URI.create(input).getScheme();
    assertThat(input, scheme, equalTo(expectedScheme));
  }

  @Test
  public void testParseScheme_noScheme2() {
    String input = "://";
    String scheme = UriUtils.parseScheme(input);
    assertThat(input, scheme, is(nullValue()));
  }

  @Test
  public void testParseScheme_noScheme3() {
    String input = "hello:80";
    String scheme = UriUtils.parseScheme(input);
    assertThat(input, scheme, equalTo(URI.create(input).getScheme()));
  }

  @Test
  public void testParseScheme_tinyScheme() {
    String input = "s://user@host";
    String scheme = UriUtils.parseScheme(input);
    String expectedScheme = URI.create(input).getScheme();
    assertThat(input, scheme, equalTo(expectedScheme));
  }

  @Test
  public void testParseScheme_normal() {
    String input = "terracotta://host,[::b]:1234";
    String scheme = UriUtils.parseScheme(input);
    assertThat(input, scheme, equalTo("terracotta"));
  }

  /*parseAuthority tests*/
  @Test
  public void testParseAuthority_noAuthority_1() {
    String input = "relative/path/to/something";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_noAuthority_2() {
    String input = "/path/to/something";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_noAuthority_3() {
    String input = "?key=value";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_noAuthority_4() {
    String input = "#blahBlah";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_noAuthority_5() {
    String input = "scheme:///path/to/something";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_schemeAndAuthority() {
    String input = "scheme://some-authority";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_schemeUserInfoAndAuthority() {
    String input = "scheme://somebody@some-authority";
    String emptyScheme = UriUtils.parseScheme(input);
    String expectedScheme = URI.create(input).getScheme();
    assertThat(input, emptyScheme, equalTo(expectedScheme));
  }

  @Test
  public void testParseAuthority_schemeUserInfoAndServerPort() {
    String input = "scheme://somebody@localhost:9510";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_schemeServerPortAndPath() {
    String input = "scheme://localhost:9510/some/dir";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_schemeUserInfoServerPortAndQuery() {
    String input = "scheme://somebody@localhost:9510?myQuery";
    String emptyScheme = UriUtils.parseScheme(input);
    String expectedScheme = URI.create(input).getScheme();
    assertThat(input, emptyScheme, equalTo(expectedScheme));
  }

  @Test
  public void testParseAuthority_schemeServerPortAndFragment() {
    String input = "scheme://localhost:9510#frag";
    String authority = UriUtils.parseAuthority(input);
    String expectedAuthority = URI.create(input).getAuthority();
    assertThat(input, authority, equalTo(expectedAuthority));
  }

  @Test
  public void testParseAuthority_bigUri() {
    String input = "scheme://localhost:9180,[::1]:9280,::2,[fe80::aaaa]:9380/path?simulation-freq=10&max-servers=2&timeout=2";
    String authority = UriUtils.parseAuthority(input);
    assertThat(input, authority, equalTo("localhost:9180,[::1]:9280,::2,[fe80::aaaa]:9380"));
  }

  @Test
  public void testParseAuthority_no_url_scheme() {
    String input = "localhost:9180";
    assertThat(input, UriUtils.parseAuthority(input), is(nullValue()));
    assertThat(input, UriUtils.parseAuthority(input), equalTo(URI.create(input).getAuthority()));
  }

  @Test
  public void testParseAuthority_no_url_scheme_2() {
    String input = "localhost";
    assertThat(input, UriUtils.parseAuthority(input), is(nullValue()));
    assertThat(input, UriUtils.parseAuthority(input), equalTo(URI.create(input).getAuthority()));
  }

  /*parseUserInfo tests*/
  @Test
  public void testParseUserInfo_noUserInfo_1() {
    String input = "scheme://localhost:9510#frag";
    String userInfo = UriUtils.parseUserInfo(input);
    String expectedUserInfo = URI.create(input).getUserInfo();
    assertThat(input, userInfo, equalTo(expectedUserInfo));
  }

  @Test
  public void testParseUserInfo_noUserInfo_2() {
    String input = "/path/to/something";
    String userInfo = UriUtils.parseUserInfo(input);
    String expectedUserInfo = URI.create(input).getUserInfo();
    assertThat(input, userInfo, equalTo(expectedUserInfo));
  }

  @Test
  public void testParseUserInfo_noUserInfo_tricky() {
    String input = "scheme://user@localhost:9510,some-host,otherhost.com";
    String userInfo = UriUtils.parseUserInfo(input);
    String expectedUserInfo = URI.create(input).getUserInfo();
    assertThat(input, userInfo, equalTo(expectedUserInfo));
  }

  @Test
  public void testParseUserInfo() {
    String input = "scheme://user@localhost:9510";
    String userInfo = UriUtils.parseUserInfo(input);
    String expectedUserInfo = URI.create(input).getUserInfo();
    assertThat(input, userInfo, equalTo(expectedUserInfo));
  }

  @Test
  public void testParseUserInfo_bigUri() {
    String input = "scheme://myUser@[::1]:9280/path?simulation-freq=10&max-servers=2&timeout=2";
    String userInfo = UriUtils.parseUserInfo(input);
    String expectedUserInfo = URI.create(input).getUserInfo();
    assertThat(input, userInfo, equalTo(expectedUserInfo));
  }

  /*parsePath tests*/
  @Test
  public void testParsePath_noPath_1() {
    String input = "scheme://myUser@[::1]:9280?simulation-freq=10&max-servers=2&timeout=2";
    String path = UriUtils.parsePath(input);
    String expectedPath = URI.create(input).getPath();
    assertThat(input, path, equalTo(expectedPath));
  }

  @Test
  public void testParsePath_noPath_2() {
    String input = "scheme://myUser@[::1]:9280?simulation-freq=10&max-servers=2&timeout=2";
    String path = UriUtils.parsePath(input);
    String expectedPath = URI.create(input).getPath();
    assertThat(input, path, equalTo(expectedPath));
  }

  @Test
  public void testParsePath_ok_1() {
    String input = "stripe://myUser@[::1]:9280/some/path?simulation-freq=10&max-servers=2&timeout=2";
    String path = UriUtils.parsePath(input);
    String expectedPath = URI.create(input).getPath();
    assertThat(input, path, equalTo(expectedPath));
  }

  @Test
  public void testParsePath_ok_2() {
    String input = "stripe://localhost:9280,other-host,host.com/some/path?simulation-freq=10&max-servers=2&timeout=2";
    String path = UriUtils.parsePath(input);
    String expectedPath = URI.create(input).getPath();
    assertThat(input, path, equalTo(expectedPath));
  }

  @Test
  public void testParsePath_absolutePathWithQuery() {
    String input = "/path/to/resource?simulation-freq=10&max-servers=2&timeout=2";
    String path = UriUtils.parsePath(input);
    String expectedPath = URI.create(input).getPath();
    assertThat(input, path, equalTo(expectedPath));
  }

  @Test
  public void testParsePath_absolutePathWithFragment() {
    String input = "/path/to/resource#frag";
    String path = UriUtils.parsePath(input);
    String expectedPath = URI.create(input).getPath();
    assertThat(input, path, equalTo(expectedPath));
  }

  @Test
  public void testParsePath_relativePathWithQueryAndFragment() {
    String input = "path/to/resource?simulation-freq=10&max-servers=2&timeout=2#frag";
    String path = UriUtils.parsePath(input);
    String expectedPath = URI.create(input).getPath();
    assertThat(input, path, equalTo(expectedPath));
  }

  /*parseQuery tests*/
  @Test
  public void testParseQuery_noQuery_1() {
    String input = "path/to/resource";
    String query = UriUtils.parseQuery(input);
    String expectedQuery = URI.create(input).getQuery();
    assertThat(input, query, equalTo(expectedQuery));
  }

  @Test
  public void testParseQuery_noQuery_2() {
    String input = "scheme://[::2]#source";
    String query = UriUtils.parseQuery(input);
    String expectedQuery = URI.create(input).getQuery();
    assertThat(input, query, equalTo(expectedQuery));
  }

  @Test
  public void testParseQuery_simpleQuery() {
    String input = "scheme://host?key=value";
    String query = UriUtils.parseQuery(input);
    String expectedQuery = URI.create(input).getQuery();
    assertThat(input, query, equalTo(expectedQuery));
  }

  @Test
  public void testParseQuery_bigUri() {
    String input = "scheme://host,other-host:9910/path?simulation-freq=10&max-servers=2&timeout=2#some-fragment";
    String query = UriUtils.parseQuery(input);
    String expectedQuery = URI.create(input).getQuery();
    assertThat(input, query, equalTo(expectedQuery));
  }

  @Test
  public void testParseQuery_onlyQuery() {
    String input = "?simulation-freq=10&max-servers=2&timeout=2#some-fragment";
    String query = UriUtils.parseQuery(input);
    String expectedQuery = URI.create(input).getQuery();
    assertThat(input, query, equalTo(expectedQuery));
  }

  /*parseSegments tests*/
  @Test
  public void testParseSegments_emptyPath() {
    String input = "scheme://host?simulation-freq=10&max-servers=2&timeout=2";
    List<String> pathSegments = UriUtils.parseSegments(input);
    assertThat(input, pathSegments.size(), is(0));
  }

  @Test
  public void testParseSegments_ok_1() {
    String input = "/path/to/resource?simulation-freq=10&max-servers=2&timeout=2";
    List<String> pathSegments = UriUtils.parseSegments(input);
    assertThat(input, pathSegments, hasItems("path", "to", "resource"));
  }

  @Test
  public void testParseSegments_ok_2() {
    String input = "scheme:///path/to/resource#simulation-freq=10&max-servers=2&timeout=2";
    List<String> pathSegments = UriUtils.parseSegments(input);
    assertThat(input, pathSegments, hasItems("path", "to", "resource"));
  }

  @Test
  public void testParseSegments_ok_3() {
    String input = "scheme://host/some/path#simulation-freq=10&max-servers=2&timeout=2";
    List<String> pathSegments = UriUtils.parseSegments(input);
    assertThat(input, pathSegments, hasItems("some", "path"));
  }

  @Test
  public void test_appendSegmentToPath() {
    assertThat(UriUtils.appendSegmentToPath(null, null), equalTo(""));
    assertThat(UriUtils.appendSegmentToPath(null, ""), equalTo(""));
    assertThat(UriUtils.appendSegmentToPath(null, "/"), equalTo("/"));
    assertThat(UriUtils.appendSegmentToPath(null, "bar"), equalTo("bar"));
    assertThat(UriUtils.appendSegmentToPath(null, "/bar"), equalTo("/bar"));
    assertThat(UriUtils.appendSegmentToPath(null, "bar/"), equalTo("bar/"));

    assertThat(UriUtils.appendSegmentToPath("", null), equalTo(""));
    assertThat(UriUtils.appendSegmentToPath("", ""), equalTo(""));
    assertThat(UriUtils.appendSegmentToPath("", "/"), equalTo("/"));
    assertThat(UriUtils.appendSegmentToPath("", "bar"), equalTo("bar"));
    assertThat(UriUtils.appendSegmentToPath("", "/bar"), equalTo("/bar"));
    assertThat(UriUtils.appendSegmentToPath("", "bar/"), equalTo("bar/"));

    assertThat(UriUtils.appendSegmentToPath("/", null), equalTo("/"));
    assertThat(UriUtils.appendSegmentToPath("/", ""), equalTo("/"));
    assertThat(UriUtils.appendSegmentToPath("/", "/"), equalTo("/"));
    assertThat(UriUtils.appendSegmentToPath("/", "bar"), equalTo("/bar"));
    assertThat(UriUtils.appendSegmentToPath("/", "/bar"), equalTo("/bar"));
    assertThat(UriUtils.appendSegmentToPath("/", "bar/"), equalTo("/bar/"));

    assertThat(UriUtils.appendSegmentToPath("foo", null), equalTo("foo"));
    assertThat(UriUtils.appendSegmentToPath("foo", ""), equalTo("foo"));
    assertThat(UriUtils.appendSegmentToPath("foo", "/"), equalTo("foo/"));
    assertThat(UriUtils.appendSegmentToPath("foo", "bar"), equalTo("foo/bar"));
    assertThat(UriUtils.appendSegmentToPath("foo", "/bar"), equalTo("foo/bar"));
    assertThat(UriUtils.appendSegmentToPath("foo", "bar/"), equalTo("foo/bar/"));

    assertThat(UriUtils.appendSegmentToPath("foo/", null), equalTo("foo/"));
    assertThat(UriUtils.appendSegmentToPath("foo/", ""), equalTo("foo/"));
    assertThat(UriUtils.appendSegmentToPath("foo/", "/"), equalTo("foo/"));
    assertThat(UriUtils.appendSegmentToPath("foo/", "bar"), equalTo("foo/bar"));
    assertThat(UriUtils.appendSegmentToPath("foo/", "/bar"), equalTo("foo/bar"));
    assertThat(UriUtils.appendSegmentToPath("foo/", "bar/"), equalTo("foo/bar/"));
  }
}
