/**********************************************************************
 * COPYRIGHT
 *   Copyright (c) 2016 by Cisco Systems, Inc.
 *   All rights reserved.
 *
 * DESCRIPTION
 *   IoTSP thing SDK 
 *
 * Version 
 *   1.0           2016-08-03 
 *********************************************************************/

package com.cisco.devicesdk.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThingDescriptor {
        private String uuid;
        private String mac;
        private String serialNumber;
        private String make;
        private String model;
        private String fwVersion;
        private String hwVersion;
        private Jsonlib iotJsonlib = null;
        private static Logger LOG = LoggerFactory.getLogger(ThingDescriptor.class); 
        
        public ThingDescriptor (String uuid) {
            this(uuid, null, null, null, null, null, null);        
        }

        public ThingDescriptor (String uuid, String mac, String serialNumber) {
            this(uuid, mac, serialNumber, null, null, null, null);        
        }

        public ThingDescriptor (String uuid, String mac, String serialNumber,
                                String make, String model, String fwVersion,
                                String hwVersion) {
            if (uuid != null) { 
                this.uuid = uuid; 
            }
            if (mac != null) { 
                this.mac = mac;
            }
            if (serialNumber != null) {
                this.serialNumber = serialNumber;
            }
            if (make != null) {
                this.make = make;
            }
            if (model != null) {
                this.model = model;
            }
            if (fwVersion != null) {
                this.fwVersion = fwVersion;
            }
            if (hwVersion != null) {
                this.hwVersion = hwVersion;
            }
            iotJsonlib = new Jsonlib();
        } 
        
        public String getUuidId () {
            return this.uuid;
        }

        public String getIdString () {
            String data = "";
            if (this.uuid == null) {
               LOG.error("Error! Thing uuid could not be empty!");
               return null;
            }
            if (this.make != null) {
                data += "\"make\":\"" + this.make + "\"" + ",";
            }
            if (this.model != null) {
                data += "\"model\":\"" + this.model + "\"" + ",";
            }
            if (this.fwVersion != null) {
                data += "\"firmwareVersion\":\"" + this.fwVersion + "\"" + ",";
            }
            if (this.hwVersion != null) {
                data += "\"hardwareVersion\":\"" + this.hwVersion + "\"" + ",";
            }
            data += "\"uniqueIdentifiers\":{";
            data += "\"manufacturingId\":\"" + this.uuid + "\"";
            if (this.mac != null) {
                data += ",\"macAddress\":\"" + this.mac + "\"";
            }
            if (this.serialNumber != null) {
                data += ",\"serialNumber\":\"" + this.serialNumber + "\"";
            }
            data += "}";
            return data;
        }

        public String getRegistrationString() {
            String data = "{\"thing\":{\"claimed\":\"true\",";
            String id = getIdString();
            if (id == null) { 
                return null;
            }        
            data += id;
            data += "}}";
            return data;
        }
        
        public String getClaimString() {
            String data = "{\"claimed\":\"false\",";
            String id = getIdString();
            if (id == null) { 
                return null;
            }        
            data += id;
            data += "}";
            return data;
        }
        
        public String getTokenCreateURL() {
            if (iotJsonlib == null) {
                return null;
            }
            String deviceId = iotJsonlib.getCredentialKey(uuid, IotspConfigs.keyName);
            if (deviceId == null) { 
                return null;
            }        
            String password = iotJsonlib.getCredentialKey(uuid, IotspConfigs.keySecret);
            if (password == null) { 
                return null;
            }        
            String data = IotspConfigs.urlOauth;
            data = data.replace(IotspConfigs.keyOauthUser, deviceId);
            data = data.replace(IotspConfigs.keyOauthSecret, password);
            return data;
        }
        
        public String getTokenRenewURL() {
            if (iotJsonlib == null) {
                return null;
            }
            String RefreshToken = iotJsonlib.getTokenKey(uuid, IotspConfigs.keyRefreshToken);
            if (RefreshToken == null) {
                return null;
            }
            String data = IotspConfigs.urlOauthRenew;
            data = data.replace(IotspConfigs.keyOauthRefreshToken, RefreshToken);
            return data;
        }
        
        public String getHttpDCHeader() {
            if (iotJsonlib == null) {
                return null;
            }
            String deviceId = iotJsonlib.getCredentialKey(uuid, IotspConfigs.keyName);
            if (deviceId == null) { 
                return null;
            }        
            String Token = iotJsonlib.getTokenKey(uuid, IotspConfigs.keyAccessToken);
            if (Token == null) {
                return null;
            }
            String data = "route:" + IotspConfigs.urlUpTopic; 
            data = data.replace(IotspConfigs.keyURLId, deviceId);
            data += ",Authorization:bearer " + Token;
            return data;
        }
}
