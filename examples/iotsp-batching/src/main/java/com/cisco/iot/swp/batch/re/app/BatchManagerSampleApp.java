/**
 * Copyright (c) 2017 by Cisco Systems, Inc. All Rights Reserved Cisco Systems Confidential
 */
package com.cisco.iot.swp.batch.re.app;

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
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

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

import com.cisco.iot.swp.batch.commandline.ApplicationCommandLineOptions;
import com.cisco.iot.swp.batch.commandline.CommandLineOptionParser;
import com.cisco.iot.swp.batch.controller.BatchToCloudClient;
import com.cisco.iot.swp.batch.exception.IoTBatchingException;
import com.cisco.iot.swp.batch.utils.ConstantUtils;
import com.cisco.iot.swp.device.sdk.common.compression.CompressionUtilsZlib;
import com.cisco.iot.swp.device.sdk.common.compression.ICompressionUtils;
import com.cisco.iot.swp.device.sdk.common.exception.IoTDeviceSDKCommonException;
import com.cisco.iot.swp.device.sdk.common.logger.CreateCustomLogger;
import com.cisco.iot.swp.device.sdk.common.model.config.DeviceAttributes;
import com.cisco.iot.swp.device.sdk.common.model.config.GatewayAttributes;
import com.cisco.iot.swp.device.sdk.common.model.message.type.BatchMessage;
import com.cisco.iot.swp.device.sdk.common.utils.BaseConstantsUserParams;
import com.cisco.iot.swp.device.sdk.common.utils.ConfigHelper;
import com.cisco.iot.swp.device.sdk.common.utils.HelperMethods;
import com.cisco.iot.swp.dsl.utils.RuleProcessorException;
import com.cisco.iot.swp.edge.mqtt.client.ICloudConnectClient;
import com.cisco.iot.swp.edge.mqtt.client.MQTTClientEdge;
import com.cisco.iot.swp.edge.mqtt.exception.IoTEdgeDcClientException;
import com.cisco.iot.swp.edge.mqtt.utils.BaseConstants;
import com.cisco.iot.swp.edge.re.REMessage;
import com.cisco.iot.swp.edge.re.RuleEngine;
import com.cisco.iot.swp.edge.re.RuleEngineWithDCClient;

public class BatchManagerSampleApp {

  private static Logger LOG = LoggerFactory.getLogger(BatchManagerSampleApp.class);
  private static Properties props = null;
  private static Map<String, DeviceAttributes> deviceAttributes = null;
  private static GatewayAttributes gwAttributes = null;
  private static CloseableHttpClient httpClient = null;
  private static CloseableHttpClient httpPostClient = null;
  private static HttpGet httpRequestOnMsgArrival = null;
  private static List<HttpGet> observationGetRequests = null;
  private static ICloudConnectClient dcClient = null;
  private static RuleEngine ruleProcessor = null;
  // Need to be fixed to be read from a file or configuration specific to application.
  private static List<String> deviceSensors = Arrays.asList("temperature", "humidity", "pressure",
      "magnetometer", "accelerometer", "gyroscope");
  private static BatchToCloudClient batchClient = null;
  private static ICompressionUtils compressionUtils = null;

  private static ScheduledExecutorService scheduledExecutorService;
  private static ScheduledFuture<?> scheduledFuture;
  private static ApplicationCommandLineOptions appOptions = null;
  private static SpecialMessage specialMessageSend;

  private BatchManagerSampleApp() {}

