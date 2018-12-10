# Changelog
All notable changes to this project will be documented in this file.   

## December 9, 2018   
### Changed  
* Fix toString method of MQTTAttribute to fix an MQTT close sequence by updating to iotsp-device-sdk-common-0.3.7.1-SNAPSHOT.jar.
* Update all 3 applications with latest iotsp-device-sdk-common jar.  
* Update launch and commandlineoption parser for latest IOx versions in applications iotsp-batching and iotsp-batching-multi-dcclient.  

## October 9, 2018    
### Changed   
* Update launch and commandlineoption parser for latest IOx versions in applications iotsp-edge-rule-engine.     

## July 10,2018    
### Changed   
* Fix log for device Id in set and create device command in SDK [iotsp-device-sdk-common-0.3.7.0-SNAPSHOT.jar].       

## June 27,2018
### Changed
* Added new version of MQTT client library to the SDK [iot-mqtt-client-edge-1.0.0.1-SNAPSHOT.jar]. 
  Supports adding telemetry data to the messages (Based on application configuration parameter]
* Added new version of Device sdk library to the SDK [iotsp-device-sdk-common-0.3.7.0-SNAPSHOT.jar]. 
  Supports reading telemetry data config flag.

## June 13,2018

### Changed
* Added new version of MQTT client library to the SDK [iot-mqtt-client-edge-1.0.0.0-SNAPSHOT.jar]. 
  Changed the way mqtt clients are instantiated. Client id is mandated during instantiation of the MQTT client. 
  This library is NOT backward compatible.
* Added new sample application [iotsp-batching-multi-dcclient] which show cases the use of above mentioned library.

## June 12, 2018    

### Changed  

* Added updated libraries iot-mqtt-client-edge-0.3.4.0-SNAPSHOT.jar and iotsp-device-sdk-common-0.3.6.0-SNAPSHOT.jar.     
* supports setting the following parameters through package_config.ini (which will be available for application to configure
  during deployment through portal. "mqtt_request_timeout_ms(equivalent to client request timeout in MQTT") and "mqtt_max_msgs_in_flight(used to control number of messages in 
  flight". They can be added to application section of package_config.ini .
* Updating the batching application with new libraries


## May 22, 2018    

### Changed  

* Update iotsp-edge-rule-engine with latest libraries supporting iotsp-device-sdk-common-0.3.5.0-SNAPSHOT.jar.    

## May 21, 2018    

### Changed  

* Tagged older versions to version_4_21may2018
* Support envelope message type, where actual users payload and telemetry details could be packaged together and sent to DCM. 
  * Also adding "send_message_in_envelope" flag support to control sending messages in envelope or not. 
* Adding new parameters like name, serial no, uuid to gateway attributes. 
* Fixing the issue in obtaining device attributes 

### Added  

* iotsp-device-sdk-common-0.3.5.0-SNAPSHOT.jar - 
  * new library supporting the above functionality 

### Removed  


## March 21, 2018    

### Tag - version_4_21May2018

### Changed   

* Example applications iotsp-edge-rule-engine and iotsp-batching updated to use latest libraries.   

## March 19, 2018    

### Removed

* Change log from README.md.  

### Added   

* CHANGELOG.md tracking all the changes to the project.   
* Link to CHANGELOG.md in README.md.   


## March 6, 2018    

### Changed  

* README.md - Added change log to README.md for iotsp-device-sdk-common-0.3.3.0.jar.

### Added  

* iotsp-device-sdk-common-0.3.3.0.jar - 
  * API to remove the supported commands from the GwConfigCache. Once the command is removed, GwConfigCache will throw an exception when command of that type is used to update cache. Users could add the command back using addSupportedCommandToCache. 
  * Corresponding javadoc in iotsp-device-sdk-common-0.3.3.0-SNAPSHOT-javadoc.jar.       

### Removed  
* iotsp-device-sdk-common-0.3.2.0-SNAPSHOT-javadoc.jar
* iotsp-device-sdk-common-0.3.2.0-SNAPSHOT.jar 


## Mar 5, 2018   

