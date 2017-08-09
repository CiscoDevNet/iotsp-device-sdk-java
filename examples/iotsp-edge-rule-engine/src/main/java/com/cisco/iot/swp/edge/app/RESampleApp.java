/**
 * Copyright (c) 2017 by Cisco Systems, Inc. All Rights Reserved Cisco Systems Confidential
 */
package com.cisco.iot.swp.edge.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.iot.swp.dsl.utils.RuleProcessorException;
import com.cisco.iot.swp.edge.logger.CreateCustomLogger;
import com.cisco.iot.swp.edge.mqtt.exception.IoTEdgeDcClientException;
import com.cisco.iot.swp.edge.re.REMessage;
import com.cisco.iot.swp.edge.re.RuleEngine;
import com.cisco.iot.swp.edge.re.RuleEngineWithDCClient;
import com.cisco.iot.swp.edge.utils.ConfigHelper;
import com.cisco.iot.swp.mqtt.client.ICloudConnectClient;
import com.cisco.iot.swp.mqtt.client.MQTTClientEdge;

public class RESampleApp {
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
  
  // initialize the syntax for this application
  private static String APP_EXE_NAME = "java -jar iot-rule-processor-<version>-all.jar";
  // initialize the logger object by providing (className.class) as parameter
  private static Logger logger = LoggerFactory.getLogger(RESampleApp.class);
  // initialize the default config file
  private static String DEFAULT_CONFIG_FILE = "package_config.ini";

  // determine whether we want to send data to mqtt broker in IoT DataConnect cloud using the mqtt client
  private static boolean sendDataUsingDCClient = false;
  // determine whether we want to post data received from IoT DataConnect cloud to Raspberry Pi
  private static boolean sendDataToDeviceUsingHttpClient = false;

  private static Properties props = null;
  private static CloseableHttpClient httpClient = null;
  private static CloseableHttpClient httpPostClient = null;
  private static HttpGet httpRequestOnMsgArrival = null;
  private static List<HttpGet> observationGetRequests = null;

  private static ICloudConnectClient dcClient = null;
  private static RuleEngine ruleProcessor = null;

  // Need to be fixed to be read from a file or configuration specific to application.
  private static final String iotdcAttributesSection = "_cisco_mqtt_attributes";
  private static final String applicationSection = "application";
  private static final String sampleAppDatPollingInterval = "polling.interval";
  private static final String sampleDeviceTagId = "device1.tag";
  private static final String sampleDeviceIPId = "device1.ip";
  private static final String sampleDevicePortId = "device1.port";
  private static final String sampleDeviceCommadTopicId = "device1.topic.command";
  private static List<String> deviceSensors = Arrays.asList("temperature", "humidity", "pressure",
      "magnetometer", "accelerometer", "gyroscope");


