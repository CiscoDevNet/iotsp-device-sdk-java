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


public class IotspConfigs {
        static final String envVariable                 = "CAFAPPPATH";
        
        static final String sdkConfigFile               = "config/sdkConfig.json";
        static final String credentialFile              = "data/registration.json";
        static final String tokenHttpFile               = "data/httpToken.json";
        
        public static final String keyRegServer         = "Server";
        
        public static final String urlRegistration      = "/v1/thing-services/things/actions/register";
        public static final String keyOauthUser         = "[DeviceId]";
        public static final String keyOauthSecret       = "[DeviceSecret]";
        public static final String keyOauthRefreshToken = "[RefreshToken]";
        public static final String urlOauth             = "/v1/user-services/oauth2/token?grant_type=password&username=[DeviceId]&password=[DeviceSecret]&client_id=iotspdeviceoauth2client&client_secret=iotspdeviceoauth2client";
        public static final String urlOauthRenew        = "/v1/user-services/oauth2/token?grant_type=refresh_token&refresh_token=[RefreshToken]&client_id=iotspdeviceoauth2client&client_secret=iotspdeviceoauth2client";
        public static final String urlHttpPublish       = "/v1/observations/publish";
        

        public static final String keyURLId             = "<thingID>";
        public static final String keyJsonType          = "json";
        public static final String keyXMLType           = "xml";
        public static final String keyOctetType         = "octet_stream";
        public static final String keyURLDefaultType    = keyJsonType;
        public static final String urlUpTopic           = "/v1/<thingID>/json_env/dev2app/";
        public static final String urlDownTopic         = "/v1/<thingID>/json_env/app2dev/";
        public static final String urlBlobUpTopic       = "/v1/<thingID>/dev2app/";
        public static final String urlBlobDownTopic     = "/v1/<thingID>/app2dev/";
        
        public static final String keyCaCert            = "CACertPath";
        public static final String keyVerifyServer      = "VerifyServer";
        
        public static final String keyDcMqtt            = "mqtt";
        public static final String keyDcHttp            = "http";
        public static final String keyDcWamp            = "wamp";
        
        public static final int    dcMqttSSLPort        = 8883;
        public static final int    defaultDcMqttPort    = dcMqttSSLPort;
        public static final String defaultDcType        = keyDcMqtt;
        public static final String keyUserDcType        = "DeviceConnector";
        public static final int    defaultDcHttpPort    = 443;
        
        public static final String keyCredential        = "credentials";
        public static final String keyCacheString       = keyCredential;
        public static final String keyRegisteredString  = "\"registered\":true";
        public static final String keyName              = "name";
        public static final String keySecret            = "secret";
        public static final String keyInterval          = "interval";
        
        public static final String keyAccessToken       = "access_token";
        public static final String keyRefreshToken      = "refresh_token";
        public static final String keyExpire            = "expires_in";
        public static final String keyToken             = keyAccessToken;
        
        public static final String keyDeviceUuid        = "deviceUuid";
        public static final String keyDeviceSerialNum   = "deviceSerialNum";
        public static final String keyMacAddress        = "macAddress";
        public static final String keyDeviceType        = "deviceType";
        public static final String keyMake              = "deviceMake";
        public static final String keyModel             = "deviceModel";
        public static final String keyFwVersion         = "deviceFirmwareVer";
        public static final String keyHwVersion         = "hardwareVer";
        
        public static final String keyGatewayUuid            = "gatewayUuid";
        public static final String keyGatewaySerialNum       = "gatewaySerialNum";
        public static final String keyGatewayMacAddress      = "gatewaymacAddress";
        public static final String keyGatewayDeviceType      = "gatewaydeviceType";
        
        public static final String keyGatewayEnabled         = "gatewayEnabled";
        
        public static final String keyLocalUuid              = "localMqttUuid";
        public static final String keyLocalMqttHost          = "localMqttHost";
        public static final String keyLocalMqttPort          = "localMqttPort";
        public static final String keyLocalMqttUpTopic       = "localMqttUpstreamTopics";
        public static final String keyLocalMqttDownTopic     = "localMqttDownstreamTopics";
        public static final String keyLocalMqttName          = "localMqttName";
        public static final String keyLocalMqttSecret        = "localMqttSecret";
        public static final String keyLocalMqttSsl           = "localMqttSsl";

        private IotspConfigs() { }
        
        static private String getFiles(String FilePath) {
            String envPath = System.getenv(envVariable);  
            if (envPath != null) {
                return envPath + '/' + FilePath;
            } else {
                return FilePath;
            }
        }

        static public String getConfigFile() {
            return getFiles(sdkConfigFile);
        }
        
        static public String getCredentialFile() {
            return getFiles(credentialFile);
        }
        
        
        static public String getTokenFile() {
            return getFiles(tokenHttpFile);
        } 
}
