/**********************************************************************
 * COPYRIGHT
 *   Copyright (c) 2016 by Cisco Systems, Inc.
 *   All rights reserved.
 *
 * DESCRIPTION
 *   IoTSP thing SDK sample app 
 *
 * Author
 *   wxiangqi  2016-07-29 
 *
 *********************************************************************/

package com.cisco.devicesdk.sample;
import com.cisco.devicesdk.core.IotspThing;
import java.util.Date;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
        
public class SampleBasic {
        private static Logger LOG = LoggerFactory.getLogger(SampleBasic.class); 
        
	public void ThingSample(String uuid){
           // IotspThing accepts different number of parameters from uuid, mac, serialNumber,
	   // make, model, firmwareVerison, hardwareVersion but uuid is a must 
           IotspThing thingSample = new IotspThing(uuid);
           java.util.Date date= new java.util.Date();
           String tmp;
           for (int i = 0; i < 3; i++) {
               String data = "{\"messages\":[";
               Random rand = new Random();
               int n = rand.nextInt(50) + 1;
               int j = rand.nextInt(50) + 1;
               long time = date.getTime();
               tmp = String.format("\"temperature\":%d,\"humidity\":%d",n, j);
               tmp = String.format("{\"data\":{%s},\"ts\":%s, \"format\":\"json\"}", tmp, Long.toString(time));
               data += tmp;
               data += "]}"; 
               LOG.debug("Posting: " + data);
               if (thingSample != null) {
                   //thingSample.post(data) and  thingSample.post(type, tag, data) are both supported
                   thingSample.post("json", null, data);
                   try {
                       Thread.sleep(1000);         
                   } catch(InterruptedException ex) {
                       Thread.currentThread().interrupt();
                   }
               }
           }
           if (thingSample != null) {
                   thingSample.close();
           }
           return;
        }
        
}
