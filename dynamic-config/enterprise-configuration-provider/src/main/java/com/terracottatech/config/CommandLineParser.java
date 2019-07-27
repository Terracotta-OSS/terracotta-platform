/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.terracottatech.config.CommandLineParser.Opt.CONFIG;
import static com.terracottatech.config.CommandLineParser.Opt.CONFIG_CONSISTENCY;
import static com.terracottatech.config.CommandLineParser.Opt.CONFIG_REPO;
import static com.terracottatech.config.CommandLineParser.Opt.NODE_NAME;

class CommandLineParser {

  enum Opt {
    CONFIG(null, "config"),
    CONFIG_REPO("r", "config-repo"),
    CONFIG_CONSISTENCY(null, "config-consistency"),
    NODE_NAME(null, "node-name");

    final String shortName;
    final String longName;

    Opt(String shortName, String longName) {
      this.shortName = shortName;
      this.longName = longName;
    }

    String shortName() {
      return shortName;
    }

    String longName() {
      return longName;
    }

    String longOption() {
      return "--" + longName;
    }
  }

  private final String config;
  private final Path configurationRepo;
  private final boolean configConsistencyMode;
  private final String nodeName;

  CommandLineParser(List<String> args) throws ParseException {
    CommandLine commandLine = new DefaultParser().parse(createOptions(), args.toArray(new String[0]));

    this.config = commandLine.getOptionValue(CONFIG.longName());
    this.configurationRepo = commandLine.hasOption(CONFIG_REPO.shortName()) ?
        Paths.get(commandLine.getOptionValue(CONFIG_REPO.shortName())).toAbsolutePath() : null;
    this.configConsistencyMode = commandLine.hasOption(CONFIG_CONSISTENCY.longName());
    this.nodeName = commandLine.getOptionValue(NODE_NAME.longName());

    if (this.configConsistencyMode && this.config == null) {
      throw new RuntimeException(CONFIG.longName() + " must be specified when " + CONFIG_CONSISTENCY.longName() + " is used");
    }

    if (!this.configConsistencyMode && this.configurationRepo == null) {
      throw new UnrecognizedOptionException("Either " + CONFIG_REPO.longName() + " or " + CONFIG_CONSISTENCY.longName() + " must be specified");
    }
  }

  public String getConfig() {
    return config;
  }

  Optional<Path> getConfigurationRepositoryPath() {
    return Optional.ofNullable(configurationRepo);
  }

  boolean isConfigConsistencyMode() {
    return configConsistencyMode;
  }

  public String getNodeName() {
    return nodeName;
  }

  static String getConfigurationParamsDescription() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    new HelpFormatter().printOptions(printWriter, HelpFormatter.DEFAULT_WIDTH, createOptions(), 1, 1);
    printWriter.close();
    return stringWriter.toString();
  }

  private static Options createOptions() {
    Options options = new Options();

    options.addOption(
        Option.builder(CONFIG.shortName())
            .longOpt(CONFIG.longName())
            .hasArg()
            .argName("config")
            .desc("Specifies the configuration location (optional)")
            .build()
    );

    options.addOption(
        Option.builder(CONFIG_REPO.shortName())
            .longOpt(CONFIG_REPO.longName())
            .hasArg()
            .argName("config-repo")
            .desc("Specifies the configuration repo location")
            .build()
    );

    options.addOption(
        Option.builder(CONFIG_CONSISTENCY.shortName())
            .longOpt(CONFIG_CONSISTENCY.longName())
            .desc("Starts the node in config consistency mode (optional)")
            .build()
    );

    options.addOption(
        Option.builder(NODE_NAME.shortName())
            .longOpt(NODE_NAME.longName())
            .hasArg()
            .argName("node-name")
            .desc("Node name (required)")
            .build()
    );

    return options;
  }
}