  public static void main(String[] args) {

    Options options = new Options();
    // initialize command line options in the form of (a single char flag, a string flag, whether this option needs argument, a detailed description)
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
    
    // parse the command line and store the result in the cmdLineOptions
    try {
      cmdLineOptions = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      logger = LoggerFactory.getLogger(RESampleApp.class);
      logger.error("err='Could not parse the command line options',errMessage={}", e.getMessage());
      formatter.printHelp(APP_EXE_NAME, options);
      return;
    }

    // process -h
    if (cmdLineOptions.hasOption(helpOptions.get(0))) {
      formatter.printHelp(APP_EXE_NAME, options);
      return;
    }

    // process -c
    String configFile = getCommandlineOption(cmdLineOptions, configFileOptions);
    if (configFile == null || configFile.isEmpty()) {
      configFile = DEFAULT_CONFIG_FILE;
    }

    try {
      // parse the initial config file, where keys are named section.keyname
      props = ConfigHelper.parseFile(new File(configFile));
      CreateCustomLogger.changeLogger(props);
      logger = LoggerFactory.getLogger(RESampleApp.class);
    } catch (IOException e) {
      logger.error(
          "Error parsing configuration parameter and logger creation failed, errMessage={}, errStack={}",
          e.getMessage(), e);
      return;
    }
   
    // process -s
    if (cmdLineOptions.hasOption(dataConnectClientOption.get(0))) {
      sendDataUsingDCClient = true;
      logger.info("Application configured to send data using data connect client.");
    }

    try {
      // instantiate the rule engine object
      ruleProcessor = new RuleEngine(configFile);
    } catch (RuleProcessorException e) {
      logger.error("Error initializing the rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
      return;
    }
   
    // process -p

    if (cmdLineOptions.hasOption(deviceSendDataOption.get(0))) {
      sendDataToDeviceUsingHttpClient = true;
      logger.info("Application configured to post data received from IoT DataConnect cloud to Raspberry Pi.");
    }

    // process -d
    if (cmdLineOptions.hasOption(dataConnectClientOption.get(0))) {
      sendDataUsingDCClient = true;
      logger.info("Application configured to send data using data connect client.");
    }

    if (cmdLineOptions.hasOption(deviceSendDataOption.get(0))) {
      sendDataToDeviceUsingHttpClient = true;
      logger.info("Application configured to send data using data connect client.");
    }

    try {
      ruleProcessor = new RuleEngine(configFile);
    } catch (RuleProcessorException e) {
      logger.error("Error initializing the rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
      return;
    }

    String dataFile = getCommandlineOption(cmdLineOptions, dataOptions);
    // if the data comes from a local file 
    if (dataFile != null && !dataFile.isEmpty()) {
      logger.info("Start processing the data obtained from file....");
      try {
        processDataFromFile(dataFile);
      } catch (IoTEdgeDcClientException e) {
        logger.error(" Error processing data exiting errMessage={}, errStack={}", e.getMessage(),
            e.getStackTrace());
      }
    } else {
      // if the data comes from Raspberry Pi
      logger.info("Start processing the data obtained using HTTP client....");
      try {
        processDataFromHttpClient();
      } catch (IoTEdgeDcClientException e) {
        logger.error(" Error processing data exiting errMessage={}, errStack={}", e.getMessage(),
            e.getStackTrace());
      }
    }
  }

  // EFFECTS: process data from Raspberry Pi and send data to IoT DataConnect cloud if -s is provided
  private static void processDataFromHttpClient() throws IoTEdgeDcClientException {
    try {
      initHTTPClient();
      if (sendDataToDeviceUsingHttpClient) {
      	initHTTPClientPost();
      }
      // if -s flag has been given in the command line
      if (sendDataUsingDCClient) {
        initDCClient();
      }// end, if
      logger.info("Done Initializing the IOTDC client");

      String pollingIntervalSecsStr =
          props.getProperty(applicationSection + "." + sampleAppDatPollingInterval);
      int pollingIntervalSecs = Integer.parseInt(pollingIntervalSecsStr);
      String assetName = props.getProperty(iotdcAttributesSection + "." + sampleDeviceTagId);
      while (true) {
        List<String> observations = getObservationUsingHTTPClient();
        logger.debug("Observations obtained : {} ", observations.size());
        for (String obsv : observations) {
          try {
            List<REMessage> messages = ruleProcessor.ProcessData(assetName, obsv);
            if (messages == null) {
              logger.info("No Valid actions");
              continue;
            }
            logger.debug("Valid actions  [{}] to be published", messages.size());
            for (REMessage msg : messages) {
              logger.debug("Valid action to be published {} {} : {}", msg.getPayload(),
                  msg.getTopic(), dcClient);
              if (sendDataUsingDCClient) {
                dcClient.publish(msg.getTopic(), msg.getPayload());
              } else {
                logger.info("RE Message : {} -> {} ", msg.getPayload(), msg.getTopic());
              }
              logger.debug("Done publishing the actions.");
            }
          } catch (IoTEdgeDcClientException e) {
            logger.error("Error publishing data:  errMessage={},errStack={}", e.getMessage(), e);
          }
        }
        logger.debug("Done processing the observations...Sleep={}", pollingIntervalSecs);
        try {
          Thread.sleep(pollingIntervalSecs * 1000);
        } catch (InterruptedException e) {
          logger.error("Encountered interrupted exception: errMessage={},errStack={}",
              e.getMessage(), e);
        }
        logger.debug("msg='Restarting the cycle.....'");
      }
    } catch (RuleProcessorException e) {
      logger.error("Error when processing data with rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }
  
  private static void initDCClient() throws IoTEdgeDcClientException {
    MqttCallbackExtended callback = new MqttCallbackExtended() {
      @Override
      public void connectionLost(Throwable cause) {
        logger.error("Lost MQTT connection... Handle the lost connection");
      }
      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.info("Message arrived on topic -> {} with payload {} ", topic, message);
        if (sendDataToDeviceUsingHttpClient) {
          sendDataToDevice(message);
        }
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
        // logger.info("Delivery complete for message...");
      }

      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
        logger.debug("Reconnection status={}, serverURI={}", reconnect, serverURI);
        subscribe();
      }
    };
    dcClient = new MQTTClientEdge();
    logger.info("Initializing the Mqtt client with callback...");
    dcClient.init(props, callback);
    dcClient.connect();
  }

  private static void subscribe() {
    String topicToSubscribe =
        props.getProperty(iotdcAttributesSection + "." + sampleDeviceCommadTopicId);
    if (topicToSubscribe != null && !topicToSubscribe.isEmpty()) {
      logger.info("Subscribing to topic : {}", topicToSubscribe);
      try {
        dcClient.subscribe(topicToSubscribe);
        logger.debug("Subscription to topic was successful on connect: {}", topicToSubscribe);
      } catch (IoTEdgeDcClientException e) {
        logger.error("Subscription to topic was unsuccessful on connect: {}", topicToSubscribe);
        logger.error("errMessage={}, errStack={}", e.getMessage(), e.getStackTrace());
      }
    }
  }

  // EFFECTS: initialize http client and request URIs
  private static void initHTTPClient() {
    httpClient = HttpClientBuilder.create().build();
    observationGetRequests = new ArrayList<HttpGet>();
    String ip = props.getProperty(applicationSection + "." + sampleDeviceIPId);
    String port = props.getProperty(applicationSection + "." + sampleDevicePortId);
    RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
    for (String sensor : deviceSensors) {
      String url = "http://" + ip + ":" + port + "/sensehat/" + sensor;
      HttpGet httpGet = new HttpGet(url);
      httpGet.setConfig(requestConfig);
      observationGetRequests.add(httpGet);
    }
    logger.info("Done Intializing HTTP client and request URIs");
  }

  // EFFECTS: obtaining the messages from the Rpi
  private static List<String> getObservationUsingHTTPClient() {
    logger.debug("Obtaining the messages from the Rpi");
    List<String> observations = new ArrayList<String>();
    for (HttpGet httpGet : observationGetRequests) {
      CloseableHttpResponse response = null;
      try {
        response = httpClient.execute(httpGet);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          HttpEntity httpEntity = response.getEntity();
          String obsv = EntityUtils.toString(httpEntity);
          obsv = obsv.replace("\n", "");
          observations.add(obsv);
          logger.debug("Http response {} : {} : ", response.getStatusLine(), obsv);
        } else {
          logger.error(
              "Method failed status code from http response is: " + response.getStatusLine());
        }
      } catch (IOException e) {
        logger.error("Failed to process HTTP request due to : {} " + e.getMessage());
      } finally {
        if (response != null) {
          try {
            response.close();
          } catch (IOException e) {
            logger.error("Failed to close HTTP response due to : {} " + e.getMessage());
          }
        }
      }
    }
    return observations;
  }
  
  private static void initHTTPClientPost() {
    httpPostClient = HttpClientBuilder.create().build();
    String ip = props.getProperty(applicationSection + "." + sampleDeviceIPId);
    String port = props.getProperty(applicationSection + "." + sampleDevicePortId);
    String url = "http://" + ip + ":" + port + "/sensehat/ping";
    logger.info("Http Post to sensehat display at URL {}", url);
    RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
    httpRequestOnMsgArrival = new HttpGet(url);
    httpRequestOnMsgArrival.setConfig(requestConfig);
  }

  private static void sendDataToDevice(MqttMessage message) {
    CloseableHttpResponse response = null;
    try {
      response = httpPostClient.execute(httpRequestOnMsgArrival);
      logger.info("msg='Message posted to sensehat display with error code ={}",
          response.getStatusLine().getStatusCode());
    } catch (IOException e) {
      logger.trace("Http post operation failed due to : {}", e.getMessage());
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          logger.error("Http post close operation failed due to :{}, {}", e.getMessage(), e);
        }
      }
    }
  }

//EFFECTS: process data from local file
  private static void processDataFromFile(String dataFile) throws IoTEdgeDcClientException {
    try {
      logger.info("Using the datafile {}", dataFile);
      List<String> observations = null;
      observations = Files.readAllLines(Paths.get(dataFile));
      if (observations.isEmpty()) {
        logger.info("No lines to process againt the rule processor");
        return;
      }

      if (sendDataUsingDCClient) {
        initDCClient();
      }
      while (true) {
        for (String observation : observations) {
          logger.debug("Observation {}", observation);
          String[] parts = observation.split(";");
          String assetName = parts[0];
          String sleep = parts[1];
          String payload = parts[2];
          List<REMessage> messages = ruleProcessor.ProcessData(assetName, payload);
          if (messages == null) {
            continue;
          }
          for (REMessage msg : messages) {
            if (dcClient != null) {
              logger.info("Publishing RE Message : {} -> {} ", msg.getPayload(), msg.getTopic());
              try {
                dcClient.publish(msg.getTopic(), msg.getPayload());
              } catch (IoTEdgeDcClientException e) {
                logger.error("err='Error publishing data',errMessage={},errStack={}",
                    e.getMessage(), e);
              }
            } else {
              logger.info("RE Message : {} -> {} ", msg.getPayload(), msg.getTopic());
            }
          }
          try {
            Thread.sleep(Integer.parseInt(sleep));
          } catch (NumberFormatException | InterruptedException e) {
            logger.error("err='Error processing data against rule',errMessage={},errStack={}",
                e.getMessage(), e);
          }
        }
      }
    } catch (RuleProcessorException e) {
      logger.error("err='Error processing data against rule',errMessage={},errStack={}",
          e.getMessage(), e);
    } catch (IOException e) {
      logger.error("err='Error processing data against rule',errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }

  // EFFECTS: return the value of the command option
  private static String getCommandlineOption(CommandLine cmdLineOptions, List<String> optionStr) {
    String resultStr = null;
    if (cmdLineOptions.hasOption(optionStr.get(0))) {
      resultStr = cmdLineOptions.getOptionValue(optionStr.get(0));
    } else if (cmdLineOptions.hasOption(optionStr.get(1))) {
      resultStr = cmdLineOptions.getOptionValue(optionStr.get(0));
    }
    return resultStr;
  }

  private static void sendProcessedDataUsingREWithDCClient(String configFile) throws IoTEdgeDcClientException {
    try {
      // Initialize HTTP client
      initHTTPClient();
      RuleEngineWithDCClient ruleProcessor = null;
      ruleProcessor = new RuleEngineWithDCClient(configFile);

      logger.info("Done Initializing the rule engine using config file : {}", configFile);
      String pollingIntervalSecsStr =
          props.getProperty(applicationSection + "." + sampleAppDatPollingInterval);
      int pollingIntervalSecs = Integer.parseInt(pollingIntervalSecsStr);
      String assetName = props.getProperty(iotdcAttributesSection + "." + sampleDeviceTagId);
      while (true) {
        List<String> observations = getObservationUsingHTTPClient();
        for (String obsv : observations) {
          try {
            ruleProcessor.ProcessAndSendDataUsingDcClient(assetName, obsv);
          } catch (Exception e) {
            logger.error("Error processing data against rule:  errMessage={},errStack={}",
                e.getMessage(), e);
          }
        }
        try {
          Thread.sleep(pollingIntervalSecs * 1000);
        } catch (InterruptedException e) {
          logger.error("Encountered interrupted exception: errMessage={},errStack={}",
              e.getMessage(), e);
        }
      }
    } catch (RuleProcessorException e) {
      logger.error("Error when processing data with rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }

  private void RESampleApp() {

  }
}