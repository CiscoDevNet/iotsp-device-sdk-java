package com.cisco.iot.swp.batch.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

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

import com.cisco.iot.swp.batch.controller.BatchToCloudClient;
import com.cisco.iot.swp.batch.exception.IoTBatchingErrorCode;
import com.cisco.iot.swp.batch.exception.IoTBatchingException;
import com.cisco.iot.swp.batch.model.BatchMessage;
import com.cisco.iot.swp.batch.utils.ConstantUtils;
import com.cisco.iot.swp.compression.CompressionUtilsZlib;
import com.cisco.iot.swp.compression.ICompressionUtils;
import com.cisco.iot.swp.dsl.utils.RuleProcessorException;
import com.cisco.iot.swp.edge.logger.CreateCustomLogger;
import com.cisco.iot.swp.edge.mqtt.exception.IoTEdgeDcClientException;
import com.cisco.iot.swp.edge.re.REMessage;
import com.cisco.iot.swp.edge.re.RuleEngine;
import com.cisco.iot.swp.edge.re.RuleEngineWithDCClient;
import com.cisco.iot.swp.edge.utils.ConfigHelper;
import com.cisco.iot.swp.mqtt.client.MQTTClientEdge;

public class BatchManagerSampleApp {

  private static Logger LOG = LoggerFactory.getLogger(BatchManagerSampleApp.class);
  private static Properties props = null;
  private static CloseableHttpClient httpClient = null;
  private static CloseableHttpClient httpPostClient = null;
  private static HttpGet httpRequestOnMsgArrival = null;
  private static List<HttpGet> observationGetRequests = null;
  private static boolean sendDataToDeviceUsingHttpClient = false;
  private static MQTTClientEdge dcClient = null;
  private static RuleEngine ruleProcessor = null;
  // Need to be fixed to be read from a file or configuration specific to application.
  private static List<String> deviceSensors = Arrays.asList("temperature", "humidity", "pressure",
      "magnetometer", "accelerometer", "gyroscope");
  private static BatchToCloudClient batchClient = null;
  private static ICompressionUtils compressionUtils = null;

  private BatchManagerSampleApp() {}

  public static void main(String[] args) throws IoTBatchingException {
    BatchManagerAppOptions appOptions = OptionsParser.readOptionsFromCommandLine(args);

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
      ruleProcessor = new RuleEngine(appOptions.getConfigFile());
    } catch (RuleProcessorException e) {
      LOG.error("Error initializing the rule processor:  errMessage={},errStack={}", e.getMessage(),
          e);
      shutDownAll();
      return;
    }
    compressionUtils = new CompressionUtilsZlib();
    props.setProperty(ConstantUtils.BATCH_TOPIC_PUBLISH, createGatewayBatchTopic());

