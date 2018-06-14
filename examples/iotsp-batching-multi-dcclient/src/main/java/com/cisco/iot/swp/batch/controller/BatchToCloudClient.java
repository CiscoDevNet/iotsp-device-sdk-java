/**
 * Copyright (c) 2017 by Cisco Systems, Inc. All Rights Reserved Cisco Systems Confidential
 */
package com.cisco.iot.swp.batch.controller;


import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.iot.swp.batch.exception.IoTBatchingErrorCode;
import com.cisco.iot.swp.batch.exception.IoTBatchingException;
import com.cisco.iot.swp.device.sdk.common.compression.ICompressionUtils;
import com.cisco.iot.swp.device.sdk.common.model.message.type.BatchMessage;
import com.cisco.iot.swp.device.sdk.common.utils.BaseConstantsUserParams;
import com.cisco.iot.swp.edge.mqtt.client.ICloudConnectClient;
import com.cisco.iot.swp.edge.mqtt.client.MQTTClientEdge;
import com.cisco.iot.swp.edge.mqtt.exception.IoTEdgeDcClientException;


/**
 * Contains methods that sends batched messages to a given MQTT client.
 */
public class BatchToCloudClient {

  /** The batch manager. */
  private IBatchStoreManager batchManager = null;

  /** The compression utils. */
  private ICompressionUtils compressionUtils = null;

  /** The log. */
  private static Logger LOG = LoggerFactory.getLogger(BatchToCloudClient.class);

  /** The batch. */
  private byte[] batch;

  /** The check batch ready thread. */
  private ScheduledExecutorService checkBatchReadyThread;
  
  
  /** The dc client. */
  private ICloudConnectClient dcClient;

  /**
   * Takes in a configuration file, a compression type, and a MQTT client and starts a thread that
   * periodically checks if a batch is ready to be sent to a given MQTT topics specified in the
   * config file.
   *
   * @param props the configuration file that contains information necessary to send a batch to the
   *        cloud
   * @param compression the type of compression that is to be applied to the messages to batch them
   * @param dcClient a MQTT client that the batch will be sent using (ssl, websockets, etc.)
   * @throws IoTBatchingException the io T batching exception
   * @throws IoTEdgeDcClientException 
   */
  public BatchToCloudClient(Properties props, ICompressionUtils compression, String clientId) throws IoTBatchingException, IoTEdgeDcClientException {
    if (props == null) {
      throw new IoTBatchingException(IoTBatchingErrorCode.MISSING_PROPERTIES,
          "Properties recieved by BatchToCloudClient is null.");
    }
    if (compression == null) {
      throw new IoTBatchingException(IoTBatchingErrorCode.COMPRESSION_OBJECT_MISSING,
          "Compression object is null in constructor of BatchStoreManager");
    }
    initDCClient(props, clientId);
    if (dcClient == null) {
      LOG.info("No valid MQTT Client found, logging messages to output");
    }
    this.compressionUtils = compression;
    batchManager = new BatchStoreManager(props, this.compressionUtils);
    String topic = props.getProperty(BaseConstantsUserParams.GATEWAY_OBV_BATCH_TOPIC)
        + BaseConstantsUserParams.TOPIC_DELIMITER;
    checkBatchReady(topic, dcClient);
  }

  /**
   * Check batch ready.
   *
   * @param topic the topic
   * @param dcClient the dc client
   */
  protected void checkBatchReady(String topic, ICloudConnectClient dcClient) {
    LOG.debug("Starting new thread to check batch readiness");
    checkBatchReadyThread = Executors.newScheduledThreadPool(1);

    checkBatchReadyThread.scheduleAtFixedRate(new Runnable() {
      public void run() {
        batch = batchManager.getCompressedBatch();
        if (batch != null && batch.length != 0) {
          sendBatch(topic + System.currentTimeMillis(), dcClient, batch);
          LOG.trace("batch {} sent to broker or published to file", batch);
        }
      }
    }, 1, 3, TimeUnit.SECONDS);
    /// checkBatchReadyThread.shutdown();
  }

  /**
   * Puts a message into a batch, returns true if message is properly added to batch, false
   * otherwise.
   *
   * @param message the message to be batched
   * @return true if message is properly sent, false otherwise
   * @throws IoTBatchingException the io T batching exception
   */
  public boolean putMessageInBatch(BatchMessage message) throws IoTBatchingException {
    if (message != null) {
      String messageStr = message.toString();
      if (batchManager.addMessageToBatch(messageStr) == true) {
        return true;
      } else {
        LOG.error("err='Message {} was not properly added to the batch'", messageStr);
        return false;
      }
    }
    return false;
  }

  /**
   * Send batch to Cisco Kinetic DCM.
   *
   * @param topic the topic
   * @param dcClient the dc client
   * @param batch the batch
   */
  protected void sendBatch(String topic, ICloudConnectClient dcClient, byte[] batch) {
    if (dcClient != null) {
      try {
        synchronized (dcClient) {
          dcClient.publish(topic, batch);
        }
        LOG.debug("msg='Successfully published byte array to MQTT'");
      } catch (Exception e) {
        LOG.error("err='Message failed to send immediately', errMessage={}, errStack={}",
            e.getMessage(), e.getStackTrace());
        batchManager.handleUnsentCompressedBatch(batch);
        LOG.debug("msg='Compressed batch handled by batch manager to be sent later'");
      }
    } else {
      LOG.trace("No MQTT Client detected, logging to output");
      try {
        byte[] decompressedBatch = compressionUtils.decompress(batch);
        String data = new String(decompressedBatch);
        LOG.debug("msg='Decompressed string:{}'", new String(data));
      } catch (DataFormatException | IOException e) {
        LOG.error("err='Decompression of batch failed with error {}', errMessage={}, errStack={}",
            e.getMessage(), e.getStackTrace());
        batchManager.handleUnsentCompressedBatch(batch);
        LOG.debug("msg='Compressed batch handled by batch manager to be sent later'");
      }
    }
  }

  /**
   * Closes the thread that continuously checks if a batch is available to send and the batch
   * manager.
   */
  public void closeBatchCheckThread() {
    LOG.info("msg='Closing Batch Checker Thread'");
    batchManager.closeBatchManager();
    checkBatchReadyThread.shutdown();
  }
  
  private void initDCClient(Properties props, String clientId) throws IoTEdgeDcClientException {
    MqttCallbackExtended callback = new MqttCallbackExtended() {
      @Override
      public void connectionLost(Throwable cause) {
        LOG.error("Lost MQTT connection... Handle the lost connection");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        LOG.info("Message arrived on topic -> {} with payload {} ", topic, message);
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
        // logger.info("Delivery complete for message...");
      }

      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
        LOG.info("Reconnection status={}, serverURI={}", reconnect, serverURI);
      }
    };
    this.dcClient = new MQTTClientEdge(clientId);
    LOG.info("Initializing the Mqtt client [{}]  with callback...", clientId);
    this.dcClient.init(props, callback);
    this.dcClient.connect();
    while (!dcClient.isConnected()) {
      LOG.info("Waiting for the connection to established with client id [{}]", clientId);
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        LOG.error("exception during mqtt client initializtion {}.", e.getMessage());
      }
    } 
  }
}