### Tag - version_3_06Mar2018

### Changed   

* README.md - Added change log to README.md for iotsp-device-sdk-common-0.3.2.0.jar.    
* iotsp-device-sdk-common-0.3.2.0-SNAPSHOT.jar - 
  * Updating the configuration/command version in GwConfigCache when an empty device list is sent with the command "set_device".
  * Intializing the device connected state, device reachable state and device admin state to UNKNOWN instead of null.     

### Removed  

* iot-mqtt-client-edge-0.3.0.4-SNAPSHOT-javadoc.jar  
* iotsp-device-sdk-common-0.1.0.8-SNAPSHOT-javadoc.jar   
* iot-mqtt-client-edge-0.3.0.4-SNAPSHOT.jar   
* iotsp-device-sdk-common-0.1.0.8-SNAPSHOT.jar   
* iotsp-device-sdk-common-0.3.0.0-SNAPSHOT.jar    


## Feb 18, 2018      

### Tag - version_2_05Mar2018   

### Added   

* iotsp-device-sdk-common-0.3.0.0-SNAPSHOT.jar - 
  * Support for new command set_devices. Support QOS and persistence mode for MQTT client. 
  * Add confId in ack.    


## Feb 14, 2018     

### Changed  

* README.md - Update change log   

### Added   

* iot-mqtt-client-edge-0.3.1.0-SNAPSHOT.jar - 
  * Ability to publish messages at user defined labels which convert to Event Type in IBM. Refer iot-mqtt-client-edge-0.3.1.0-SNAPSHOT-javadoc.jar for details. 
  * Fixed - Max In Flight message exceeded, "Too Many Publish In Progress Exception". 
  * Adding support for configuring QOS and persistence.        


## Jan 31, 2018    

### Tag - version_1_14Feb2018   

### Changed   

* README.md - Update change log and add links for javadoc.   
* iotsp-batch-store-manager-0.3.0.8-SNAPSHOT.jar - 
  * Remove all files under src/sampleApp/java.
  * Added java doc.
  * Update multi gateway logic in CustomMQTTSSLClient.   
* iot-mqtt-client-edge-0.3.0.4-SNAPSHOT.jar - 
  * Added javadoc.
  * Update multi gateway logic in CustomMQTTSSLClient.    
* iotsp-device-sdk-common-0.1.0.8-SNAPSHOT.jar -  Added javadoc.      
* Separated out jars for libraries and their javadocs into libs/lib-jars and libs/javadoc-jars respectively.   
* Updated build.gradle for iotsp-batching and iotsp-edge-rule-engine projects for new location of jars.   
* Changed launch file for latest iotsp-batching and iotsp-edge-rule-engine jar.   
* Updated package.tar.gz for iotsp-batching and iotsp-edge-rule-engine jar.   

### Added  

* Java doc for all libraries under libs/javadoc-jars.    

### Removed   

* Files removed from iotsp-batch-store-manager-0.3.0.8-SNAPSHOT.jar - 
  * ApplicationCommandLineOptions.java
  * CommandLineOptionParser.java
  * BatchToCloudClient.java
  * BatchManagerSampleAppHBR.java
  * BatchManagerSampleAppHBRRS232.java
  * SpecialMessage.java
  * BatchToCloudClientHBR.java
  * SIUDecoder.java
  * SerialRS232Implementation.java
  * BatchManagerSampleApp.java
  * BatchManagerSampleAppAmsterdam.java 
  * SerialRS232ImplementationAms.java.   


## Jan 22, 2018   

### Changed   

* Update build.gradle in iotsp-batching and iotsp-edge-rule-engine projects for latest jars.    


## Jan 17, 2018    

# Added   

* iotsp-device-sdk-common-0.1.0.8-SNAPSHOT.jar, iotsp-device-sdk-common-0.1.0.8-SNAPSHOT-javadoc.jar -  Added javadoc.    

## Jan 16, 2018   

### Changed  

* Updated iot-mqtt-client-edge-0.3.0.4-SNAPSHOT.jar with java doc.    


## Nov 22, 2017   

### Added      

* Initial Commit      







