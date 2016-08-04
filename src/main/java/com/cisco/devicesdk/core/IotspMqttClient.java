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

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.io.*;
import java.net.URL;


import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;


import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;


import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.StringWriter;


public class IotspMqttClient implements MqttCallback {

        MqttClient IotMqttClient       = null;
        MqttConnectOptions connOpt     = null;
        private String mqttHostURL     = null;
        private String mqttUpTopic     = null;
        private String mqttDownTopic   = null;
        private String mqttUsername    = null;
        private String mqttPassword    = null;
        private String sslCaCertPath   = null;
        private        String clientID = null;
        private int    pubQoS          = 0;
        private int    subQoS          = 0;
        private int    sslConfig       = 0;
        private int    sslVerifyServer = 1;
        private int    mqttConnected   = 0;
        
        private static Logger LOG = LoggerFactory.getLogger(IotspMqttClient.class); 
        
        public void setClientConfig(String iotMqttHost, int iotMqttPort, 
                                    String iotMqttUpTopic, String iotMqttDownTopic, 
                                    String iotMqttUsername, String iotMqttPassword, 
                                    String iotClientID) {
            if (sslConfig != 0) {
                mqttHostURL = "ssl://" + iotMqttHost + ":" + Integer.toString(iotMqttPort);
            } else {
                mqttHostURL = "tcp://" + iotMqttHost + ":" + Integer.toString(iotMqttPort);
            }
            //mqttHostURL = "tcp://" + iotMqttHost + ":" + Integer.toString(1883);
            mqttUpTopic = iotMqttUpTopic;
            mqttDownTopic = iotMqttDownTopic;
            mqttUsername = iotMqttUsername;
            mqttPassword = iotMqttPassword;
            clientID = iotClientID;
        }

        /* This function needs to set before other mqtt config to make ssl work */
        public void setSSLConfig(int sslOn){
            sslConfig = sslOn;
        }
        
        public void setSSLVerifyServerConfig(int verifyServer){
            if (sslConfig != 0) {
                sslVerifyServer = verifyServer;
            }
        }
        
        public void setSSLCaCert(String caCertPath){
            if (sslConfig != 0) {
                sslCaCertPath = caCertPath;
            }
        }

        /**
         * 
         * connectionLost
         * This callback is invoked upon losing the MQTT connection.
         * 
         */
        @Override
        public void connectionLost(Throwable t) {
            LOG.info("Connection lost!");
            // code to reconnect to the broker would go here if desired
        }

        /**
         * 
         * deliveryComplete
         * This callback is invoked when a message published by this client
         * is successfully received by the broker.
         * 
         */
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            LOG.info("Pub complete" );
        }

        /**
         * 
         * messageArrived
         * This callback is invoked when a message is received on a subscribed topic.
         * 
         */
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            LOG.info("-------------------------------------------------");
            LOG.info("| Message: " + new String(message.getPayload()));
            LOG.info("-------------------------------------------------");
        }

        public void setSubQos(int Qos) {
            subQoS = Qos;
        }
        
        public void setPubQos(int Qos) {
            pubQoS = Qos;
        }
        
        public void setUpTopic(String iotMqttUpTopic) {
            mqttUpTopic = iotMqttUpTopic;
        }
        
        public void setDownTopic(String iotMqttDownTopic) {
            mqttDownTopic = iotMqttDownTopic;
        }

        private int doSetup() {
            if (connOpt == null) {
                connOpt = new MqttConnectOptions();
                if (connOpt == null) {
                    return -1;
                }
            }
            connOpt.setCleanSession(true);
            connOpt.setKeepAliveInterval(30);
            connOpt.setUserName(mqttUsername);
            if (mqttPassword != null) {
                connOpt.setPassword(mqttPassword.toCharArray());
            }
            // Connect to Broker
            try {
                if (IotMqttClient == null) {
                    IotMqttClient = new MqttClient(mqttHostURL, clientID);
                }
                if (sslConfig != 0) {        
                    Security.addProvider(new BouncyCastleProvider());
                    // load CA certificate
                    JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");
                    String caCrtFile = sslCaCertPath;
                    PEMParser reader = new PEMParser(new FileReader(caCrtFile));
                    X509CertificateHolder caCertHolder = (X509CertificateHolder) reader.readObject();
                    reader.close();
                    
                    X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);
                    
                    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                                      TrustManagerFactory.getDefaultAlgorithm());
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keyStore.load(null, null);
                    keyStore.setCertificateEntry("ca-certificate", caCert);
                    trustManagerFactory.init(keyStore);
                    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
                    connOpt.setSocketFactory(sslContext.getSocketFactory());
                }
                IotMqttClient.setCallback(this);
            } catch (Exception e) {
                LOG.error("Mqtt set up error! ");
                return -1;
            }
            return 0; 
        }
        
        public int doConnect() {
            int res = 0;        
            try {
                if (mqttConnected == 0) {
                    IotMqttClient.connect(connOpt);
                    mqttConnected = 1;
                    LOG.info("Connected to " + mqttHostURL);
                }
            } catch (Exception e) {
                mqttConnected = 0;
                res = -1;
                LOG.error("Mqtt connection error to " + mqttHostURL);
            }
            return res;
        }

        
        public int Close() {
            // disconnect
            try {
                IotMqttClient.disconnect();
            } catch (Exception e) {
                LOG.error("Mqtt disconnection error!");
                return -1;
            }
            return 0;

        }

        public int Publish(String pubMsg) {
           // setup MQTT Client
            int ret = 0;
             
            if (IotMqttClient == null) {
                ret = doSetup();
                if (ret != 0) {
                   return ret;
                }
            }
            
            ret = doConnect(); 
            if (ret != 0) {
               return ret;
            }

            // setup topic
            MqttTopic topic = IotMqttClient.getTopic(mqttUpTopic);

            // publish messages if publisher
            MqttMessage message = new MqttMessage(pubMsg.getBytes());
            message.setQos(pubQoS);
            message.setRetained(false);

            // Publish the message
            LOG.debug("Publishing to topic \"" + topic + "\" qos " + pubQoS);
            MqttDeliveryToken token = null;
            try {
                // publish message to broker
                token = topic.publish(message);
                // Wait until the message has been delivered to the broker
                token.waitForCompletion();
                Thread.sleep(100);
            } catch (Exception e) {
                LOG.error("Mqtt publishing error!");
                return -1;
            }
            
            return ret;
        }
        
        
        public int Subscribe() {
            int ret = 0;
            if (IotMqttClient == null) {
                ret = doSetup();
                if (ret != 0) {
                   LOG.error("Mqtt setup error!");
                   return ret;
                }
            }
            
            ret = doConnect();
            if (ret != 0) {
               return ret;
            }

            // subscribe to topic if subscriber
            try {
                IotMqttClient.subscribe(mqttDownTopic, subQoS);
            } catch (Exception e) {
                LOG.error("Mqtt subscription error!");
            }
            return ret;
        }
}
