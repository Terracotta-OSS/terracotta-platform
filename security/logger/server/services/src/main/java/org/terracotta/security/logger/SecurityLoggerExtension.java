/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.security.logger;

import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigExtension;
import org.terracotta.dynamic_config.api.server.PathResolver;
import org.terracotta.entity.PlatformConfiguration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import ch.qos.logback.classic.Logger;
import org.terracotta.dynamic_config.api.model.Node;

import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.SettingName;

/**
 * @author Mathieu Carbou
 */
public class SecurityLoggerExtension implements DynamicConfigExtension {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SecurityLoggerExtension.class);

  // Our logger name: never changes
  private static final String SECURITY_LOGGER_NAME = "SecurityLogger";

  // When both:
  // - JSON_LOGGING=true (env var defaulted to false)
  // - JSON_LOGGING_SECURITY=true (env var defaulted to true)
  // Then: this logger will grab the appender named JSON-SECURITY.
  // This appender is configured when building the Docker image with a special layout
  //
  // <appender name="JSON-SECURITY" class="com.tc.l2.logging.BufferingAppender">
  //   <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
  //     <layout class="com.terracottatech.cloud.logging.json.TerracottaJsonLayout">
  //       <file>terracotta-security.log</file>
  //       <product>Terracotta DB</product>
  //     </layout>
  //   </encoder>
  // </appender>
  //
  // <logger name="SecurityLogger" level="INFO" additivity="false">
  //   <appender-ref ref="JSON-SECURITY"/>
  // </logger>
  //
  private static final String JSON_LOGGING_ENV_KEY = "JSON_LOGGING";
  private static final String JSON_LOGGING_SECURITY_ENV_KEY = "JSON_LOGGING_SECURITY";
  private static final String JSON_LOGGING_SECURITY_APPENDER_NAME = "JSON-SECURITY";

  private static final String LOG_PATTERN = "%d{HH:mm:ss} - %msg%n";
  private static final String LOG_FILE_MAX_SIZE = "10MB";
  private static final String LOG_FILE_NAME = "terracotta-security-log-%d{yyyy-MM-dd}.%i.log";

  @Override
  public void configure(Registrar registrar, PlatformConfiguration platformConfiguration) {
    ConfigChangeHandlerManager configChangeHandlerManager = findService(platformConfiguration, ConfigChangeHandlerManager.class);
    IParameterSubstitutor parameterSubstitutor = findService(platformConfiguration, IParameterSubstitutor.class);
    TopologyService topologyService = findService(platformConfiguration, TopologyService.class);
    PathResolver pathResolver = findService(platformConfiguration, PathResolver.class);
    Node node = topologyService.getRuntimeNodeContext().getNode();

    // add support for "security-log-dir" setting in DC config
    configChangeHandlerManager.set(Setting.SECURITY_LOG_DIR, new SecurityLoggerChangeHandler(parameterSubstitutor, pathResolver));

    // muted by default
    SecurityLogger securityLogger = SecurityLogger.NOOP;

    // Check if JSON_LOGGING is activated
    if ("true".equals(System.getenv(JSON_LOGGING_ENV_KEY)) && "true".equals(System.getenv(JSON_LOGGING_SECURITY_ENV_KEY))) {
      // Grab our logger to get the appender
      final Logger delegate = (Logger) LoggerFactory.getLogger(SECURITY_LOGGER_NAME);

      if (delegate.getAppender(JSON_LOGGING_SECURITY_APPENDER_NAME) == null) {
        throw new IllegalStateException("'" + JSON_LOGGING_ENV_KEY + "' and '" + JSON_LOGGING_SECURITY_ENV_KEY + "' are activated but no appender named '" + JSON_LOGGING_SECURITY_APPENDER_NAME + "' was configured");
      }

      LOGGER.info("Security Logger activated with JSON Logging: logs will be written to appender: {}", JSON_LOGGING_SECURITY_APPENDER_NAME);
      securityLogger = new SecurityLogger() {
        @Override
        public void log(String message, Object... args) {
          delegate.info(message, args);
        }
      };

    } else {
      // File-based configuration
      final RawPath configuredSecurityLogDir = node.getSecurityLogDir().orDefault();

      if (configuredSecurityLogDir == null) {
        // No security log dir ?
        // We need to decide where to write these logs depending on the environment
        if ("true".equals(System.getenv(JSON_LOGGING_ENV_KEY))) { // and JSON_LOGGING_SECURITY_ENV_KEY == false
          // We do not have a security directory configured, and we cannot write to the console because JSON logging is activated but not JSON_LOGGING_SECURITY.
          // This is a special case where we have to mute them.
          LOGGER.warn("Security Logger deactivated: " + SettingName.SECURITY_LOG_DIR + " is missing");
          securityLogger = SecurityLogger.NOOP;

        } else {
          // No JSON logging and no security log dir configured: go to the console
          LOGGER.info("Security Logger activated on default appender");
          securityLogger = new SecurityLogger() {
            final Logger delegate = (Logger) LoggerFactory.getLogger(SECURITY_LOGGER_NAME);

            @Override
            public void log(String message, Object... args) {
              delegate.info(message, args);
            }
          };
        }

      } else {
        // User has configured a folder where to store the logs
        final Path localPath;

        try {
          localPath = Files.createDirectories(parameterSubstitutor.substitute(pathResolver.resolve(configuredSecurityLogDir.toPath())));
        } catch (IOException e) {
          throw new IllegalStateException("Target " + SettingName.SECURITY_LOG_DIR + ": " + configuredSecurityLogDir + " is invalid: " + e.getMessage(), e);
        }

        // These validations are only useful when starting after a bew node is activated.
        // Activation phase does not validate the DC configuration within each node before they are activated because there is no transaction (nodes are in diagnostic state).
        // The validations in the config change handler only apply for a DC change done at runtime.
        if (!Files.isReadable(localPath)) {
          throw new IllegalStateException("Target " + SettingName.SECURITY_LOG_DIR + ": " + localPath + " doesn't have read permissions for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
        }
        if (!Files.isWritable(localPath)) {
          throw new IllegalStateException("Target " + SettingName.SECURITY_LOG_DIR + ": " + localPath + " doesn't have write permissions for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
        }

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger delegate = loggerContext.getLogger(SECURITY_LOGGER_NAME);

        // if our log appender has not already been configured...
        if (!delegate.iteratorForAppenders().hasNext()) {
          PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
          logEncoder.setContext(loggerContext);
          logEncoder.setPattern(LOG_PATTERN);
          logEncoder.start();

          RollingFileAppender<ILoggingEvent> logFileAppender = new RollingFileAppender<>();
          logFileAppender.setContext(loggerContext);
          logFileAppender.setEncoder(logEncoder);
          logFileAppender.setAppend(true);

          SizeAndTimeBasedRollingPolicy<?> logFilePolicy = new SizeAndTimeBasedRollingPolicy<>();
          logFilePolicy.setContext(loggerContext);
          logFilePolicy.setParent(logFileAppender);
          logFilePolicy.setMaxFileSize(FileSize.valueOf(LOG_FILE_MAX_SIZE));
          logFilePolicy.setFileNamePattern(localPath.resolve(LOG_FILE_NAME).toString());
          logFilePolicy.start();

          logFileAppender.setRollingPolicy(logFilePolicy);
          logFileAppender.start();

          delegate.setLevel(Level.INFO);
          delegate.addAppender(logFileAppender);

        } else {
          LOGGER.warn("Security Logger appender {} already exists!", SECURITY_LOGGER_NAME);
        }

        LOGGER.info("Security Logger activated: logs will be written to: {}", localPath);
        securityLogger = new SecurityLogger() {
          @Override
          public void log(String message, Object... args) {
            delegate.info(message, args);
          }
        };
      }
    }

    registrar.registerExtendedConfiguration(SecurityLogger.class, securityLogger);

    securityLogger.log("Security logging activated.");
    securityLogger.log("Server starting at: {}", node.getBindHostPort());
  }
}