    String dataFile = appOptions.getDataFile();
    if (dataFile != null && !dataFile.isEmpty()) {
      LOG.info("Start processing the data obtained from file....");
      try {
        processDataFromFile(dataFile, appOptions.isSendDataDcClient());
      } catch (IoTEdgeDcClientException e) {
        LOG.error("Error processing data exiting errMessage={}, errStack={}", e.getMessage(),
            e.getStackTrace());
      }
    } else {
      LOG.info("Start processing the data obtained using HTTP client....");
      try {
        processDataFromHttpClient(appOptions.isSendDataDcClient());
      } catch (IoTEdgeDcClientException e) {
        LOG.error("Error processing data exiting errMessage={}, errStack={}", e.getMessage(),
            e.getStackTrace());
      }
    }

  }

  private static String createGatewayBatchTopic() throws IoTBatchingException {
    /// v1/2/json/dev2app/
    String gwTopic = props
        .getProperty(ConstantUtils.IOTDC_ATTRIBUTES_SECTION + ConstantUtils.PACKAGE_CONFIG_DELIMITER
            + ConstantUtils.SAMPLE_GATEWAY_OBSERVATION_TOPIC_ID);
    if (gwTopic == null || gwTopic.isEmpty()) {
      throw new IoTBatchingException(IoTBatchingErrorCode.GW_TOPIC_OBSERVATION_MISSING, gwTopic);
    }
    String[] split = gwTopic.split("/");
    String batchTopic = ConstantUtils.TOPIC_DELIMITER + split[1] + ConstantUtils.TOPIC_DELIMITER
        + split[2] + ConstantUtils.TOPIC_DELIMITER + ConstantUtils.TOPIC_MESSAGE_FORMAT
        + ConstantUtils.TOPIC_DELIMITER + split[4];
    LOG.debug("msg='Batch topic publish={}'", batchTopic);
    return batchTopic;
  }

  private static BatchMessage constructBatchMessage(REMessage msg) throws IoTBatchingException {
    long timestamp = System.currentTimeMillis();
    Random rnd = new Random();
    long messageID = 100000 + rnd.nextInt(900000);
    // TODO - correct the message fields below (should be reading from package_config)
    BatchMessage batchMessage = new BatchMessage.BatchMessageBuilder().format("json").qos(0)
        .timestamp(timestamp).messageId(messageID).deviceId("6047").data(msg.getPayload())
        .label(msg.getTopic()).build();
    if (batchMessage != null) {
      LOG.trace("Batch message has been constructed from string data {}", msg.getPayload());
      return batchMessage;
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
        LOG.debug("Reconnection status={}, serverURI={}", reconnect, serverURI);
        subscribe();
      }
    };
    dcClient = new MQTTClientEdge();
    LOG.info("Initializing the Mqtt client with callback...");
    dcClient.init(props, callback);
    dcClient.connect();
  }

  private static void subscribe() {
    String topicToSubscribe = props.getProperty(ConstantUtils.IOTDC_ATTRIBUTES_SECTION
        + ConstantUtils.PACKAGE_CONFIG_DELIMITER + ConstantUtils.SAMPLE_DEVICE_COMMAND_TOPIC_ID);
    if (topicToSubscribe != null && !topicToSubscribe.isEmpty()) {
      LOG.info("Subscribing to topic : {}", topicToSubscribe);
      try {
        dcClient.subscribe(topicToSubscribe);
        LOG.debug("Subscription to topic was successful on connect: {}", topicToSubscribe);
      } catch (IoTEdgeDcClientException e) {
        LOG.error("Subscription to topic was unsuccessful on connect: {}", topicToSubscribe);
        LOG.error("errMessage={}, errStack={}", e.getMessage(), e.getStackTrace());
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
    String ip = props.getProperty(ConstantUtils.APPLICATION_ATTRIBUTES_SECTION
        + ConstantUtils.PACKAGE_CONFIG_DELIMITER + ConstantUtils.SAMPLE_DEVICE_IP_ID);
    String port = props.getProperty(ConstantUtils.APPLICATION_ATTRIBUTES_SECTION
        + ConstantUtils.PACKAGE_CONFIG_DELIMITER + ConstantUtils.SAMPLE_DEVICE_PORT_ID);
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
    String ip = props.getProperty(ConstantUtils.APPLICATION_ATTRIBUTES_SECTION
        + ConstantUtils.PACKAGE_CONFIG_DELIMITER + ConstantUtils.SAMPLE_DEVICE_IP_ID);
    String port = props.getProperty(ConstantUtils.APPLICATION_ATTRIBUTES_SECTION
        + ConstantUtils.PACKAGE_CONFIG_DELIMITER + ConstantUtils.SAMPLE_DEVICE_PORT_ID);
    String url = "http://" + ip + ":" + port + "/sensehat/ping";
    LOG.info("Http Post to sensehat display at URL {}", url);
    RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).build();
    httpRequestOnMsgArrival = new HttpGet(url);
    httpRequestOnMsgArrival.setConfig(requestConfig);
  }

  private static void sendDataToDevice(MqttMessage message) {
    CloseableHttpResponse response = null;
    try {
      response = httpPostClient.execute(httpRequestOnMsgArrival);
      LOG.info("msg='Message posted to sensehat display with error code ={}",
          response.getStatusLine().getStatusCode());
    } catch (IOException e) {
      LOG.trace("Http post operation failed due to : {}", e.getMessage());
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
      throws IoTBatchingException {
    if (msg.getAsBatch()) {
      // add message to batch; batch client background thread will send when ready
      if (batchClient.putMessageInBatch(constructBatchMessage(msg))) {
        LOG.debug("msg='Message successfully added to batch {}'", msg.toString());
      } else {
        LOG.debug("msg='Message not added to batch {}'", msg.toString());
      }
    } else {
      if (sendDataUsingDCClient) {
        // Send immediately to cloud
        try {
          dcClient.publish(msg.getTopic(), msg.getPayload());
        } catch (IoTEdgeDcClientException e) {
          LOG.error(
              "err='Could not send message to cloud. Adding to batch.', erMessage={}, errStack={}",
              e.getMessage(), e.getStackTrace());
          LOG.trace("msg='Message that could not be sent to cloud={}'", msg.toString());
          batchClient.putMessageInBatch(constructBatchMessage(msg));
        }
      } else {
        LOG.debug("msg=Message published={} on topic {}'", msg.getPayload(), msg.getTopic());
      }
    }
  }

  private static void processDataFromHttpClient(boolean sendDataUsingDCClient)
      throws IoTEdgeDcClientException, IoTBatchingException {
    try {
      initHTTPClient();
      initHTTPClientPost();
      if (sendDataUsingDCClient) {
        initDCClient();
      }
      batchClient = new BatchToCloudClient(props, compressionUtils, dcClient);
      LOG.info("Done Initializing the IOTDC client");

      String pollingIntervalSecsStr = props.getProperty(
          ConstantUtils.APPLICATION_ATTRIBUTES_SECTION + ConstantUtils.PACKAGE_CONFIG_DELIMITER
              + ConstantUtils.SAMPLE_APP_DATA_POLLING_INTERVAL);
      int pollingIntervalSecs = Integer.parseInt(pollingIntervalSecsStr);
      String assetName = props.getProperty(ConstantUtils.IOTDC_ATTRIBUTES_SECTION
          + ConstantUtils.PACKAGE_CONFIG_DELIMITER + ConstantUtils.SAMPLE_DEVICE_TAG_ID);
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
              batchOrSendMsgBasedOnRule(msg, sendDataUsingDCClient);
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

  private static void processDataFromFile(String dataFile, boolean sendDataUsingDCClient)
      throws IoTEdgeDcClientException, IoTBatchingException {
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

      if (sendDataUsingDCClient) {
        initDCClient();
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
            LOG.info("Publishing RE Message : {} -> {} ", msg.getPayload(), msg.getTopic());
            batchOrSendMsgBasedOnRule(msg, sendDataUsingDCClient);
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
      ruleProcessor = new RuleEngineWithDCClient(configFile);

      LOG.info("Done Initializing the rule engine using config file : {}", configFile);
      String pollingIntervalSecsStr = props.getProperty(
          ConstantUtils.APPLICATION_ATTRIBUTES_SECTION + ConstantUtils.PACKAGE_CONFIG_DELIMITER
              + ConstantUtils.SAMPLE_APP_DATA_POLLING_INTERVAL);
      int pollingIntervalSecs = Integer.parseInt(pollingIntervalSecsStr);
      String assetName = props.getProperty(ConstantUtils.IOTDC_ATTRIBUTES_SECTION
          + ConstantUtils.PACKAGE_CONFIG_DELIMITER + ConstantUtils.SAMPLE_DEVICE_TAG_ID);
      while (true) {
        List<String> observations = getObservationUsingHTTPClient();
        for (String obsv : observations) {
          try {
            ruleProcessor.ProcessAndSendDataUsingDcClient(assetName, obsv);
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
    if (batchClient != null) {
      batchClient.closeBatchCheckThread();
      }
  }

}
