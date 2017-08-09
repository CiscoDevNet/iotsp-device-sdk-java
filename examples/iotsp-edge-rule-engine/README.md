# iotsp-edge-rule-engine


This project provides parser library and sample app for iot rule engine. The sample application reads sensor values from a Raspberry Pi attached to your gateway. The Raspberry Pi should be running the SenseHat app (Instructions for installing the Sensehat app on the Raspberry Pi can be found in the readme of Pi-sensehat directory of this repository). The messages pulled from Raspberry Pi are fed to the rule engine and depending on the rule and command line options specified, are sent to IoT DataConnect Cloud.  

## Change log
08/02/2017 - Inital Commit, Version 0.1.0.0 

## Features 
- [x] Rule Processing library 
- [x] Rule processing sample application
- [x] Unit test  
- [x] Connection to cloud using mqtt client  
- [ ] Metrics
- [x] Logging
- [ ] Performance test
- [ ] Other enhancements (TBD) 

# Getting Started  
## Development notes
Follow these steps for the initial setup and get started with development:

1. Install JDK, version 8 is recommended

2. Clone this repository

3. Build the project using "gradlew build" 

4. To generate the eclipse project info including setting up classpaths use "gradlew eclipse" 

5. After step 3, a jar file for the project will be created. Make sure to copy the <>-all.jar from build/libs folder to IOxPackage folder to use the app as it is or use the <>.jar in any of your reference projects.    

6. To read about how the Sample App is created and how to use it, read the GettingStarted.md in this repository  


## Important - Updating the repository  
* After making and testing all changes to the repository, make sure to copy the latest fat jar (from build/libs/) to IOxPackage folder before checking in any code. Also update the ReadMe for project structure if there is any change.  

## Important - Building the IOXPackage
* The contents of the IOxPackage directory to be built using the ioxclient in the iox-client directory MUST be built in a LINUX environment. Building it in a local macbook environment will not work. Ubuntu 16 is recommended.


### Notes
#### Java version
The project is compiled using Java 1.8. and Java 1.8 is required for this application.

# Running the Application 
## To RUN the app in Eclipse IDE :  
* Run the RESampleApp.java.  

## To RUN the build-in command line rule eval app :  
* Clone the repo and Build the app using  
```  
git clone <this repository>     

cd /device-sdk-java/examples/iotsp-edge-rule-engine

./gradlew clean build    
```    

* Modify package_config.ini. If this code is running on a local environment, modify the package_config.ini in the root directory of this project to contain the correct cluster and device information. If this code is running in IOx, the package_config.ini in the IOxPackage directory needs to be updated to contain the correct cluster and device information.   

* To read messages from raspberry pi and print the actions on them, run the following command:  
```
java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iot-edge-rule-engine-<version>-SNAPSHOT-all.jar  
```  
* By default the messages that are received by the sample application (by subscribing to MQTT topics) are logged in the log file. The sample application posts messages to the thing(the Rasberry pi) attached to the gateway when the option -p is mentioned.  
   ```  
   java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -p    
   ```   

* To read messages from raspberry pi and send the messages along with actions on them to IoTDC, run the following command:   
```  
java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -s  
```   

* To read messages from a file and print the actions, run the following command:   
```  
java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -d IOxPackage/genericData.data  
```  

## To RUN the app in IOx VM on your local setup:
* Learn how to setup the local IOx VM with a step by step guide on how to build IOx packages with an ioxclient here
https://learninglabs.cisco.com/tracks/Cisco-IOx 

* Clone this repo and build the app using  
```  
git clone <this repository>     

cd /device-sdk-java/examples/iotsp-edge-rule-engine

./gradlew clean build    
```    

* Copy the fat jar from build/libs to IOxPackage.     
```  
cp build/libs/iot-edge-rule-engine-0.1-SNAPSHOT-all.jar IOxPackage/  
```  

