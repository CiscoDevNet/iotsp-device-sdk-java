# iotsp-batching   

## Documentation     
* [GettingStarted](GettingStarted.md) for documentation.   

## Overview   
The following repository provides sample application for batching, storing and compression of messages sent upstream.       
There are 2 types of messages that are received by the library -  
* To be batched according to policy defined     
* To be batched for immediate send - These messages are those which had to be sent immediately but couldn't be processed due to connection errors or some other reasons. These are to be sent as soon as the prevailing condition is resolved.       
 
 
## Requirements   
* Java 1.8     

# Getting Started  
## Development notes
Follow these steps for initial setup and get started with development:

1. Install JDK, recommended version 8  

2. Clone repo      

3. execute "gradlew build" to build 

4. "gradlew eclipse" to generate eclipse project info including setting up classpath  

5. After step 3, a jar for the project will be created. Make sure you copy the <>-all.jar from build/libs folder to IOxPackage folder to use the app as it is or use the <>.jar in any of your reference projects.    

## To update the repository - Important  
* After making and testing all your changes to the repository, make sure you copy the latest fat jar (from build/libs/) to IOxPackage folder before checking in your code. Also update the ReadMe for project structure if there is any change.  


### Notes
#### Java version
The project is compiled using Java 1.8.  So, you will need to use Java 1.8 for this application.


# Run the app    

## To RUN the app in Eclipse IDE :  
* Clone the repository and Build the app using  
```  
cd iotsp-batching  

./gradlew clean build   
```    

* Modify package_config.ini in root.   

* Run the BatchManagerSampleAppHBR.java.   

* Provide environment variables by adding in Program Variables section of Run Configurations  
	* -s -> For sending data to cloud  
	* -d IOxPackage/genericData.data -> For reading data from a file. Refer [genericData](IOxPackage/genericData.data) file to check the message format.  
	
* Provide following in the VM Arguments section of Run Configurations  
	* -Djava.util.logging.config.file=IOxPackage/logging.properties  

## To RUN the jar:    
* Clone the repo and Build the app using  
```  
cd iotsp-batching  

./gradlew clean build   
```    

* Modify package_config.ini in root. To see logs on the console, make sure the "log_level_console" under logging section in package_config.ini is turned to at least INFO.    

* To read messages from a file and print the actions, run the following command:   
   ```  
   java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iotsp-batching-<version>-SNAPSHOT-all.jar -d IOxPackage/genericData.data  
   ```  
     
* To read messages from a file and send data upstream, run the following command:   
   ```  
   java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iotsp-batching-<version>-SNAPSHOT-all.jar -d IOxPackage/genericData.data -s
   ```  

## To RUN the app in IOx VM on your local setup:
* Setup the local IOx VM using
https://confluence-eng-sjc5.cisco.com/conf/display/IC/How+to+build+applications+on+Cisco+IOx  

* Clone the repository and Build the app using  
```  
cd iotsp-batching/  

./gradlew clean build    
```    

* Copy the fat jar from build/libs to IOxPackage.     
```  
cp build/libs/iotsp-batching-<version>-SNAPSHOT-all.jar IOxPackage/  
```  

* The IOxPackage has all the necessary files required to deploy the app. Go inside the folder.  Following files can be found
 - **.jar** - The latest fat jar file created by you used for deployment.  
 - **launch.sh** - The launch file used by application. 
   * If you want to send data to mqtt client make sure to add -s in java -jar line like  
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batching-<version>-SNAPSHOT-all.jar -s   
   ```   
   * If you want to read messages from a file then make sure you add the file in IOxPackage and add -d filename in java -jar line like  
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batching-<version>-SNAPSHOT-all.jar -d genericData.data   
   ```    
 - **genericData.data** - In case you want to read the observations from a file, add it here like this file.  
 - **logging.properties** - Modify this only if you want to change log levels or logging format.  
 - **package_config.ini** - This file contains configuration for app. Modify this accordingly.  
 - **package.yaml** - This is used for IOx app packaging. Modify it accordingly.  

* Deploy app in local IOx using commands shown below. The commands can change based on your IOxClient location and your iotsp-edge-rule-engine location.  
```
./ioxclient package ..path/iotsp-batching/IOxPackage  

./ioxclient application install <application_name> ..path/iotsp-batching/IOxPackage/package.tar    

./ioxclient application activate <application_name>    

./ioxclient application start <application_name>  
```    

## To RUN the app on Gateway using Portal:  
* Clone the repository and Build the app using  
```     

cd iotsp-batching/  

./gradlew clean build    
```    

* Copy the fat jar from build/libs to IOxPackage.     
```  
cp build/libs/iotsp-batching-<snapshot>-SNAPSHOT-all.jar IOxPackage/  
```  

* The IOxPackage has all the necessary files required to deploy the app. Go inside the folder.  Following files can be found
 - **.jar** - The latest fat jar file created by you used for deployment.  
 - **launch.sh** - The launch file used by application. 
   * If you want to send data to mqtt client make sure to add -s in java -jar line like  
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batching-<version>-SNAPSHOT-all.jar -s  
   ```   
   * By default the messages that are received by the sample application (by subscribing to MQTT topics) are logged in the log file. 
     The sample application could post the the message to thing(Rpi) attached to gateway when the option -p is mentioed as below.
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batching-<version>-SNAPSHOT-all.jar -p  
   ```   
   * If you want to read messages from a file then make sure you add the file in IOxPackage and add -d filename in java -jar line like  
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batching-<version>-SNAPSHOT-all.jar -d genericData.data   
   ```    
 - **genericData.data** - In case you want to read the observations from a file, add it here like this file.  
 - **logging.properties** - Modify this only if you want to change log levels or logging format.  
 - **package_config.ini** - This file contains configuration for app. Modify this accordingly.  
 - **package.yaml** - This is used for IOx app packaging. Modify it accordingly.  

* Package the app using commands shown below. The commands can change based on your IOxClient location and your iotsp-edge-rule-engine location.  
```
./ioxclient package ..path/iotsp-batching/IOxPackage      
```    

* Go to portal and upload your package.tar.gz in Fog Application tab.  

* After you upload the app, install the app with at least c1.medium size.       

* When you run the app, the logs by default go into RE_BC_Multi_Devices<number>.log file. Log rotation is by default enabled which allows only 3 log files to be uploaded each of size 1Mb.  

* Note on console logging: The current sample application redirects the console logs to "stdout.log". The console logging is turned off by default in the "package_config.ini". If turned on and application runs for long time there is a chance of filling up the space. If the redirection is disabled, the IoX will have an issue with the stdout/stderr buffers getting full and not getting flushed properly and application migh hang. So when enabling console logging care need to be taken. The application developers can choose not to log to console at all by disabling the console logger in logging.properties itself.

# Change Log
* To change log level for your log file or stdout, go to IOxPackage/package_config.ini. Modify log_level_file or log_level_console to get desired log level. The project uses java.util.logging. So the supported log levels are:-  
  FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE  
  They map to logback.xml logging levels as follow:  
 * FINEST  -> TRACE  
 * FINER   -> DEBUG  
 * FINE    -> DEBUG  
 * CONFIG  -> INFO  
 * INFO    -> INFO  
 * WARNING -> WARN  
 * SEVERE  -> ERROR  

# Remaining Tasks   
- [x] Batching  
- [x] Batch Policies - Number of Messages, Timeout, Size of Batch    
- [x] Compression and Decompression  
- [x] Unit test  
- [x] Helper class for usage        