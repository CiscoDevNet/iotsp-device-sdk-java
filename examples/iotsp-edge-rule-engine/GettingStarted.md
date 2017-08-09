# iot-edge-rule-engine

## Overview
The functionality of this app consists of three parts: 
- Data extraction: In terms of methods to extract data, this app supports both reading messages from a local file and receiving sensor data from a Raspberry Pi. It is also possible to use other technologies to retrieve data.
- Data processing via rule engine: The rule engine takes in each message and applies a sequence of customizable operations (rules).
- Data transmission from gateway to cloud: MQTT client establishes a connection between the gateway and the cloud to output results in the cloud.   

## Pre-requisites  

- Install JDK, Version 8 is recommended 

- To extract sensor data from Raspberry Pi, then make sure the Raspberry Pi runs the following code in this repository under Pi_senseHat 
https://github.com/CiscoDevNet/iot-developer-demo-kit-python.git 


## Sample Application  

Take a look at the sample application under /src/main/java/com/cisco/iot/swp/edge/app/RESampleApp.java, showing some of the the rule engine's common usages. This getting started guide provides a walkthrough of the RESampleApp. At the end of the process, the directory executed should be the IOxPackage directory, and the structure is shown below:
```  
├── genericData.data			# This file contains the format for sample messages to read from a file instead of from a Raspberry Pi.  
├── launch.sh				# This file is used in the IOxPackage to launch the jar file. This file is modified when changing the command-line options for the jar to run.
├── logging.properties			# The logging.properties file required for java.util.logging
├── package.yaml			# File required for IOxPackaging  
├── <Jar Name>.jar			# The final jar file created by the application
└── package_config.ini			# This file contains the important config variable which can be modified during deployment through IoT DataConnect portal. Everything under [_cisco_mqtt_attributes] is modified by the portal only.
 
```  

### Code Walk Through for the Sample Application:
1. **Command line options**    
The sample application takes up to 5 command line options which are listed below:

| Command line Option      | Argument      | Use  | 
|:-------------:|:-------------:|:-----:|
| -h | none | Help options | 
| -d | file_name  |   The .data file to read messages in from a file | 
| -c | file_name    |   The package_config.ini file which contains all the necessary parameters required to run the IOx app. | 
| -s | none    |   Send data to mqtt broker in IoT DataConnect cloud using the mqtt client. | 
| -p | none | Post data received from IoT DataConnect cloud to Raspberry Pi. |   



2. **Message Formats and Methods to Retrieve Data from Messages**<br />
As was introduced in the overview, the sample application supports two ways to retrieve data, which could be sent to cloud later:<br />
 - (1) **Read from a local file:**<br />
	- (1.1)Message Format: The genericData.data in IOxPackage provides the way to declare messages to be sent to the cloud. The format of the file is
```
<deviceName>;<Delay between Messages>;<JSON message>
```
For example, to read from .data file, the run command can be modified in the launch.sh in IOxPackage to be
```
java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-<version>-SNAPSHOT-all.jar -d genericData.data
```
Other flags such as -s and -p can be used in the combination of this command as well. <br />  	

  - (1.2)Methods to Call: processDataFromFile. This function takes in the data file, reads in the message in the file line by line, and processes each message read in. The process of reading in data can be repeated at certain frequency, which is set through function **Thread.sleep(Integer.parseInt(sleep))**. To send data to the cloud, initialize a DCClient by calling the function **initDCClient()**. <br />

 - (2) **Read sensor data from Raspberri Pi:**<br /> 
	- (2.1)Message Format: If the users have a Raspberry Pi connected to some gateway, then note down the ip address of the Raspberry Pi. The sensehat app, mentioned in the second point of the prerequisite, runs a sever on Raspberry Pi and extracts sensor data or posts data to the Raspberry Pi. The following APIs are supported:
```
http://<ip>:<port>/sensehat/temperature
http://<ip>:<port>/sensehat/humidity
http://<ip>:<port>/sensehat/pressure
http://<ip>:<port>/sensehat/magnetometer
http://<ip>:<port>/sensehat/accelerometer
http://<ip>:<port>/sensehat/gyroscope
http://<ip>:<port>/sensehat/ping
``` 
Note that the default port is 5000.  
  - (2.2)Methods to call: processDataFromHttpClient. This function starts with establishing a connection between the gateway and the device by calling the function **initHTTPClient()**. To post data to the device, initialize an object of HTTPClientPost by calling **initHTTPClientPost()**; to send data from gateway to the cloud, initialize an object of DCClient by calling **ICloudConnectClient()**. The frequency of polling data from the device is defined in the application section of the config file. After that is set, data can be retrieved from Raspberry Pi by calling the function **getObservationUsingHTTPClient()**. The rest of the procedure is the same as that of processing data from a file. <br />

3. **Rule Engine**   
Main methods regarding the Rule Engine in the sample file are explained below:  
```    
/*Instantiate the rule engine object*/
/*If a configuration file is not provided, then the default configuFile will be used*/
RuleEngine ruleProcessor = new RuleEngine(configFile);

/*EFFECTS: passes each message through ProcessData method in the rule engine to get a list of actions generated after applying the rule on the message, where the deviceName is the name of the device as defined in the IoT DataConnect portal. The message is a json string of the message received from Raspberry Pi or read from the file.*/
/*REMessage has the topic, payload and destination as modified by the rule. Refer *processDataFromHttpClient()* or *processDataFromFile()* to see usage.*/
List<REMessage> messages = ruleProcessor.ProcessData(deviceName, message);
```    

4. **MQTT Client**     
Main methods regarding the in-built MQTT client in the sample file are explained below:
```    
/*EFFECTS: client object instantiation*/ 

/*To use the already provided implementation, initialize the ICloudConnectClient as*/
ICloudConnectClient dcClient = new MQTTClientEdge();

/*To use a custom MQTT implementation, implement the "ICloudConnectClient" interface and then initialize the custom object as follows:*/
ICloudConnectClient dcClient = new SomeCustomMQTTImplementation();

/*EFFECTS: initializes the client with Properties and MqttCallbackExtended, where props is a properties object which reads the package_config.ini to populate mqtt credentials. The variable props is populated using an in-built method *ConfigHelper.parseFile(new File(configFile))* where configFile is the package_config.ini file.
*/
dcClient.init(props, callback);

/*EFFECTS: establishes the mqtt connection from the sample app to IoT Data Connect Cloud */
dcClient.connect();

/*EFFECTS: publishes 'msg' on 'topic' for mqtt client in gateway to broker in IoT DataConnect Cloud */
dcClient.publish(topic, msg);

/*EFFECTS: subscribes to 'topicToSubscribe' for mqtt client in gateway from broker in IoT DataConnect Cloud */
dcClient.subscribe(topicToSubscribe);
```  