* The IOxPackage has all the necessary files required to deploy the app. Inside of the folder the following files can be found
 - .jar - The latest fat jar file created by you used for deployment.  
 - launch.sh - The launch file used by application. 
   * To send data to mqtt client make sure to add -s in java -jar line   
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -s   
   ```   
   * By default the messages that are received by the sample application (by subscribing to MQTT topics) are logged in the log file. 
     The sample application posts messages to the thing(the Raspberry pi) attached to gateway when the -p option is supplied as in the example below.
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -p  
   ```   
   * To read messages from a file, make sure to add the file in IOxPackage and add -d filename in java -jar line   
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -d genericData.data   
   ```    
 - genericData.data - To read the observations from a file, add it here like this file.   
 - logging.properties - Modify this to change log levels or logging format.   
 - package_config.ini - This file contains the configuration for app. Modify this accordingly.  
 - package.yaml - This is used for IOx app packaging. Modify it accordingly.  

* Deploy app in local IOx using commands shown below. The commands can change based on the IOxClient location and the iotsp-edge-rule-engine location.  
```
./ioxclient package ..path/iotsp-edge-rule-engine/IOxPackage  

./ioxclient application install <application_name> ..path/iotsp-edge-rule-engine/IOxPackage/package.tar    

./ioxclient application activate <application_name>    

./ioxclient application start <application_name>  
```    

## To RUN the app on Gateway using Portal:  
* Clone the repo and Build the app using  
```  
git clone <this repository>     

cd /device-sdk-java/examples/iotsp-edge-rule-engine

./gradlew clean build 
```    

* Copy the fat jar from build/libs to IOxPackage.     
```  
cp build/libs/iot-edge-rule-engine-0.1-SNAPSHOT-all.jar IOxPackage/  
```  

* The IOxPackage has all the necessary files required to deploy the app. Go inside the folder.  Following files can be found
 - .jar - The latest fat jar file created is used for deployment.  
 - launch.sh - The launch file used by application. 
   * To send data to mqtt client make sure to add -s in java -jar line in the format below. 
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -s  
   ```   
   * By default the messages received by the sample application (by subscribing to MQTT topics) are logged in the log file. The sample application posts messages to the thing(the Raspberry pi) attached to gateway when the -p option is supplied as in the example below.
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -p  
   ```   
   * To read messages from a file then make sure to add the file in IOxPackage and add -d filename in java -jar line in the format below.  
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -d genericData.data   
   ```    
 - genericData.data - To read the observations from a file, add it here where this file currently is.  
 - logging.properties - Modify this to change log levels or logging format.  
 - package_config.ini - This file contains configuration for app. Modify this accordingly.  
 - package.yaml - This is used for IOx app packaging. Modify it accordingly.  

* Package the app using the command shown below. This takes the ioxclient located in iox-client and uses it to package the contents of the IOxPackage directory. The command below can change based on the IOxClient location and the iotsp-edge-rule-engine location.  
```
./ioxclient package ..path/iotsp-edge-rule-engine/IOxPackage      
```    

* Go to portal and upload the package.tar.gz in Fog Application tab.  

* After uploading the app, install the app with atleast c1.medium size. Change the device ip address to point to the Raspberry Pi ip address. Change the rule if required.    

* When running the app, the logs by default go into RE_Multi_Devices<number>.log file. Log rotation is by default enabled which allows only 3 log files to be uploaded each of size 1Mb.  

* Note on console logging: The current sample application redirects the console logs to "stdout.log". The console logging is turned off by default in the "package_config.ini". If this is turned on and the application runs for long time, there is a chance of logs flooding up the space. If the redirection is disabled, IOx will have an issue with the stdout/stderr buffers getting full and it will not be flushed properly, causing the application to hang. Therefore, care needs to be taken when enabling console logging. The application developers can choose not to log to console at all by disabling the console logger in logging.properties.

# Change Log
* To change log level in the log file or stdout, go to IOxPackage/package_config.ini. Modify the log_level_file or log_level_console to get desired log level. The project uses java.util.logging. So the supported log levels are:-  
  FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE  
  They map to logback.xml logging levels as follow:  
 * FINEST  -> TRACE  
 * FINER   -> DEBUG  
 * FINE    -> DEBUG  
 * CONFIG  -> INFO  
 * INFO    -> INFO  
 * WARNING -> WARN  
 * SEVERE  -> ERROR  
* To change log level for the log file or stdout, go to IOxPackage/package_config.ini.  

# Questions and Feedback
Please email all questions and feedback to dataconnect-support@cisco.com
