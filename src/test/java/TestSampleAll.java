/**********************************************************************
 * COPYRIGHT
 *   Copyright (c) 2016 by Cisco Systems, Inc.
 *   All rights reserved.
 *
 * DESCRIPTION
 *   IoTSP thing SDK sample app 
 *
 * Version 
 *   1.0           2016-08-03 
 *********************************************************************/

import com.cisco.devicesdk.sample.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
        

public class TestSampleAll {
        
        private static Logger LOG = LoggerFactory.getLogger(TestSampleAll.class); 
        
        public static String currentUuid = "JavaSDKTest";
        
        public void TestAll() {
           LOG.info("************************ Sample Basic Test *****************************");
           SampleBasic MySampleBasic = new SampleBasic();
           MySampleBasic.ThingSample(currentUuid);
           LOG.info("***************************** Done *************************************\n ");
           
           LOG.info("************************ Sample MockData Test *****************************");
           SampleMockData MySampleMockData = new SampleMockData();
           MySampleMockData.ThingSample();
           LOG.info("***************************** Done *************************************\n ");
        }
        
        public static void main(String[] args) {
           TestSampleAll MyTest = new TestSampleAll();
           MyTest.TestAll();
        }
}
