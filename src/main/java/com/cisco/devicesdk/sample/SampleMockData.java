/**********************************************************************
 * COPYRIGHT
 *   Copyright (c) 2016 by Cisco Systems, Inc.
 *   All rights reserved.
 *
 * DESCRIPTION
 *   IoTSP thing SDK sample app 
 *
 *
 *********************************************************************/

package com.cisco.devicesdk.sample;
import com.cisco.devicesdk.core.IotspThing;
import com.cisco.devicesdk.core.Jsonlib;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
        
public class SampleMockData {
    private static Logger LOG = LoggerFactory.getLogger(SampleMockData.class); 

    public void ThingSample(){
	File fin = new File("data/mockdata.txt");
        BufferedReader br = null;
        String line = null;
        String tmp = null;
	IotspThing thingSample = null;
        
        try {	
            br = new BufferedReader(new FileReader(fin));
            tmp = br.readLine();
            if (tmp != null) {
		Jsonlib parser = new Jsonlib();    
		String uuid = parser.parserKeyInBuffer(tmp, "uuid");    
		String macAddress = parser.parserKeyInBuffer(tmp, "macAddress");    
		String SerialNumber = parser.parserKeyInBuffer(tmp, "serialNumber");
	        thingSample = new IotspThing(uuid, macAddress, SerialNumber);
	        if (thingSample == null) {
	    	    LOG.error("Thing initialization error!");
	            return;
	        }
	    } else {
	        LOG.error("Thing initialization error!");
	        return;
	    }

            java.util.Date date= new java.util.Date();
            while ((line = br.readLine()) != null) {
                String data = "{\"messages\":[";
                long time = date.getTime();
                tmp = String.format("{\"data\":%s,\"ts\":%s, \"format\":\"json\"}", line, Long.toString(time));
                data += tmp;
                data += "]}"; 
                LOG.debug("Posting: " + data);
                if (thingSample != null) {
                    //thingSample.post(data) and  thingSample.post(type, tag, data) are both supported
                    thingSample.post("json", null, data);
                    try {
                        Thread.sleep(2000);         
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
	    br.close();
	} catch (Exception e) {
	    LOG.error("Mockdata publishing error!");
	} 
        if (thingSample != null) {
            thingSample.close();
        }
        return;
    }
    
}
