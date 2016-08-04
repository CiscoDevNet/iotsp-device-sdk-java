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
import java.util.Timer;
import java.util.TimerTask;

public class IotspThing {
        private int thingRegistered   = 0;
        private int postCount         = 0;
        private int dcMqttPort        = IotspConfigs.defaultDcMqttPort;
        private int dcHttpPort        = IotspConfigs.defaultDcHttpPort;
        
        private String thingUuid      = null;
        private String dcHost         = null;
        private String dcName         = null;
        private String dcType         = null;
        private String dcSecret       = null;
        private String topicType      = null;
        private String topicTag       = null;
        private String sdkConfigFile  = IotspConfigs.getConfigFile();
        
        private Timer timer             = null;
        private IotspMqttClient iotMqtt = null;
        private IotspHttp iotHttp       = null;
        private Jsonlib iotJsonlib      = null;
        private ThingDescriptor thing   = null;
        private IotspRegistration iotRegistration = null;
       
        private static Logger LOG = LoggerFactory.getLogger(IotspThing.class); 
        
       /**
        * Instantiate a IotspThing object.
        * 
        * @param uuid         unique identifier, other parames default to null
        * @return 
        */
        public IotspThing (String uuid) {
            this(uuid, null, null, null, null, null, null);
        }

       /**
        * Instantiate a IotspThing object.
        * 
        * @param uuid         unique identifier
        * @param mac          mac address 
        * @param serialNumber serial number
        * @return 
        */
        public IotspThing (String uuid, String mac, String serialNumber) {
            this(uuid, mac, serialNumber, null, null, null, null);
        }

       /**
        * Instantiate a IotspThing object.
        * 
        * @param uuid         unique identifier, must be set
        * @param mac          mac address, default is null 
        * @param serialNumber serial number, default is null
        * @param make         make, default is null
        * @param model        model, default is null
        * @param fwVersion    firmware verison, default is null
        * @param hwVersion    hardware version, default is null
        * @return 
        */
        public IotspThing (String uuid, String mac, String serialNumber, 
                           String make, String model, String fwVersion, 
                           String hwVersion) {
            this.thing = new ThingDescriptor(uuid, mac, serialNumber, 
                                             make, model, fwVersion, hwVersion);
            iotJsonlib = new Jsonlib();
            this.thingRegistered = 0;
            this.iotRegistration = null;
            this.thingUuid = uuid;
        }

       /**
        * Release the IotspThing resources.
        * 
        * @return 0  Success
        *         -1 Failure 
        */
        public int close() {
            if (iotMqtt != null) {
                iotMqtt.Close();
            }
            if (timer != null) {
                timer.cancel();
                timer.purge();
            }
            return 0;
        }
        
       /**
        * Delete the IotspThing registration entry in local database.
        * 
        * @return 0  Success
        *         -1 Failure 
        */
        public int reset() {
            if (iotJsonlib == null) {
                    return -1;
            }
            return iotJsonlib.removeCacheEntry(thingUuid);
        }

       /**
        * Specify the IotspThing data connector type.
        * 
        * @param userDcType "http" - https or "mqtt" - mqtt , default "http"
        * @return 0  Success
        *         -1 Failure 
        */
        public void setupDcType(String userDcType) {
            dcType = userDcType; 
        }
        
        public void setupdcMqttPort(int userDcMqttPort) {
            dcMqttPort = userDcMqttPort; 
        }
        
        public void setupDcHttpPort(int userDcHttpPort) {
            dcHttpPort = userDcHttpPort; 
        }

        private String getMqttTopics(String basicTopics, String userId, String userType, String userTag) {
            String tmp = basicTopics;
            tmp =  tmp.replace(IotspConfigs.keyURLId, userId);

            //only xml and json are allowed in the url
            if (IotspConfigs.keyXMLType.equals(userType)) {
                tmp = tmp.replace(IotspConfigs.keyURLDefaultType, userType);
            }
            if (userTag != null) {
                tmp = tmp +  userTag;
            }
            LOG.debug("Topics: " + tmp);
            return tmp;
        }


        private int mqttSetup(String type, String tag) {
           if (iotMqtt == null) {
               iotMqtt = new IotspMqttClient();
           }
           if (iotMqtt == null) {
               return -1;
           }
           String dcUpTopic    = getMqttTopics(IotspConfigs.urlUpTopic, dcName, type, tag);
           String dcDownTopic  = getMqttTopics(IotspConfigs.urlDownTopic, dcName, type, tag);
           
           if (dcMqttPort == IotspConfigs.dcMqttSSLPort) {
                   String mqttCert = iotJsonlib.parserKey(sdkConfigFile, IotspConfigs.keyCaCert); 
                   String mqttVerifyServer = iotJsonlib.parserKey(sdkConfigFile, IotspConfigs.keyVerifyServer); 
                   iotMqtt.setSSLConfig(1);
                   iotMqtt.setSSLCaCert(mqttCert);
                   if (mqttVerifyServer != null) { 
                       iotMqtt.setSSLVerifyServerConfig(Integer.parseInt(mqttVerifyServer));
                   } else {
                       LOG.error("Error! Could not get VerifyServer string");
                   }
           }
           iotMqtt.setClientConfig(dcHost, dcMqttPort,
                                   dcUpTopic, dcDownTopic, dcName, dcSecret, 
                                   thingUuid);
           return 0; 

        }
        
