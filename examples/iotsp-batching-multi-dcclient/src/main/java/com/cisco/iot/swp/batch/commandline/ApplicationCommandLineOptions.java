/**
 * Copyright (c) 2017 by Cisco Systems, Inc. All Rights Reserved Cisco Systems Confidential
 */
package com.cisco.iot.swp.batch.commandline;

public class ApplicationCommandLineOptions {
  private String configFile = "";
  private String dataFile = "";
  private boolean sendDatatoCloud = false;
  private boolean sendDataToDevice = false;

  public ApplicationCommandLineOptions() {
  }

  public String getConfigFile() {
      return configFile;
  }

  public void setConfigFile(String configFile) {
      this.configFile = configFile;
  }

  public String getDataFile() {
      return dataFile;
  }

  public void setDataFile(String dataFile) {
      this.dataFile = dataFile;
  }

  public boolean isSendDataToCloud() {
      return sendDatatoCloud;
  }

  public void setSendDataToCloud(boolean sendDatatoCloud) {
      this.sendDatatoCloud = sendDatatoCloud;
  }

  public boolean isSendDataToDevice() {
      return sendDataToDevice;
  }

  public void setSendDataToDevice(boolean sendDataToDevice) {
      this.sendDataToDevice = sendDataToDevice;
  }
}