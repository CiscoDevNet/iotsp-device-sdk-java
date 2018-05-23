package com.cisco.iot.swp.batch.re.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.iot.swp.device.sdk.common.exception.IoTDeviceSDKCommonException;
import com.cisco.iot.swp.device.sdk.common.model.config.DeviceAttributes;
import com.cisco.iot.swp.device.sdk.common.model.config.GatewayAttributes;
import com.cisco.iot.swp.device.sdk.common.model.message.type.AckMessage;
import com.cisco.iot.swp.device.sdk.common.model.message.type.DiagnosticMessage;
import com.cisco.iot.swp.device.sdk.common.model.message.type.EMessageStatus;
import com.cisco.iot.swp.device.sdk.common.model.message.type.EMessageType;
import com.cisco.iot.swp.device.sdk.common.model.message.type.KeepAliveConfigurationInformation;
import com.cisco.iot.swp.device.sdk.common.model.message.type.KeepAliveDeviceInformation;
import com.cisco.iot.swp.device.sdk.common.model.message.type.KeepAliveMessage;
import com.cisco.iot.swp.device.sdk.common.utils.ConfigHelper;
import com.cisco.iot.swp.device.sdk.common.utils.EDeviceAdministrativeState;
import com.cisco.iot.swp.device.sdk.common.utils.EDeviceConnectedToGateway;
import com.cisco.iot.swp.device.sdk.common.utils.EDeviceReachableFromGateway;
import com.cisco.iot.swp.edge.mqtt.client.ICloudConnectClient;
import com.cisco.iot.swp.edge.mqtt.exception.IoTEdgeDcClientException;
import com.cisco.it.swp.edge.utils.ConstantUtils;


/**
 * Contains methods that create special types of messages that do not contain data.
 * The types of special messages that exist are Keep Alive messages, Diagnostic messages, and Ack Messages 
 */
public class SpecialMessage {
  
  /** The log. */
  private static Logger LOG = LoggerFactory.getLogger(SpecialMessage.class);
  
  /** The gw attributes. */
  private GatewayAttributes gwAttributes = null;
  
  /** The device attributes. */
  private Map<String, DeviceAttributes> deviceAttributes = null;

  /** The send data to cloud. */
  private boolean sendDataToCloud;
  
  /** The dc client. */
  private ICloudConnectClient dcClient;

  /**
   * Instantiates a new special message.
   *
   * @param sendDataToCloud the send data to cloud
   * @param dcClient the dc client
   * @param gwAttributes the gw attributes
   * @param deviceAttributes the device attributes
   */
  public SpecialMessage(boolean sendDataToCloud, ICloudConnectClient dcClient,
      GatewayAttributes gwAttributes, Map<String, DeviceAttributes> deviceAttributes) {
    this.dcClient = dcClient;
    this.sendDataToCloud = sendDataToCloud;
    this.gwAttributes = gwAttributes;
    this.deviceAttributes = deviceAttributes;
  }
  
