/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.terracottatech.dynamic_config.config.Node;

import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class SecurityTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testBasic() throws IOException {
    Path securityPath = temporaryFolder.newFolder().toPath();
    Path auditPath = temporaryFolder.newFolder().toPath();
    Node node = new Node();
    node.setSecurityDir(securityPath);
    node.setSecurityWhitelist(true);
    node.setSecuritySslTls(true);
    node.setSecurityAuthc("file");
    node.setSecurityAuditLogDir(auditPath);

    com.terracottatech.config.security.Security actual = new Security(node).createSecurity();

    assertThat(actual, notNullValue());
    assertThat(actual.getAuditDirectory(), is(auditPath.toString()));
    assertThat(actual.getSecurityRootDirectory(), is(securityPath.toString()));
    assertThat(actual.getAuthentication(), notNullValue());
    assertThat(actual.getAuthentication().getFile(), notNullValue());
    assertThat(actual.getWhitelist(), notNullValue());
    assertThat(actual.getSslTls(), notNullValue());
  }

  @Test
  public void testAuditDirectoryNotConfigured() throws IOException {
    Path securityPath = temporaryFolder.newFolder().toPath();
    Node node = new Node();
    node.setSecurityDir(securityPath);
    node.setSecurityAuditLogDir(null);

    com.terracottatech.config.security.Security actual = new Security(node).createSecurity();

    assertThat(actual, notNullValue());
    assertThat(actual.getAuditDirectory(), nullValue());
  }

  @Test
  public void testWhitelistNotConfigured() throws IOException {
    Path securityPath = temporaryFolder.newFolder().toPath();
    Node node = new Node();
    node.setSecurityDir(securityPath);
    node.setSecurityWhitelist(false);

    com.terracottatech.config.security.Security actual = new Security(node).createSecurity();

    assertThat(actual, notNullValue());
    assertThat(actual.getWhitelist(), nullValue());
  }

  @Test
  public void testSslTlsNotConfigured() throws IOException {
    Path securityPath = temporaryFolder.newFolder().toPath();
    Node node = new Node();
    node.setSecurityDir(securityPath);
    node.setSecuritySslTls(false);

    com.terracottatech.config.security.Security actual = new Security(node).createSecurity();

    assertThat(actual, notNullValue());
    assertThat(actual.getSslTls(), nullValue());
  }

  @Test
  public void testAuthenticationNotConfigured() throws IOException {
    Path securityPath = temporaryFolder.newFolder().toPath();
    Node node = new Node();
    node.setSecurityDir(securityPath);
    node.setSecurityAuthc(null);

    com.terracottatech.config.security.Security actual = new Security(node).createSecurity();

    assertThat(actual, notNullValue());
    assertThat(actual.getAuthentication(), nullValue());
  }

  @Test
  public void testCertificateAuthentication() throws IOException {
    Path securityPath = temporaryFolder.newFolder().toPath();
    Node node = new Node();
    node.setSecurityDir(securityPath);
    node.setSecurityAuthc("certificate");

    com.terracottatech.config.security.Security actual = new Security(node).createSecurity();

    assertThat(actual, notNullValue());
    assertThat(actual.getAuthentication(), notNullValue());
    assertThat(actual.getAuthentication().getCertificate(), notNullValue());
  }

  @Test
  public void testLdapAuthentication() throws IOException {
    Path securityPath = temporaryFolder.newFolder().toPath();
    Node node = new Node();
    node.setSecurityDir(securityPath);
    node.setSecurityAuthc("ldap");

    com.terracottatech.config.security.Security actual = new Security(node).createSecurity();

    assertThat(actual, notNullValue());
    assertThat(actual.getAuthentication(), notNullValue());
    assertThat(actual.getAuthentication().getLdap(), notNullValue());
  }
}