  public static void main(String[] args)
      throws IoTBatchingException, IoTEdgeDcClientException, IoTDeviceSDKCommonException {
       
    appOptions = CommandLineOptionParser.readOptionsFromCommandLine(args);
    deviceAttributes = new HashMap<String, DeviceAttributes>();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        LOG.info("msg='stopping app, shutting down threads', class={}",
            BatchManagerSampleApp.class.getSimpleName());
        shutDownAll();
      }
    });

    try {
      // Parse the ini file. keys are named section.keyname
      props = ConfigHelper.parseFile(new File(appOptions.getConfigFile()));
      // Change logger
      CreateCustomLogger.changeLogger(props);
      LOG = LoggerFactory.getLogger(BatchManagerSampleApp.class);
    } catch (IOException e) {
      LOG.error(
          "Error parsing configuration parameter and logger creation failed, errMessage={}, errStack={}",
          e.getMessage(), e);
      shutDownAll();
      return;
    }

    // Initialize rule engine
    try {
      ruleProcessor = new RuleEngine(ConfigHelper.getRule(props));
    } catch (RuleProcessorException e) {
      LOG.error("Error initializing the rule processor:  errMessage={},errStack={}", e.getMessage(),
          e);
      shutDownAll();
      return;
    }

    // Initialize compression utility
    compressionUtils = new CompressionUtilsZlib();

    // Initialize gateway and device parameters
    deviceAttributes = ConfigHelper.getDeviceDetailsFromConfig(props);
    gwAttributes = ConfigHelper.getGatewayDetailsFromConfig(props);
    // Important step = Create the batch topic and add it to properties
    props.setProperty(BaseConstantsUserParams.GATEWAY_OBV_BATCH_TOPIC,
        ConfigHelper.getBatchZipTopicForGateway(gwAttributes));

    // Initialize DC client
    if (appOptions.isSendDataToCloud()) {
      LOG.info("Going to initialize the cloud client connection....");
      initDCClient();
      LOG.info("Done Initializing the IOTDC client");
    }
    
    if (appOptions.isSendDataToCloud()) {
      subscribe();
    }

    specialMessageSend = new SpecialMessage(appOptions.isSendDataToCloud(), dcClient, gwAttributes,
        deviceAttributes);

    // Start Special Message Thread for KeepALive and Diagnostic Messages
    startSpecialMessageMonitoring();

    String dataFile = appOptions.getDataFile();
    if (dataFile != null && !dataFile.isEmpty()) {
      LOG.info("Start processing the data obtained from file....");
      try {
        processDataFromFile(dataFile);
      } catch (IoTDeviceSDKCommonException e) {
        LOG.error("Error processing data exiting errMessage={}, errStack={}", e.getMessage(),
            e.getStackTrace());
      }
    } else {
      LOG.info("Start processing the data obtained using HTTP client....");
      try {
        processDataFromHttpClient();
      } catch (IoTDeviceSDKCommonException e) {
        LOG.error("Error processing data exiting errMessage={}, errStack={}", e.getMessage(),
            e.getStackTrace());
      }
    }

  }

  private static void startSpecialMessageMonitoring() {
    LOG.info("Starting Special Message Monitoring Thread");
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    Runnable r = () -> {
      try {
        sendSpecialMessageUpstream();
      } catch (Exception e) {
        LOG.error("errMessage={}, errStack={}", e.getMessage(), e.getStackTrace());
      }
    };
    LOG.info("{}:{}:{}:{}", scheduledExecutorService.toString(), r,
        ConstantUtils.SPECIAL_MESSAGE_INTERVAL, ConstantUtils.SPECIAL_MESSAGE_INTERVAL_UNIT);
    int initialDelay = 5;
    scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(r, initialDelay,
        ConstantUtils.SPECIAL_MESSAGE_INTERVAL, ConstantUtils.SPECIAL_MESSAGE_INTERVAL_UNIT);
  }

  private static void sendSpecialMessageUpstream() throws Exception {
    specialMessageSend.sendKeepAlive();
    specialMessageSend.sendDiagnosticMessage();
    specialMessageSend.sendAckMessage();
  }

  private static BatchMessage constructBatchMessage(REMessage msg, String topicToPublish,
      String gwId, String deviceId) throws IoTBatchingException, IoTDeviceSDKCommonException {
    
    long timestamp = System.currentTimeMillis();
    Random rnd = new Random();
    long messageID = 100000 + rnd.nextInt(900000);

    BatchMessage obvMsg =
        new BatchMessage.BatchMessageBuilder().format("json").qos(BaseConstants.ATMOST_ONCE_QOS)
            .timestamp(timestamp).messageId(messageID).deviceId(deviceId).data(msg.getPayload())
            .topic(topicToPublish).label(msg.getTopic()).build();
    if (obvMsg != null) {
      LOG.trace("Batch message has been constructed from string data {}", obvMsg);
      return obvMsg;
    } else {
      LOG.error("Batch message cannot be null, unable to construct batch message with data {}",
          msg.getPayload());
      return null;
    }
  }

  private static void initDCClient() throws IoTEdgeDcClientException {
    MqttCallbackExtended callback = new MqttCallbackExtended() {
      @Override
      public void connectionLost(Throwable cause) {
        LOG.error("Lost MQTT connection... Handle the lost connection");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        LOG.info("Message arrived on topic -> {} with payload {} ", topic, message);
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
        LOG.info("Reconnection status={}, serverURI={}", reconnect, serverURI);

        // Re subscribing when reconnect=true is important otherwise the mqtt connection is lost
        // when establishing actual connection. It is important to re subscribe here as we have set
        // autoReconnect=true and cleanSession=true. So every time it reconnects subscribe info is
        // lost.
        if (reconnect) {
          subscribe();
        }
      }
    };
    dcClient = new MQTTClientEdge();
    LOG.info("Initializing the Mqtt client with callback...");
    dcClient.init(props, callback);
    dcClient.connect();
    while (!dcClient.isConnected()) {
      LOG.info("Waiting for the connection to established...");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        LOG.error("exception during mqtt client initializtion {}.", e.getMessage());
      }
    }
  }

  private static void subscribe() {
    String topicToSubscribe = deviceAttributes == null ? null
        : deviceAttributes.get(ConstantUtils.FIRST_DEVICE) == null ? null
            : deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getTopicCommand();
    if (topicToSubscribe != null && !topicToSubscribe.isEmpty()) {
      LOG.info("Subscribing to topic : {}", topicToSubscribe);
      try {
        dcClient.subscribe(topicToSubscribe);
        LOG.debug("Subscription to topic was successful on connect: {}", topicToSubscribe);
      } catch (IoTEdgeDcClientException e) {
        LOG.error("Subscription to topic was unsuccessful on connect: {}", topicToSubscribe);
        LOG.error("errMessage={}, errStack=[{}]", e.getMessage(), e.getStackTrace());
      }
    }

    topicToSubscribe = gwAttributes.getGatewayCommandTopic();
    if (topicToSubscribe != null && !topicToSubscribe.isEmpty()) {
      LOG.info("Subscribing to topic : {}", topicToSubscribe);
      try {
        dcClient.subscribe(topicToSubscribe);
        LOG.debug("Subscription to topic was successful on connect: {}", topicToSubscribe);
      } catch (IoTEdgeDcClientException e) {
        LOG.error("Subscription to topic was unsuccessful on connect: {}", topicToSubscribe);
        LOG.error("errMessage={}, errStack=[{}]", e.getMessage(), e.getStackTrace());
      }
    }
  }

  private static List<String> getObservationUsingHTTPClient() {
    LOG.debug("Obtaining the messages from the Rpi");
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
          LOG.debug("Http response {} : {} : ", response.getStatusLine(), obsv);
        } else {
          LOG.error("Method failed status code from http response is: " + response.getStatusLine());
        }
      } catch (IOException e) {
        LOG.error("Failed to process HTTP request due to : {} " + e.getMessage());
      } finally {
        if (response != null) {
          try {
            response.close();
          } catch (IOException e) {
            LOG.error("Failed to close HTTP response due to : {} " + e.getMessage());
          }
        }
      }
    }
    return observations;
  }

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
    LOG.info("Done Intializing HTTP client and request URIs");
  }

  private static void initHTTPClientPost() {
    httpPostClient = HttpClientBuilder.create().build();
    String ip = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getIp();
    String port = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getPort();
    String url = "http://" + ip + ":" + port + "/sensehat/ping";
    LOG.info("Http Post to sensehat display at URL {}", url);
    RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
    httpRequestOnMsgArrival = new HttpGet(url);
    httpRequestOnMsgArrival.setConfig(requestConfig);
  }

  private static boolean sendDataToDevice(MqttMessage message) {
    CloseableHttpResponse response = null;
    try {
      response = httpPostClient.execute(httpRequestOnMsgArrival);
      LOG.info("msg='Message posted to sensehat display with error code ={}",
          response.getStatusLine().getStatusCode());
      return true;
    } catch (IOException e) {
      LOG.trace("Http post operation failed due to : {}", e.getMessage());
      return false;
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          LOG.error("Http post close operation failed due to :{}, {}", e.getMessage(), e);
        }
      }
    }
  }

  private static void batchOrSendMsgBasedOnRule(REMessage msg, boolean sendDataUsingDCClient)
      throws IoTBatchingException, IoTDeviceSDKCommonException {
    String topicToPublish = gwAttributes.getGatewayObservationTopic() + msg.getTopic();

    String gwId = gwAttributes.getGatewayId();
    String deviceId = deviceAttributes == null ? gwId
        : deviceAttributes.get("1") == null ? gwId
            : deviceAttributes.get("1").getId() == null ? gwId : deviceAttributes.get("1").getId();

    if (msg.getAsBatch()) {
      // add message to batch; batch client background thread will send when ready
      BatchMessage batchMessage = constructBatchMessage(msg, topicToPublish, gwId, deviceId);
      if (batchClient.putMessageInBatch(batchMessage)) {
        LOG.debug("msg='Message successfully added to batch {}'", msg.toString());
      } else {
        LOG.debug("msg='Message not added to batch {}'", msg.toString());
      }
    } else {
      String payload = HelperMethods.getPublishPayload(msg.getPayload(),
          gwAttributes.isUseEnvelop(), topicToPublish, msg.getTopic(), deviceId, 0, false);
      topicToPublish = HelperMethods.getPublishTopic(topicToPublish, gwAttributes.isUseEnvelop(),
          msg.getAsBatch());

      if (dcClient != null) {
        LOG.info("Publishing RE Message : {} -> {} ", payload, topicToPublish);
        try {
          synchronized (dcClient) {
            dcClient.publish(topicToPublish, payload);
          }
        } catch (IoTEdgeDcClientException e) {
          LOG.error("err='Error publishing data',errMessage={},errStack={}", e.getMessage(), e);
        }
      } else {
        LOG.info("RE Message : {} -> {} ", msg.getPayload(), topicToPublish);
      }
    }
  }

  private static void processDataFromHttpClient()
      throws IoTEdgeDcClientException, IoTBatchingException, IoTDeviceSDKCommonException {
    try {
      initHTTPClient();
      initHTTPClientPost();

      batchClient = new BatchToCloudClient(props, compressionUtils, dcClient);
      LOG.info("Done Initializing the IOTDC client");

      String pollingIntervalSecsStr = props.getProperty(BaseConstantsUserParams.APPLICATION_SECTION
          + BaseConstantsUserParams.SECTION_PARAMETER_DELIMITER
          + BaseConstantsUserParams.POLLING_INTERVAL);
      int pollingIntervalSecs = Integer.parseInt(pollingIntervalSecsStr);
      String assetName = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getTag();
      while (true) {
        List<String> observations = getObservationUsingHTTPClient();
        LOG.debug("Observations obtained : {} ", observations.size());
        for (String obsv : observations) {
          try {
            List<REMessage> messages = ruleProcessor.ProcessData(assetName, obsv);
            if (messages == null) {
              LOG.info("No Valid actions");
              continue;
            }
            LOG.debug("Valid actions  [{}] to be published", messages.size());
            for (REMessage msg : messages) {
              LOG.debug("Valid action to be published {} {} : {}", msg.getPayload(), msg.getTopic(),
                  dcClient);
              batchOrSendMsgBasedOnRule(msg, appOptions.isSendDataToCloud());
              LOG.debug("Done batching the actions for publishing.");
            }
          } catch (IoTBatchingException e) {
            LOG.error("Error adding message to batch:  errMessage={},errStack={}", e.getMessage(),
                e);
          }
        }

        LOG.debug("Done processing the observations...Sleep={}", pollingIntervalSecs);
        try {
          Thread.sleep(pollingIntervalSecs * 1000);
        } catch (InterruptedException e) {
          LOG.error("Encountered interrupted exception: errMessage={},errStack={}", e.getMessage(),
              e);
        }
        LOG.debug("msg='Restarting the cycle.....'");
      }
    } catch (RuleProcessorException e) {
      LOG.error("Error when processing data with rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }

  private static void processDataFromFile(String dataFile)
      throws IoTEdgeDcClientException, IoTBatchingException, IoTDeviceSDKCommonException {
    if (dataFile == null || dataFile.isEmpty()) {
      LOG.info("Not a valid data file {} ", dataFile);
      shutDownAll();
      return;
    }
    try {
      LOG.info("Using the datafile {}", dataFile);
      List<String> observations = null;
      observations = Files.readAllLines(Paths.get(dataFile));
      if (observations.isEmpty()) {
        LOG.info("No lines to process againt the rule processor");
        shutDownAll();
        return;
      }

      batchClient = new BatchToCloudClient(props, compressionUtils, dcClient);
      while (true) {
        for (String observation : observations) {
          LOG.debug("Observation {}", observation);
          String[] parts = observation.split(";");
          String assetName = parts[0];
          String sleep = parts[1];
          String payload = parts[2];
          List<REMessage> messages = ruleProcessor.ProcessData(assetName, payload);
          if (messages == null) {
            continue;
          }
          for (REMessage msg : messages) {
            LOG.info("Publishing RE Message : {} with tag {} ", msg.getPayload(), msg.getTopic());
            batchOrSendMsgBasedOnRule(msg, appOptions.isSendDataToCloud());
          }
          try {
            Thread.sleep(Integer.parseInt(sleep));
          } catch (NumberFormatException | InterruptedException e) {
            LOG.error("err='Error processing data against rule',errMessage={},errStack={}",
                e.getMessage(), e);
          }
        }
      }
    } catch (RuleProcessorException e) {
      LOG.error("err='Error processing data against rule',errMessage={},errStack={}",
          e.getMessage(), e);
    } catch (IOException e) {
      LOG.error("err='Error processing data against rule',errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }

  private static void sendProcessedDataUsingREWithDCClient(String configFile)
      throws IoTEdgeDcClientException {
    try {
      // Initialize HTTP client
      initHTTPClient();
      RuleEngineWithDCClient ruleProcessor = null;
      ruleProcessor = new RuleEngineWithDCClient(props);

      LOG.info("Done Initializing the rule engine using config file : {}", configFile);
      String pollingIntervalSecsStr = props.getProperty(BaseConstantsUserParams.APPLICATION_SECTION
          + BaseConstantsUserParams.SECTION_PARAMETER_DELIMITER
          + BaseConstantsUserParams.POLLING_INTERVAL);
      int pollingIntervalSecs = Integer.parseInt(pollingIntervalSecsStr);
      String assetName = deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getTag();
      while (true) {
        List<String> observations = getObservationUsingHTTPClient();
        for (String obsv : observations) {
          try {
            ruleProcessor.ProcessAndSendDataUsingDcClient(assetName, obsv,
                gwAttributes.getGatewayObservationTopic());
          } catch (Exception e) {
            LOG.error("Error processing data against rule:  errMessage={},errStack={}",
                e.getMessage(), e);
          }
        }
        try {
          Thread.sleep(pollingIntervalSecs * 1000);
        } catch (InterruptedException e) {
          LOG.error("Encountered interrupted exception: errMessage={},errStack={}", e.getMessage(),
              e);
        }
      }
    } catch (RuleProcessorException e) {
      LOG.error("Error when processing data with rule processor:  errMessage={},errStack={}",
          e.getMessage(), e);
    }
  }

  private static void shutDownAll() {
    // Intentionally logging to stdout.
    if (batchClient != null) {
      System.out.println("msg='Shutting down Batch Client Thread'");
      batchClient.closeBatchCheckThread();
      System.out.println("msg='Closed Batch Client Thread'");
    }

    if (dcClient != null) {
      try {
        System.out.println("Unsubscribing DC Client...");
        dcClient.unsubscribe();
        System.out.println("Disconnecting the DC client...");
        dcClient.disconnect();
        System.out.println("Closing the DC client");
        dcClient.close();
      } catch (IoTEdgeDcClientException e) {
        System.out.println("Failed to close the DC client");
      }
    }
    System.out.println("Shutting down the application");
  }

}
