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

import java.net.HttpURLConnection;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.sql.Timestamp;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;

public class IotspHttp extends Httplib {
        private long tokenTime                  = 0; 
        private String hostURL                = null;
        private String uuid                   = null;
        private ThingDescriptor thing         = null;
        private Jsonlib jsonReader            = null;
        private String sdkConfigFile          = IotspConfigs.getConfigFile();
        
        private static final int TYPE_CREATE  = 0; 
        private static final int TYPE_RENEW   = 1; 
        private static final Logger LOG       = LoggerFactory.getLogger(IotspHttp.class); 
        
        public IotspHttp (ThingDescriptor iotThing) {
           this.thing = iotThing;
           uuid = iotThing.getUuidId();
           jsonReader = new Jsonlib();
            if (jsonReader == null) {
                LOG.error("Out of memeory!");
                return;
            }
        }
        
        public void setClientConfig(String host, int port,
                                    String upTopic, String downTopic, 
                                    String username, String password,
                                    String iotClientID) {
            hostURL =  "https://" + host + IotspConfigs.urlHttpPublish;
        }
        
        
        public int createTokens() {
            return opTokens(TYPE_CREATE);
        }


        public int renewTokens() {
            return opTokens(TYPE_RENEW);
        }
        
        public int opTokens(int type) {
            int res = -1;
            String tokenURL = "https://" + jsonReader.parserKey(sdkConfigFile, IotspConfigs.keyRegServer);
            String oAuthresponse = null;
            if ( TYPE_CREATE == type) {
                LOG.debug("Creating Tokens ...");
                tokenURL += thing.getTokenCreateURL();
                setClientHeader(null);
            } else {
                LOG.debug("Renewing Tokens ...");
                tokenURL += thing.getTokenRenewURL(); 
                String token = jsonReader.getTokenKey(uuid, IotspConfigs.keyAccessToken);
                setClientHeader("Authorization:Bearer " + token);
            }
            if (tokenURL == null) {
                 LOG.error("Fail to get token URL!");
                 return res;
            }
            LOG.debug("oAuthURL: " + tokenURL);
            try {
                 oAuthresponse = sendData("POST", tokenURL, "");
            } catch (Exception e) {
                 LOG.error("{} ", e.getMessage());
                 LOG.error("Fail to get token!");
                 return res;
            }
                
            if ((oAuthresponse != null) && oAuthresponse.contains(IotspConfigs.keyToken)) {
                jsonReader.SaveInfoToCache(uuid, oAuthresponse);
                // Update the token time
                java.util.Date date= new java.util.Date();
                tokenTime = date.getTime();
                res = 0;
            }
            return res;
        }
        
        public String getAccessToken() {
            String token = null;
            java.util.Date date= new java.util.Date();
            long now = date.getTime();
            
            String expireString = jsonReader.getTokenKey(uuid, IotspConfigs.keyExpire);
            LOG.debug("expire: " + expireString);
            if (expireString != null) {
                long expire = Long.parseLong(expireString);
                // get new Tokens after the token expires
                if ((expire + tokenTime) < now) {
                    createTokens();
                }
            } else {
                createTokens();
            }

            token = jsonReader.getTokenKey(uuid, IotspConfigs.keyAccessToken);
            LOG.debug("Token: " + token);
            return token;
        }

        public String getClientHeader() {
            String token = getAccessToken();
            if (token == null) {
                 LOG.error("Fail to get access token!");
                return null;
            }
            String header = thing.getHttpDCHeader();
            return header; 
        }

        public int publish(String pubMsg) {
            int res = -1;
            String header = getClientHeader();
            if (header == null) {
                 LOG.error("Fail to get access header!");
                return res;
            }
            LOG.debug("Publishing: \n" + pubMsg);
            try {
                 setClientHeader(header);
                 String response = sendData("POST", hostURL, pubMsg);
                 res = 0;
            } catch (Exception e) {
                 res = -1;
                 LOG.error("{} ", e.getMessage());
            }
            return res;            
        }

} 
