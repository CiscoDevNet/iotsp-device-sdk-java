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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

import com.cisco.iot.swp.device.sdk.common.exception.IoTDeviceSDKCommonException;
import com.cisco.iot.swp.device.sdk.common.logger.CreateCustomLogger;
import com.cisco.iot.swp.device.sdk.common.model.config.DeviceAttributes;
import com.cisco.iot.swp.device.sdk.common.model.config.GatewayAttributes;
import com.cisco.iot.swp.device.sdk.common.utils.BaseConstantsUserParams;
import com.cisco.iot.swp.device.sdk.common.utils.ConfigHelper;
import com.cisco.iot.swp.device.sdk.common.utils.HelperMethods;
import com.cisco.iot.swp.dsl.utils.RuleProcessorException;
import com.cisco.iot.swp.edge.mqtt.client.ICloudConnectClient;
import com.cisco.iot.swp.edge.mqtt.client.MQTTClientEdge;
import com.cisco.iot.swp.edge.mqtt.exception.IoTEdgeDcClientException;
import com.cisco.iot.swp.edge.re.REMessage;
import com.cisco.iot.swp.edge.re.RuleEngine;
import com.cisco.it.swp.edge.utils.ConstantUtils;


/**
 * The Class RESampleApp implements the sample application for demonstrating use of Cisco's MQTT client implementation and edge rule engine.
 */
public class RESampleApp {
  
  /** The logger. */
  // initialize the logger object by providing (className.class) as parameter
  private static Logger logger = LoggerFactory.getLogger(RESampleApp.class);
  // determine whether we want to send data to mqtt broker in IoT DataConnect cloud using the mqtt
  // client
  /** The props. */
  
  private static Properties props = null;
  
  /** The http client to read data from raspberry pi. */
  private static CloseableHttpClient httpClient = null;
  
  /** The http client to post data to raspberry pi. */
  private static CloseableHttpClient httpPostClient = null;
  
  /** The http request on msg arrival. */
  private static HttpGet httpRequestOnMsgArrival = null;
  
  /** The observation get requests for different kind of sensors raspberry pi. */
  private static List<HttpGet> observationGetRequests = null;
  
  /** The dc client. */
  private static ICloudConnectClient dcClient = null;
  
  /** The rule processor. */
  private static RuleEngine ruleProcessor = null;
  
  /** The device sensors. */
  private static List<String> deviceSensors = Arrays.asList("temperature", "humidity", "pressure",
      "magnetometer", "accelerometer", "gyroscope");
  
  /** The scheduled executor service. */
  private static ScheduledExecutorService scheduledExecutorService;
  
  /** The scheduled future. */
  private static ScheduledFuture<?> scheduledFuture;
  
  /** The special message interval. */
  private static int SPECIAL_MESSAGE_INTERVAL = 30;
  
  /** The special message interval unit. */
  private static TimeUnit SPECIAL_MESSAGE_INTERVAL_UNIT = TimeUnit.SECONDS;
  
  /** The gw attributes. */
  private static GatewayAttributes gwAttributes = null;
  
  /** The device attributes. */
  private static Map<String, DeviceAttributes> deviceAttributes = null;
  
  /** The app options. */
  private static ApplicationCommandLineOptions appOptions = null;
  
  /** The special message send. */
  private static SpecialMessage specialMessageSend;


