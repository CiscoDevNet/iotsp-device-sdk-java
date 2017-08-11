# device-sdk-java

This directory contains sample applications and libraries for both the iotsp-rule-engine-edge and iotsp-batching. Detailed information about the 
rule engine and batching applications and libraries can be found in their respective directories under examples. 

## Directory Structure

### Pi-Sensehat
  - The Pi-Sensehat directory contains all the materials necessary to install the sensehat application on the raspberry pi. In this directory the readme contains instructions to install the application. The sensehat-app directory is to be installed into the home/pi/projects directory on the raspberry pi. 
  
### examples
  - The examples directory contains two subdirectories, iotsp-batching and iotsp-edge-rule-engine. These directories contain information on how to install and run the sample applications. They also provide APIs and documentation on different options the sample applications provide and use.
  
### libs
  - This directory contains libraries for the rule-engine, mqtt-client-edge, state-processing-dsl, and batching
    - iot-edge-rule-engine-0.1.0.0-SNAPSHOT.jar is the jar file for rule engine. This library has a dependency on mqtt-client-edge and state-processing-dsl
    - iot-mqtt-client-edge-0.1.0.0-SNAPSHOT.jar is the jar file for the mqtt client on edge. There are no other dependencies to any other libaries in this directory.   
    - iot-state-processing-dsl-0.6.1-SNAPSHOT.jar is the jar file for the state processing library. There are no other dependencies to any other libaries in this directory.  	
    - iotsp-batch-store-manager-0.1.0.0-SNAPSHOT.jar is the jar file for batching and compression. This library has a dependency on rule engine, mqtt client edge, and state processing dsl. 
