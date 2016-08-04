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

import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;

public class Jsonlib {
        
    private String credentialConfigFile  = IotspConfigs.getCredentialFile();
    private String tokenConfigFile       = IotspConfigs.getTokenFile();
    
    private static final Logger LOG = LoggerFactory.getLogger(Jsonlib.class); 

    public String parserKey(String filename, String key) {
        String value = null;
        JSONParser parser = new JSONParser();
        try {
            FileReader fr = new FileReader(filename);
            Object obj = parser.parse(fr);
            JSONObject jsonObject = (JSONObject) obj;
            value = (String) jsonObject.get(key);
            fr.close();
        } catch (Exception e) {
            LOG.error("{} ", e.getMessage());
        }
        return value;

    }
    
    public String parserKeyInBuffer(String buffer, String key) {
        String value = null;
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(buffer);
            JSONObject jsonObject = (JSONObject) obj;
            value = (String) jsonObject.get(key);
        } catch (Exception e) {
            LOG.error("{} ", e.getMessage());
        }
        return value;

    }
        
        
    public void writeJsonFile(String s, String filename) {
        try {
            FileWriter file = new FileWriter(filename);
            file.write(s);
            file.flush();
            file.close();

        } catch (Exception e) {
            LOG.error("{} ", e.getMessage());
        }
    }
           
    public String getTokenKey(String uuid, String key) {
        JSONParser parser = new JSONParser();
        String value = null;
        
        try {
            FileReader fr = new FileReader(tokenConfigFile + "_" + uuid);
            Object obj = parser.parse(fr);
            JSONObject jsonObject = (JSONObject) obj;
            if (IotspConfigs.keyExpire.equals(key)) {
                value = Long.toString((long)jsonObject.get(key));
            }
            else { 
                value = (String)jsonObject.get(key);
            }
            fr.close();
            
        } catch (Exception e) {
            LOG.error("{} ", e.getMessage());
        }
        //LOG.info(value);

        return value;
    }

    public String getCredentialKey(String uuid, String key) {
        JSONParser parser = new JSONParser();
        String value = null;
        
        try {
            FileReader fr = new FileReader(credentialConfigFile + "_" + uuid);
            Object obj = parser.parse(fr);
            JSONObject jsonObject = (JSONObject) obj;
            JSONObject tmp = (JSONObject)jsonObject.get("service");
            if (key.equals(IotspConfigs.keyName) || key.equals(IotspConfigs.keySecret)) {
                tmp = (JSONObject)tmp.get("credentials");
            } else {
                tmp = (JSONObject)tmp.get("request");
            }
            value = (String)tmp.get(key);
            fr.close();
            
        } catch (Exception e) {
                LOG.error("{} ", e.getMessage());
        }
        //LOG.info(value);

        return value;
    }
    


    public void SaveInfoToCache(String uuid, String s) {
        if ((s != null) && s.contains(IotspConfigs.keyCredential)) {
            writeJsonFile(s,  credentialConfigFile + "_" + uuid);
        }
        else {
            if ((s != null) && s.contains(IotspConfigs.keyToken)) {
                writeJsonFile(s,  tokenConfigFile + "_" + uuid);
            }
        }
    }

    public String checkCache(String uuid) {
        String content = null; 
        FileReader fr = null;
        try {
            fr = new FileReader(credentialConfigFile + "_" + uuid);
        } catch (FileNotFoundException e) { }
        if (fr != null) {
            try (BufferedReader br = new BufferedReader(fr)) {
                content = br.readLine(); 
            } catch (IOException e) {
                LOG.error("Error occurred {} ", e);
            }
        }
        return content;
    }
    
    public int removeCacheEntry(String uuid) {
        int res = -1; 
        File file = null;
        
        try {
            file = new File(credentialConfigFile + "_" + uuid);
            if (file.delete()) {
                res = 0;
            }
        } catch (Exception e) {
                LOG.error("File deleting occurred {} ", e);
        }
        return res;
    }

}
