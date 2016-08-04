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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException;

public class Httplib {
        
        private static final Logger LOG   = LoggerFactory.getLogger(Httplib.class); 
        
        private int    sslVerifyServer = 0;
        private String sslCaCertPath   = null;
        private String clientHeader    = null;
        private String PayloadType     = IotspConfigs.keyURLDefaultType;
        
        public void setSSLVerifyServer(int verifyServer){
            sslVerifyServer = verifyServer;
        }
        
        public void setSSLCaCert(String caCertPath){
            sslCaCertPath = caCertPath;
        }
       
        public void setPayloadType(String UserPayloadType){
            if (IotspConfigs.keyXMLType.equals(UserPayloadType)) {
                PayloadType     = IotspConfigs.keyXMLType;
            } else {
                PayloadType     = IotspConfigs.keyURLDefaultType;
            }
        }
        
        private SSLContext setupSSLContext(String sslCaCertPath) {
            SSLContext sslContext = null;
            //System.setProperty("javax.net.ssl.trustStore",sslCaCertPath);
            //System.setProperty("javax.net.ssl.trustStorePassword","changeit");
            //System.setProperty("javax.net.debug","ssl");
            try { 
                KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null); 
                InputStream fis = new FileInputStream(sslCaCertPath); 
                BufferedInputStream bis = new BufferedInputStream(fis);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                
                while (bis.available() > 0) {
                    Certificate cert = cf.generateCertificate(bis);
                    trustStore.setCertificateEntry("iotspdev" + bis.available(), cert);
                }
                sslContext = SSLContext.getInstance("TLSv1.2");
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                                  TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            } catch (Exception e) {
                LOG.error("{} ", e.getMessage());
            }
            return sslContext;        
        }

        public void setClientHeader(String headerString) {
            clientHeader = headerString;
        }


        private void setConClientHeader(HttpURLConnection con) {
            String [] parts = clientHeader.split(",");
            for (int i = 0; i < parts.length; i++) {
                String [] content = parts[i].split(":");
                con.setRequestProperty(content[0], content[1]);
                LOG.debug("Header: " + content[0] + ":" +  content[1]);
            }
            return;
        }


        public String sendData(String Operations, String serverUrl, String postData) throws IOException {
            // curlInit and url
            URL url = new URL(serverUrl);
            HttpURLConnection con;
            if ( serverUrl.contains("https") ) {
                SSLContext sslContext = null;

                if (null != sslCaCertPath) {
                    sslContext = setupSSLContext(sslCaCertPath);
                }
                if (0 == sslVerifyServer) {
                    disableSslVerification(); 
                    con = (HttpsURLConnection) url.openConnection();
                } else {
                    HttpsURLConnection HttpsCon = (HttpsURLConnection) url.openConnection();
                    if (sslContext != null) {
                        HttpsCon.setSSLSocketFactory(sslContext.getSocketFactory());
                    }
                    con = (HttpsURLConnection)HttpsCon;
                }

            } else {
                con = (HttpURLConnection) url.openConnection();
            }
            
            if (clientHeader != null) {
                setConClientHeader(con); 
            }

            if ("POST".equals(Operations)) { 
                //  CURLOPT POST
                con.setRequestMethod("POST");
                if (postData != null) {
                    con.setRequestProperty("Content-length", String.valueOf(postData.length()));
                }
                con.setRequestProperty("Content-type", "application/" + PayloadType);
                con.setRequestProperty("Accept", "application/" + PayloadType);
        
                con.setDoOutput(true);
                //con.setDoInput(true);
                DataOutputStream output = new DataOutputStream(con.getOutputStream());
                if (postData != null) {
                    output.writeBytes(postData);
                }
                output.close();
            } else {
                //  CURLOPT POST
                con.setRequestMethod("GET");
            }
       

            int code = con.getResponseCode(); // 200 = HTTPOK
            LOG.debug("Response    (Code):" + code);
            LOG.debug("Response (Message):" + con.getResponseMessage());
        
            // read the response
            InputStream is = null;
            if (code >= 200 && code < 400) {
                // Create an InputStream in order to extract the response object
                is = con.getInputStream();
            } else {
                is = con.getErrorStream();
            }
            
            DataInputStream input = new DataInputStream(is);
            int c;
            StringBuilder resultBuf = new StringBuilder();
            while ( (c = input.read()) != -1) {
                resultBuf.append((char) c);
            }
            input.close();

            if (code >= 200 && code < 400) {
                LOG.debug(resultBuf.toString());
            } else {
                LOG.error("Http Error " + code + " : " +  serverUrl);
                LOG.error(resultBuf.toString());
            }
       
            return resultBuf.toString();
        }

        private static void disableSslVerification() {
            try
            {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
                };
        
                // Install the all-trusting trust manager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
                // Create all-trusting host name verifier
                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
        
                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            } catch (Exception e) {
                LOG.error("{} ", e.getMessage());
            }
        }
}
