# iotsp-batch-store-manager  

This project provides functionality for batching, storing, and compressing messages received.     

## Overview   
The following repository implements batching, storing and compression library.   
For batching and storing the user defines the policies which decide the minimum batch size maintained before it can be compressed and sent. The policies provided are -   
* **MAX_SIZE** - The number of elements in a batch before it is declared to be ready for compression. If this policy is not defined then a default size policy is taken. (Default is 1mb)    
  - **IMPORTANT** - Please note that max size has priority over number of elements and max timeout.    
* **MAX_NUMBER_OF_ELEMENTS** - The size of batch before it is declared to be ready for compression.     
* **MAX_TIMEOUT** - The timeout of a batch before the batch is declared to be ready for compression. The batch time starts from its 1st message and is reinitialized after a clear of batch is called.    
   
There are 2 types of messages that are received by the library -  
* Messages that are to be batched according to policy defined.     
* Messages that are to be batched for immediate sending - These messages are messages that must be sent immediately but could not be processed due to connection errors or other disruptions. These messages are to be sent as soon as the inhibiting condition is resolved.       

## Change log

08/08/2017 - Initial Commit v 0.1.0.0

 
# Features  
- [x] Batching APIs 
- [x] Batching Policies - Number of Messages, Timeout, Size of Batch    
- [x] Compression (Zlib) and Decompression APIs
- [x] Client for pushing to cloud
- [ ] Unit tests  
- [ ] Additional compression libraries 
 
## Requirements   
* Java 1.8    

## API Overview    
  
### The Batching APIs are:  
  
* Add message to the batch      
```    
public boolean addMessageToBatch(String message) throws IoTBatchingException;       
```     

* Get the number of messages, size and time left before batch is ready    
```     
public String getCurrentBatchStatus();  
```    
 
* Get the compressed batch after checking if it's ready.  
```    
public byte[] getCompressedBatch();
```   

* Clear batch stored     
```   
public void clearBatch();  
```    

* Close the batch manager   
```   
public void closeBatchManager();
```   

### The Compression APIs are:      

* Set the compression level - MIN_COMPRESSION,MED_COMPRESSION,MAX_COMPRESSION      
```
public void setCompressionLevel(CompressionLevel level);   
```  

* Get the compression level.  
```
public CompressionLevel getCompressionLevel();  
```   

* Compressing the input String   
```  
public byte[] compress(String data) throws IOException;
```   

* Compressing the input byte array  
```  
public byte[] compress(byte[] data) throws IOException;
```   

* De-compressing the byte array  
```  
public byte[] decompress(byte[] data) throws IOException, DataFormatException;
```   

### Sending Batch to Cloud APIs:

* Creates a client that takes in a properties file, compression library, and a MQTT client and will continuously check if a if batch is ready to send to the client.
```
public BatchToCloudClient(Properties props, ICompressionUtils compression,
ICloudConnectClient dcClient) throws IoTBatchingException;
```

* Puts a message into a batch
```
public boolean putMessageInBatch(BatchMessage message) throws IoTBatchingException;
```

* Closes thread that continuously checks if batches are ready that is spun up during creation of the BatchToCloudClient object.
```
public void closeBatchCheckThread();
```


# Getting Started  
## Development notes
Follow these steps for initial setup and get started with development:

1. Install JDK, recommended version 8  

2. Clone this repository 

3. Build the project using "gradlew build" 

4. To generate the eclipse project info including setting up classpaths use "gradlew eclipse"  

5. After step 3, a jar file for the project will be created. Make sure to copy the <>-all.jar from build/libs folder to IOxPackage folder to use the app as it is or use the <>.jar in any of your reference projects.    

## Important - Updating the repository
After making and testing all changes to the repository, make sure to copy the latest fat jar (from build/libs/) to IOxPackage folder before checking in any code. Also update the ReadMe for project structure if there is any change.

### Notes
#### Java version
The project is compiled using Java 1.8.  So, you will need to use Java 1.8 for this application.


# Runing the Application
## To RUN the app in Eclipse IDE :  
* Run the BatchManagerSampleApp.java.   

## To RUN the build-in command line rule eval app :  
* Clone the repo and Build the app using  
```  
git clone <this repository>       

cd /device-sdk-java/examples/iotsp-batch-store-manager  

./gradlew clean build    
```    

* Modify package_config.ini. If this code is running on a local environment, modify the package_config.ini in the root directory of this project to contain the correct cluster and device information. If this code is running in IOx, the package_config.ini in the IOxPackage directory needs to be updated to contain the correct cluster and device information.  

* To read messages from raspberry pi and print the actions on them, run the following command:
   ```
   java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar  
   ```  
* By default the messages that are received by the sample application (by subscribing to MQTT topics) are logged in the log file. The sample application posts messages to the thing(the Rasberry pi) attached to the gateway when the option -p is mentioned. 
   ```  
   java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -p    
   ```   

* To read messages from raspberry pi and send the messages along with actions on them to IoTDC, run the following command::   
   ```  
   java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -s  
   ```   

* To read messages from a file and print the actions, run the following command:   
   ```  
   java -Djava.util.logging.config.file=IOxPackage/logging.properties -jar build/libs/iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -d IOxPackage/genericData.data  
   ```  