  /**
   * Generates and sends a keep alive message from the application running on the gateway. The application 
   * knows about device state and configuration. These messages are generated and periodically sent to
   * the data pipeline
   *
   * @throws IoTDeviceSDKCommonException the io T device SDK common exception
   * @throws IoTEdgeDcClientException the io T edge dc client exception
   */
  public void sendKeepAlive() throws IoTDeviceSDKCommonException, IoTEdgeDcClientException {
    // Keep Alive Message
    String deviceId = deviceAttributes == null ? gwAttributes.getGatewayId()
        : deviceAttributes.get("1") == null ? gwAttributes.getGatewayId()
            : deviceAttributes.get("1").getId() == null ? gwAttributes.getGatewayId() : deviceAttributes.get("1").getId();
    KeepAliveDeviceInformation device =
        new KeepAliveDeviceInformation.KeepAliveDeviceInformationBuilder()
            .connected(EDeviceConnectedToGateway.CONNECTED)
            .deviceId(deviceId)
            .enabled(EDeviceAdministrativeState.ON).reachable(EDeviceReachableFromGateway.REACHABLE)
            .build();
    List<KeepAliveDeviceInformation> deviceInfo = new ArrayList<KeepAliveDeviceInformation>();
    deviceInfo.add(device);
    List<KeepAliveConfigurationInformation> configs =
        new ArrayList<KeepAliveConfigurationInformation>();
    KeepAliveConfigurationInformation configForDevice =
        new KeepAliveConfigurationInformation.KeepAliveConfigurationInfoBuilder().confId("-1")
            .messageKind("abc").build();
    configs.add(configForDevice);
    KeepAliveMessage msg = new KeepAliveMessage.KeepAliveMessageBuilder()
        .keepAliveDeviceInfo(deviceInfo).timestamp(String.valueOf(System.currentTimeMillis()))
        .configurationsInfo(configs).build();
    if (sendDataToCloud) {
      synchronized (dcClient) {
        dcClient.publish(ConfigHelper.getKeepAliveTopicForGateway(gwAttributes), msg.toString());
        LOG.debug("msg='Keep Alive message generated. {} -> {}'",
            com.cisco.iot.swp.device.sdk.common.utils.ConfigHelper
                .getKeepAliveTopicForGateway(gwAttributes),
            msg.toString());
      }
    } else {
      LOG.info("msg='Keep Alive message generated. {} -> {}'",
          ConfigHelper.getKeepAliveTopicForGateway(gwAttributes), msg.toString());
    }
  }
  
  /**
   * Generates and sends a diagnostic message from the application running on the gateway. The diagnostic messages
   * contain information about any issues with the devices or gateways.
   * These messages are generated and periodically sent to the data pipeline
   *
   * @throws IoTDeviceSDKCommonException the io T device SDK common exception
   * @throws IoTEdgeDcClientException the io T edge dc client exception
   */
  public void sendDiagnosticMessage() throws IoTDeviceSDKCommonException, IoTEdgeDcClientException {
    Random rand = new Random();
    int n = rand.nextInt(50) + 1;
    String diagMsg = "Diagnostic Message" + String.valueOf(n);
    DiagnosticMessage diagnostic = new DiagnosticMessage.DiagnosticMessageBuilder().message(diagMsg)
        .severity(1).timestamp(String.valueOf(System.currentTimeMillis())).build();
    if (sendDataToCloud) {
      synchronized (dcClient) {
        dcClient.publish(ConfigHelper.getDiagnosticTopicForGateway(gwAttributes),
            diagnostic.toString());
        LOG.debug("msg='Diagnostic message generated. {} -> {}'",
            ConfigHelper.getDiagnosticTopicForGateway(gwAttributes), diagnostic.toString());
      }
    } else {
      LOG.info("msg='Diagnostic message generated. {} -> {}'",
          ConfigHelper.getDiagnosticTopicForGateway(gwAttributes), diagnostic.toString());
    }
  }
  
  /**
   * Generates and sends Ack/Nack messages to confirm if a message for a command is received from the
   * IBM application.
   *
   * @throws Exception the exception
   */
  public void sendAckMessage() throws Exception {
    AckMessage ackMessage = new AckMessage.AckMessageBuilder()
        .deviceId(deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getId())
        .gatewayId(gwAttributes.getGatewayId()).messageKind("create_Devices")
        .messageStatus(EMessageStatus.ACK).messageType(EMessageType.COMMAND)
        .timestamp(String.valueOf(System.currentTimeMillis())).messageId("1234").build();

    if (sendDataToCloud) {
      dcClient.publish(ConfigHelper.getMessageStatusTopicForGateway(gwAttributes),
          ackMessage.toString());
      LOG.debug("msg='Sending ackMessage={}'", ackMessage.toString());
    } else {
      LOG.info("msg='ackMessage={}'", ackMessage.toString());
    }
  }
}
