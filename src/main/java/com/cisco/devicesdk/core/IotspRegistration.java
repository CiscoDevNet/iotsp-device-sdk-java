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

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IotspRegistration {
        
        private static Logger LOG = LoggerFactory.getLogger(IotspRegistration.class); 
        private String sdkConfigFile  = IotspConfigs.getConfigFile();

        /* Returns data connector type */
        public int Registration(ThingDescriptor thing) {
            int res = -1;

            String uuid = thing.getUuidId(); 
            Jsonlib jsonReader = new Jsonlib();
            if (jsonReader == null) { 
                return -1; 
            }
            
            Httplib tmpHttplib = new Httplib();
            if (tmpHttplib == null) {
                return -1; 
            }

            // Check if the thing has been registered
            // if registered, just return the whole response
            String data = jsonReader.checkCache(uuid);
            
            LOG.debug("Trying to register ...");

            String cpodServerUrl = "https://" +
                                   jsonReader.parserKey(sdkConfigFile, IotspConfigs.keyRegServer) +
                                   IotspConfigs.urlRegistration;

            LOG.debug(cpodServerUrl);

            String CaCert = jsonReader.parserKey(sdkConfigFile, IotspConfigs.keyCaCert); 
            if (null != CaCert) {
                tmpHttplib.setSSLCaCert(CaCert);
                LOG.debug("Cert path: " + CaCert);
            }
            
            String VerifyServer = jsonReader.parserKey(sdkConfigFile, IotspConfigs.keyVerifyServer); 
            LOG.debug("Verify Server: " + VerifyServer);
            if (VerifyServer != null) {
                tmpHttplib.setSSLVerifyServer(Integer.parseInt(VerifyServer));
            } else {
                LOG.error("Error! Could not get VerifyServer status!");
            }
            if (data == null) {
                data = thing.getRegistrationString();
            }
            if (data == null) {
                LOG.error("Error! Could not get registration string!");
                return -1;
            }
        
            LOG.debug("Registration: \n" + data);
            try {
                 String response = tmpHttplib.sendData("POST", cpodServerUrl, data);
                 if ((response != null) && response.contains(IotspConfigs.keyRegisteredString)) {
                     if (response.contains(IotspConfigs.keyCacheString)) {
                         jsonReader.SaveInfoToCache(uuid, response);
                     }
                     res = 0;
                 } 
            } catch (Exception e) {
                 res = -1;        
                 LOG.error("{} ", e.getMessage());
            }
            return res;
        }
}