## To RUN the app in IOx VM on your local setup:
* Learn how to setup the local IOx VM with a step by step guide on how to build IOx packages with an ioxclient here https://learninglabs.cisco.com/tracks/Cisco-IOx


* Clone the repo and Build the app using  
```  
git clone <this repository>      

cd /device-sdk-java/examples/iotsp-batch-store-manager

./gradlew clean build    
```    

* Copy the fat jar from build/libs to IOxPackage.     
```  
cp build/libs/iotsp-batch-store-manager-0.1.0.0-SNAPSHOT-all.jar IOxPackage/  
```  

* The IOxPackage has all the necessary files required to deploy the app. Inside of the folder the following files can be found 
 - **.jar** - The latest fat jar file created by you used for deployment.  
 - **launch.sh** - The launch file used by application. 
   - To send data to mqtt client make sure to add -s in java -jar line 
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -s   
   ```   
   * By default the messages that are received by the sample application (by subscribing to MQTT topics) are logged in the log file. The sample application posts messages to the thing(the Raspberry pi) attached to gateway when the -p option is supplied as in the example below.
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -p  
   ```   
   * To read messages from a file, make sure to add the file in IOxPackage and add -d filename in java -jar line
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -d genericData.data   
   ```    
 - **genericData.data** - In case you want to read the observations from a file, add it here like this file.  
 - **logging.properties** - Modify this only if you want to change log levels or logging format.  
 - **package_config.ini** - This file contains configuration for app. Modify this accordingly.  
 - **package.yaml** - This is used for IOx app packaging. Modify it accordingly.  

* Deploy app in local IOx using commands shown below. The commands can change based on the IOxClient location and the iotsp-edge-rule-engine location.  
```
./ioxclient package ..path/iotsp-batch-store-manager/IOxPackage  

./ioxclient application install <application_name> ..path/iotsp-batch-store-manager/IOxPackage/package.tar    

./ioxclient application activate <application_name>    

./ioxclient application start <application_name>  
```    

## To RUN the app on Gateway using Portal:  
* Clone the repo and Build the app using  
```  
git clone <this repository>   

cd /device-sdk-java/examples/iotsp-batch-store-manager  

./gradlew clean build    
```    

* Copy the fat jar from build/libs to IOxPackage.     
```  
cp build/libs/iotsp-batch-store-manager-0.1.0.0-SNAPSHOT-all.jar IOxPackage/  
```  

* The IOxPackage has all the necessary files required to deploy the app. Inside the folder, the following files can be found
 - **.jar** - The latest fat jar file created by you used for deployment.  
 - **launch.sh** - The launch file used by application. 
   - To send data to mqtt client make sure to add -s in java -jar line in the format below  
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -s  
   ```   
   * By default the messages received by the sample application (by subscribing to MQTT topics) are logged in the log file. The sample application posts messages to the thing(the Raspberry pi) attached to gateway when the -p option is supplied as in the example below.
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -p  
   ```   
   * To read messages from a file then make sure to add the file in IOxPackage and add -d filename in java -jar line in the format below.
   ```  
   java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -d genericData.data   
   ```    
 - **genericData.data** - In case you want to read the observations from a file, add it here like this file.  
 - **logging.properties** - Modify this only if you want to change log levels or logging format.  
 - **package_config.ini** - This file contains configuration for app. Modify this accordingly.  
 - **package.yaml** - This is used for IOx app packaging. Modify it accordingly.  

* Package the app using commands shown below. The commands can change based on your IOxClient location and your iotsp-edge-rule-engine location.  
```
./ioxclient package ..path/iotsp-edge-rule-engine/IOxPackage      
```    

* Go to portal and upload your package.tar.gz in Fog Application tab.  

* After uploading the app, install the app with atleast c1.medium size. Change the device ip address to point to the Raspberry Pi ip address. Change the rule if required.   

* When running the app, the logs by default go into RE_Multi_Devices.log file. Log rotation is by default enabled which allows only 3 log files to be uploaded each of size 1Mb.  

* Note on console logging: The current sample application redirects the console logs to "stdout.log". The console logging is turned off by default in the "package_config.ini". If this is turned on and the application runs for long time, there is a chance of logs flooding up the space. If the redirection is disabled, IOx will have an issue with the stdout/stderr buffers getting full and it will not be flushed properly, causing the application to hang. Therefore, care needs to be taken when enabling console logging. The application developers can choose not to log to console at all by disabling the console logger in logging.properties.

# Changing Log Level
* To change log level in the log file or stdout, go to IOxPackage/package_config.ini. Modify the log_level_file or log_level_console to get desired log level. The project uses java.util.logging. So the supported log levels are:
- FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE
  They map to logback.xml logging levels as follow:  
 * FINEST  -> TRACE  
 * FINER   -> DEBUG  
 * FINE    -> DEBUG  
 * CONFIG  -> INFO  
 * INFO    -> INFO  
 * WARNING -> WARN  
 * SEVERE  -> ERROR  

# Questions and Feedback  
Please email all questions and feedback to dataconnect-support@cisco.com        
