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

public class SpecialMessage {
  private static Logger LOG = LoggerFactory.getLogger(SpecialMessage.class);
  private GatewayAttributes gwAttributes = null;
  private Map<String, DeviceAttributes> deviceAttributes = null;

  private boolean sendDataToCloud;
  private ICloudConnectClient dcClient;

  public SpecialMessage(boolean sendDataToCloud, ICloudConnectClient dcClient,
      GatewayAttributes gwAttributes, Map<String, DeviceAttributes> deviceAttributes) {
    this.dcClient = dcClient;
    this.sendDataToCloud = sendDataToCloud;
    this.gwAttributes = gwAttributes;
    this.deviceAttributes = deviceAttributes;
  }

  public void sendKeepAlive() throws IoTDeviceSDKCommonException, IoTEdgeDcClientException {
    // Keep Alive Message
    KeepAliveDeviceInformation device =
        new KeepAliveDeviceInformation.KeepAliveDeviceInformationBuilder()
            .connected(EDeviceConnectedToGateway.CONNECTED)
            .deviceId(deviceAttributes.get(ConstantUtils.FIRST_DEVICE).getId())
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