        private int httpSetup(String type, String tag) {
           if (iotHttp == null) {
               iotHttp = new IotspHttp(this.thing);
           } 
           if (iotHttp == null) {
               return -1;
           }
           
           //String dcUpTopic    = GetHttpTopics(IotspConfigs.urlUpTopic, dcName, type, tag);
           //String dcDownTopic  = GetHttpTopics(IotspConfigs.urlDownTopic, dcName, type, tag);
           
           iotHttp.setClientConfig(dcHost, dcHttpPort, 
                                   null, null, dcName, dcSecret, 
                                   thingUuid);
          
           return 0;
        }

        private int dcSetup() {
            int res = 0;
            
            if (iotJsonlib == null) {
                    return -1;
            }
           
            if (dcName == null) {        
                dcName   = iotJsonlib.getCredentialKey(thingUuid, IotspConfigs.keyName);
            }

            if (dcSecret == null) {        
                dcSecret = iotJsonlib.getCredentialKey(thingUuid, IotspConfigs.keySecret);
            }

            if ( dcName == null || (dcSecret == null)) {
                    LOG.error("Error! Registration credential failure!");
                    return -1;
            }
            
            dcHost = iotJsonlib.parserKey(sdkConfigFile, IotspConfigs.keyRegServer);
            dcType = iotJsonlib.parserKey(sdkConfigFile, IotspConfigs.keyUserDcType);
            
            if (dcType != null) {
                if (dcType.contains(IotspConfigs.keyDcMqtt)) {
                    res = mqttSetup(null, null); 
                } 
                if (dcType.contains(IotspConfigs.keyDcHttp)) {
                        res = httpSetup(null, null);
                } 
            } else {
                // by default https is used
                dcType = IotspConfigs.keyDcHttp; 
                res = httpSetup(null, null);
            }
             
            return res;
        }

        class KeepAliveTask extends TimerTask {
            public void run() {
                LOG.debug("Posted " + postCount);
                if (thingRegistered != 0) {
                    if (postCount > 0) {
                        // Keep Alive
                        int res = iotRegistration.Registration(thing);
                        if (res != 0) {
                           LOG.error("Error! Keep Alive failed!");
                           thingRegistered = 0;
                        }
                        postCount = 0;
                    } else {
                        // system will need to register after the     
                        timer.cancel();
                        timer.purge();
                        thingRegistered = 0;  
                    }
                    return;
                }
            }
        }

       /**
        * Post message.
        * 
        * @param type     message type, "xml" or "json" 
        * @param tag      user defined tag for the message 
        * @param message  message payload
        * @return 0  Success
        *         -1 Failure 
        */
        public int post(String type, String tag, String message) {
            int res = 0;
            int configChange = 0;
            if (thingRegistered == 0) {
                iotRegistration = new IotspRegistration();
                if (iotRegistration != null) {
                    res = iotRegistration.Registration(this.thing);
                    if (res != 0) {
                       LOG.error("Error! Thing registration failed!");
                       return -1;
                    }
                    thingRegistered = 1;
                    
                    res = dcSetup();
                    if (res != 0) {
                        return res;
                    }
                    
                    String timeString = iotJsonlib.getCredentialKey(thingUuid, IotspConfigs.keyInterval);
                    if (timeString != null) {
                        timeString = timeString.replaceAll("s", "");    
                        int mSeconds = Integer.parseInt(timeString) * 1000;
                        timer = new Timer();
                        timer.scheduleAtFixedRate(new KeepAliveTask(), mSeconds, mSeconds);
                    } else {
                        return -1;
                    }
                }
            }
            if ((type != null) || (tag != null) ) {
                if ((type != null) && !type.equals(topicType)) {
                    topicType = type;
                    configChange = 1;
                }
                if ((tag != null) && !tag.equals(topicTag)) {
                    topicTag = tag; 
                    configChange = 1;
                }
            } 
            if (iotMqtt != null) {
                 if (configChange != 0) {
                     mqttSetup(type, tag);
                     configChange = 0;
                 }
                 if (message != null) {
                     res = iotMqtt.Publish(message);
                 } else {
                     // empty message is allowed for keep alive 
                     res = 0; 
                 }
            }
            if (iotHttp != null) {
                 if (configChange != 0) {
                     httpSetup(type, tag);
                     configChange = 0;
                 }
                 if (message != null) {
                     res = iotHttp.publish(message);
                 } else {
                     // empty message is allowed for keep alive 
                     res = 0; 
                 }
            }
            if (res != 0) {
                LOG.error("Error! Thing post failure!");
            }
            postCount++;
            return res;
        }
        
       /**
        * Post message.
        * 
        * @param message  message payload, default type json and no tag.
        * @return 0  Success
        *         -1 Failure 
        */
        public int post(String message) {
            return post(null, null, message);
        }
}
