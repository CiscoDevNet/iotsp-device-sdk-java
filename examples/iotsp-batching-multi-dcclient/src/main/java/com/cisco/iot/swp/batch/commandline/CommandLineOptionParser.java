/**
 * Copyright (c) 2017 by Cisco Systems, Inc. All Rights Reserved Cisco Systems Confidential
 */
package com.cisco.iot.swp.batch.commandline;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineOptionParser {

  private static List<String> dataOptions =
      Arrays.asList("d", "data-file", "name of data file(absolute path).");
  private static List<String> helpOptions =
      Arrays.asList("h", "help", "help on using the jar file");
  private static List<String> configFileOptions =
      Arrays.asList("c", "config-file", "Config file having data connect details/Rulesets/Assets.");
  private static List<String> dataConnectClientOption =
      Arrays.asList("s", "send-data", "send data using data connect client");
  private static List<String> deviceSendDataOption =
      Arrays.asList("p", "post-data", "send data to device connected to gateway");

  private static String APP_EXE_NAME = "java -jar iot-rule-processor-<version>-all.jar";
  private static String DEFAULT_CONFIG_FILE = "package_config.ini";
  private static String APP_CONFIG_FILE_ENV_VAR = "CAF_APP_CONFIG_FILE";

  private static String getCommandlineOption(CommandLine cmdLineOptions, List<String> optionStr) {
    String resultStr = null;
    if (cmdLineOptions.hasOption(optionStr.get(0))) {
      resultStr = cmdLineOptions.getOptionValue(optionStr.get(0));
    } else if (cmdLineOptions.hasOption(optionStr.get(1))) {
      resultStr = cmdLineOptions.getOptionValue(optionStr.get(0));
    }
    return resultStr;
  }

  public static ApplicationCommandLineOptions readOptionsFromCommandLine(String[] args) {
    Options options = new Options();
    ApplicationCommandLineOptions appOptions = new ApplicationCommandLineOptions();

    options.addOption(dataOptions.get(0), dataOptions.get(1), true, dataOptions.get(2));
    options.addOption(helpOptions.get(0), helpOptions.get(1), false, helpOptions.get(2));
    options.addOption(configFileOptions.get(0), configFileOptions.get(1), false,
        configFileOptions.get(2));
    options.addOption(dataConnectClientOption.get(0), dataConnectClientOption.get(1), false,
        dataConnectClientOption.get(2));
    options.addOption(deviceSendDataOption.get(0), deviceSendDataOption.get(1), false,
        deviceSendDataOption.get(2));

    CommandLine cmdLineOptions = null;
    HelpFormatter formatter = new HelpFormatter();
    Logger logger = LoggerFactory.getLogger(CommandLineOptionParser.class);

    try {
      cmdLineOptions = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      logger.error("err='Could not parse the command line options',errMessage={}", e.getMessage());
      formatter.printHelp(APP_EXE_NAME, options);
      return null;
    }

    // If -h mentioned just ignore everything and print help.
    if (cmdLineOptions.hasOption(helpOptions.get(0))) {
      formatter.printHelp(APP_EXE_NAME, options);
      return null;
    }

    String configFile = getCommandlineOption(cmdLineOptions, configFileOptions);
    if (configFile == null || configFile.isEmpty()) {
      logger.info("Obtaining config file setting from env variable : [{}]", APP_CONFIG_FILE_ENV_VAR);
      configFile = System.getenv(APP_CONFIG_FILE_ENV_VAR);
      if (configFile == null || configFile.isEmpty()) {
        logger.info("Setting to default config file");
        configFile = DEFAULT_CONFIG_FILE;
      }
    }
    appOptions.setConfigFile(configFile);

    appOptions.setDataFile(getCommandlineOption(cmdLineOptions, dataOptions));

    if (cmdLineOptions.hasOption(dataConnectClientOption.get(0))) {
      appOptions.setSendDataToCloud(true);
      logger.info("Application configured to send data using data connect client.");
    }

    if (cmdLineOptions.hasOption(deviceSendDataOption.get(0))) {
      appOptions.setSendDataToDevice(true);
      logger.info("Application configured to send data using data connect client.");
    }

    return appOptions;
  }
}