  /**
   * The main method.
   *
   * @param args the arguments
   * @throws IoTEdgeDcClientException the io T edge dc client exception
   */
  public static void main(String[] args) throws IoTEdgeDcClientException {

    deviceAttributes = new HashMap<String, DeviceAttributes>();
    appOptions = CommandLineOptionParser.readOptionsFromCommandLine(args);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        logger.info("msg='stopping app, shutting down threads', class={}",
            RESampleApp.class.getSimpleName());
        shutDownAll();
      }
    });

    try {
      // parse the initial config file, where keys are named section.keyname
      props = ConfigHelper.parseFile(new File(appOptions.getConfigFile()));
      CreateCustomLogger.changeLogger(props);
      logger = LoggerFactory.getLogger(RESampleApp.class);
    } catch (IOException e) {
      logger.error(
          "Error parsing configuration parameter and logger creation failed, errMessage={}, errStack={}",
          e.getMessage(), e);
      return;
    }

    deviceAttributes = ConfigHelper.getDeviceDetailsFromConfig(props);
    gwAttributes = ConfigHelper.getGatewayDetailsFromConfig(props);
    try {
      ruleProcessor = new RuleEngine(ConfigHelper.getRule(props));
    } catch (RuleProcessorException e) {
      logger.error("Error initializing the rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
      return;
    }

    // if -s flag has been given in the command line
    if (appOptions.isSendDataToCloud()) {
      initDCClient();
      logger.info("Done Initializing the IOTDC client");
    }
    // Subscribe to mqtt topics
    if (appOptions.isSendDataToCloud()) {
      subscribe();
    }
    specialMessageSend = new SpecialMessage(appOptions.isSendDataToCloud(), dcClient, gwAttributes,
        deviceAttributes);

    // Start Special Message Thread for KeepAlive and Diagnostic Messages
    startSpecialMessageMonitoring();

    // Start sending observations
    String dataFile = appOptions.getDataFile();
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

  /**
   * Start special message monitoring.
   */
  private static void startSpecialMessageMonitoring() {
    logger.info("Starting Special Message Monitoring Thread");
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    Runnable r = () -> {
      try {
        sendSpecialMessageUpstream();
      } catch (Exception e) {
        logger.error("errMessage={}, errStack={}", e.getMessage(), e.getStackTrace());
      }
    };

    r.run();

    logger.info("{}:{}:{}:{}", scheduledExecutorService.toString(), r, SPECIAL_MESSAGE_INTERVAL,
        SPECIAL_MESSAGE_INTERVAL_UNIT);

    scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(r, 0, SPECIAL_MESSAGE_INTERVAL,
        SPECIAL_MESSAGE_INTERVAL_UNIT);
  }

  /**
   * Send special messages like KeepAlive, Ack and Diagnostic messages upstream
   *
   * @throws Exception the exception
   */
  public static void sendSpecialMessageUpstream() throws Exception {
    specialMessageSend.sendKeepAlive();
    specialMessageSend.sendDiagnosticMessage();
    specialMessageSend.sendAckMessage();
  }

  // EFFECTS: process data from Raspberry Pi and send data to IoT DataConnect cloud if -s is
  //process data from Raspberry Pi and send data to IoT DataConnect cloud if -s is
  /**
   * Process data from received from raspberry pi if data sending to cloud is enabled
   *
   * @throws IoTEdgeDcClientException the io T edge dc client exception
   */
  
  private static void processDataFromHttpClient() throws IoTEdgeDcClientException {
    try {
      initHTTPClient();
      if (appOptions.isSendDataToDevice()) {
        initHTTPClientPost();
      }

      String pollingIntervalSecsStr = props.getProperty(BaseConstantsUserParams.APPLICATION_SECTION
          + BaseConstantsUserParams.SECTION_PARAMETER_DELIMITER
          + BaseConstantsUserParams.POLLING_INTERVAL);
      int pollingIntervalSecs = Integer.parseInt(pollingIntervalSecsStr);
      String assetName = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getTag();
      if ((assetName == null) || (assetName.isEmpty())) {
        logger.info("Data from device {} not found in the configuration (skipping)", assetName);
        return;
      }

      String gwTopicObs = gwAttributes.getGatewayObservationTopic();
      if ((gwTopicObs == null) || (gwTopicObs.isEmpty())) {
        logger.error("Could not publish the message, missing gateway obsv. topic in configuration");
        return;
      }

      while (true) {
        List<String> observations = getObservationUsingHTTPClient();
        logger.debug("Observations obtained : {} ", observations.size());
        for (String obsv : observations) {
          List<REMessage> messages = ruleProcessor.ProcessData(assetName, obsv);
          if (messages == null) {
            logger.info("No Valid actions");
            continue;
          }
          logger.debug("Valid actions  [{}] to be published", messages.size());
          for (REMessage msg : messages) {
            sendMessageUpstream(gwTopicObs, msg);
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
    } catch (RuleProcessorException | IoTDeviceSDKCommonException e) {
      logger.error("Error when processing data with rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }

  /**
   * Inits the DC client.
   *
   * @throws IoTEdgeDcClientException the io T edge dc client exception
   */
  private static void initDCClient() throws IoTEdgeDcClientException {
    MqttCallbackExtended callback = new MqttCallbackExtended() {
      @Override
      public void connectionLost(Throwable cause) {
        logger.error("Lost MQTT connection... Handle the lost connection");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.info("Message arrived on topic -> {} with payload {} ", topic, message);
        if (appOptions.isSendDataToDevice()) {
          sendDataToDevice(message);
        }
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
        // logger.info("Delivery complete for message...");
      }

      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
        logger.info("Reconnection status={}, serverURI={}", reconnect, serverURI);

        // Re subscribing when reconnect=true is important otherwise the mqtt connection is lost
        // when establishing actual connection. It is important to re subscribe here as we have set
        // autoReconnect=true and cleanSession=true. So every time it reconnects subscribe info is
        // lost.
        if (reconnect) {
          subscribe();
        }
      }
    };
    String clientId = gwAttributes.getGatewayId() + "-RESinglePublisher";  
    dcClient = new MQTTClientEdge(clientId);
    logger.info("Initializing the Mqtt client with callback...");
    dcClient.init(props, callback);
    dcClient.connect();
    while (!dcClient.isConnected()) {
      logger.info("Waiting for the connection to established...");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        logger.error("exception during mqtt client initializtion {}.", e.getMessage());
      }
    }
  }

  /**
   * Subscribe.
   */
  private static void subscribe() {
    String topicToSubscribe = deviceAttributes == null ? null
        : deviceAttributes.get(ConstantUtils.FIRST_DEVICE) == null ? null
            : deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getTopicCommand();
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

    topicToSubscribe = gwAttributes.getGatewayCommandTopic();
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

  /**
   * Inits the HTTP client to read data from Raspberry pi
   */
  private static void initHTTPClient() {
    httpClient = HttpClientBuilder.create().build();
    observationGetRequests = new ArrayList<HttpGet>();
    String ip = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getIp();
    String port = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getPort();
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

  /**
   * Gets the observation using HTTP client.
   *
   * @return the observation using HTTP client
   */
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

  /**
   * Inits the HTTP client to post data to device
   */
  private static void initHTTPClientPost() {
    httpPostClient = HttpClientBuilder.create().build();
    String ip = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getIp();
    String port = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getPort();
    String url = "http://" + ip + ":" + port + "/sensehat/ping";
    logger.info("Http Post to sensehat display at URL {}", url);
    RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
    httpRequestOnMsgArrival = new HttpGet(url);
    httpRequestOnMsgArrival.setConfig(requestConfig);
  }

  /**
   * Send data to device.
   *
   * @param message the message
   */
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

  /**
   * Process data from file.
   *
   * @param dataFile the data file
   * @throws IoTEdgeDcClientException the io T edge dc client exception
   */
  // EFFECTS: process data from local file
  private static void processDataFromFile(String dataFile) throws IoTEdgeDcClientException {
    try {
      logger.info("Using the datafile {}", dataFile);
      List<String> observations = null;
      observations = Files.readAllLines(Paths.get(dataFile));
      if (observations.isEmpty()) {
        logger.info("No lines to process againt the rule processor");
        return;
      }

      while (true) {
        for (String observation : observations) {
          logger.debug("Observation {}", observation);
          String[] parts = observation.split(";");
          String assetName = parts[0];
          String sleep = parts[1];
          String payload = parts[2];
          if ((assetName == null) || (assetName.isEmpty())) {
            logger.info("Data from device {} not found in the configuration (skipping)", assetName);
            return;
          }
          String gwTopicObs = gwAttributes.getGatewayObservationTopic();
          if ((gwTopicObs == null) || (gwTopicObs.isEmpty())) {
            logger.error(
                "Could not publish the message, missing gateway obsv. topic in configuration");
            return;
          }

          List<REMessage> messages = ruleProcessor.ProcessData(assetName, payload);
          if (messages == null) {
            continue;
          }

          for (REMessage msg : messages) {
            sendMessageUpstream(gwTopicObs, msg);
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
    } catch (IoTDeviceSDKCommonException e) {
      logger.error("err='Error processing data against rule',errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }

  private static void sendMessageUpstream(String gwTopicObs, REMessage msg)
      throws IoTDeviceSDKCommonException {
    // Important step.
    // Adding label to gateway topic
    String gwId = gwAttributes.getGatewayId();
    String deviceId = deviceAttributes == null ? gwId : deviceAttributes.get("1") == null ? gwId
        : deviceAttributes.get("1").getId() == null ? gwId : deviceAttributes.get("1").getId();
    String topicToPublish = null;
    if ((deviceAttributes != null) && (deviceAttributes.get("1") != null)
        && (deviceAttributes.get("1").getTopicObservation() != null)
        && !(deviceAttributes.get("1").getTopicObservation().isEmpty())) {
      topicToPublish = deviceAttributes.get("1").getTopicObservation() + msg.getTopic();
    } else {
      topicToPublish = gwTopicObs + msg.getTopic();
    }
    String payload = HelperMethods.getPublishPayload(msg.getPayload(), gwAttributes.isUseEnvelop(),
        topicToPublish, msg.getTopic(), deviceId, 0, false);
    topicToPublish =
        HelperMethods.getPublishTopic(topicToPublish, gwAttributes.isUseEnvelop(), false);

    if (dcClient != null) {
      logger.info("Publishing RE Message : {} -> {} ", payload, topicToPublish);
      try {
        synchronized (dcClient) {
          dcClient.publish(topicToPublish, payload);
        }
      } catch (IoTEdgeDcClientException e) {
        logger.error("err='Error publishing data',errMessage={},errStack={}", e.getMessage(), e);
      }
    } else {
      logger.info("RE Message : {} -> {} ", msg.getPayload(), topicToPublish);
    }
  }


  /**
   * Shuts down the application.
   */
  private static void shutDownAll() {
    logger.info("msg='Shutting down Special Message Thread'");
    if (scheduledExecutorService != null) {
      scheduledExecutorService.shutdown();
    }
  }

  /**
   * Instantiates a new Rule Engine sample app.
   */
  private RESampleApp() {

  }
}
