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
package org.terracotta.config.generator;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateFromEnvironmentVariables {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenerateFromEnvironmentVariables.class);
  static final String TCCONFIG_ = "TCCONFIG_";
  private static final String GENERIC_ERROR_MESSAGE = "You should call the tc-config-generator with exactly four arguments.\n" +
      "Examples : \n" +
      "java -jar tc-config-generator -i /Users/anthony/tc-template.ftlh -o /Users/anthony/tc-config.xml";

  private Configuration configuration;

  public GenerateFromEnvironmentVariables(Configuration configuration) {
    this.configuration = configuration;
  }

  private static Configuration createTemplateConfiguration(File baseDir) throws IOException {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
    configuration.setTemplateLoader(new FileTemplateLoader(baseDir));
    configuration.setDefaultEncoding("UTF-8");
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    configuration.setLogTemplateExceptions(false);
    return configuration;
  }

  public static void main(String[] args) throws IOException, TemplateException {
    if (args.length != 4) {
      LOGGER.error("Wrong number of arguments : \n" + GENERIC_ERROR_MESSAGE);
      System.exit(40);
    }
    if (!args[0].equals("-i") || !args[2].equals("-o")) {
      LOGGER.error("Wrong arguments : \n" + GENERIC_ERROR_MESSAGE);
      System.exit(40);
    }
    File inputFile = new File(args[1]);
    if (!inputFile.exists() || !inputFile.isFile()) {
      LOGGER.error("Please check your input file path\n" + GENERIC_ERROR_MESSAGE);
      System.exit(40);
    }
    File outputFile = new File(args[3]);

    String tcConfigGeneratorPrefix = System.getProperty("tcConfigGeneratorPrefix", TCCONFIG_);

    Map<String, String> envMap = System.getenv();
    if (envMap.keySet().stream().anyMatch(s -> s.startsWith(tcConfigGeneratorPrefix))) {
      LOGGER.info("We found the following relevant environment variables : ");
      envMap.keySet().stream().filter(s -> s.startsWith(tcConfigGeneratorPrefix)).forEach(key -> {
        LOGGER.info(key);
      });
    }
    else {
      LOGGER.warn("Please make sure you provided environment variables starting with " + tcConfigGeneratorPrefix);
    }

    Configuration templateConfiguration = createTemplateConfiguration(inputFile.getParentFile());
    GenerateFromEnvironmentVariables generateFromEnvironmentVariables = new GenerateFromEnvironmentVariables(templateConfiguration);
    Map<String, Object> root = generateFromEnvironmentVariables.retrieveNamesWithPrefix(envMap, tcConfigGeneratorPrefix);
    generateFromEnvironmentVariables.generateXmlFile(root, inputFile.getName(), new FileWriter(outputFile));
    LOGGER.info("Successfully generated " + args[3]);
  }

  void generateXmlFile(Map<String, Object> root, String inputFileName, Writer output) throws IOException, TemplateException {
    Template template = configuration.getTemplate(inputFileName);
    template.process(root, output);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  Map<String, Object> retrieveNamesWithPrefix(Map<String, String> envMap, String envVariablePrefix) {
    // make sure keys are sorted
    Map<String, String> sortedMap = new TreeMap(envMap);
    Map<String, Object> resultMap = new TreeMap<>();
    sortedMap.entrySet().forEach(envEntry -> {
      if (envEntry.getKey().startsWith(envVariablePrefix)) {
        // single entry
        String regexp = envVariablePrefix + "([a-zA-Z_]*)";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(envEntry.getKey());
        if (matcher.matches() && !matcher.group(1).contains("[")) {
          resultMap.put(toCamelCase(matcher.group(1)), envEntry.getValue());
        }
        // array entry
        regexp = envVariablePrefix + "([a-zA-Z_]*)([\\d]+)_([a-zA-Z_]+)";
        pattern = Pattern.compile(regexp);
        matcher = pattern.matcher(envEntry.getKey());
        if (matcher.matches()) {
          List list;
          String listName = toCamelCase(matcher.group(1));
          int index = Integer.parseInt(matcher.group(2));
          String variableName = toCamelCase(matcher.group(3));
          if (!resultMap.containsKey(listName)) {
            list = new ArrayList();
            resultMap.put(listName, list);
          } else {
            list = (List) resultMap.get(listName);
          }
          Map<String, String> complexVariable;
          if (list.size() > index) {
            complexVariable = (Map<String, String>) list.get(index);
          } else {
            complexVariable = new TreeMap<>();
            list.add(complexVariable);
          }
          complexVariable.put(variableName, envEntry.getValue());
        }
      }
    });
    return resultMap;
  }

  //https://stackoverflow.com/a/1143979/24069
  static String toCamelCase(String s) {
    String[] parts = s.split("_");
    String camelCaseString = "";
    for (String part : parts) {
      camelCaseString = camelCaseString + toProperCase(part);
    }
    return camelCaseString.substring(0, 1).toLowerCase() + camelCaseString.substring(1);
  }

  static String toProperCase(String s) {
    return s.substring(0, 1).toUpperCase() +
        s.substring(1).toLowerCase();
  }

}
