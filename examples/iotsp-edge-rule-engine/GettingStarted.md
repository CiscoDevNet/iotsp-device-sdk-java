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

Take a look at the sample application under [RESampleApp](src/main/java/com/cisco/iot/swp/edge/app/RESampleApp.java), showing some of the the rule engine's common usages. This getting started guide provides a walkthrough of the RESampleApp. At the end of the process, the directory executed should be the IOxPackage directory, and the structure is shown below:
```  
├── genericData.data			# This file contains the format for sample messages to read from a file instead of from a Raspberry Pi.  
├── launch.sh				# This file is used in the IOxPackage to launch the jar file. This file is modified when changing the command-line options for the jar to run.
├── logging.properties			# The logging.properties file required for java.util.logging
├── package.yaml			# File required for IOxPackaging  
├── <Jar Name>.jar			# The final jar file created by the application
└── package_config.ini			# This file contains the important config variable which can be modified during deployment through IoT DataConnect portal. Everything under [_cisco_mqtt_attributes] is modified by the portal only.
 
```  

## Telemetry data
Currently the SDK supports adding telemetry data to the messages. This can turned on by setting the application configuration parameter in package_config.ini to "true".
* The configuration parameter that can used to turn on the telemetry data and turn them off is : "include_meta_data_with_msg"
* When using the "CustomSSLMqttClient" part of SDK, the users do not need to do anything other than adding this flag into the package_config.ini under application section and set to "true" to obtain telemetry data. The CustomSSLMqttClient inherently checks the "include_meta_data_with_msg" flag and constructs the appropriate payload and topic (thats used to publish to DCM)
* When not using the "CustomSSLMqttClient" and using the regular  "MQTTClientEdge" part of SDK, users can use helper class to obtain the payload and topic to publish. 
```
  com.cisco.iot.swp.device.sdk.common.utils.getPublishTopic(String topic, boolean useTelemetry, boolean isBatch)

  com.cisco.iot.swp.device.sdk.common.utils.getPublishPayload(String payload, boolean useTelemetry, String topicToPublish, String label, String deviceId, int QOS, boolean isBatch)

https://github.com/CiscoDevNet/iotsp-device-sdk-java/blob/8f14c8eed1c033bb7904dc2ea57f9108ab55a7cb/examples/iotsp-edge-rule-engine/src/main/java/com/cisco/iot/swp/edge/app/RESampleApp.java#L543

```* When the "include_meta_data_with_msg" flag is set to true, the actual message payload is sent with the telemetry data to the destination.               
* For AMQP 0.9 the data can be expected in the following manner:
```
('Headers : ', {'dp_received_ts': u'1530143798459', 'sent_to_destination': u'1530143798632', 'route_version': u'v1', '__kinetic__app_sent_at': u'1530143798092', 'route': u'/v1/321:285961/json/dev2app/alertTemp', '__kinetic__dcm_received_at': u'1530143798229', 'gatewayId': u'321', 'message_direction': u'dev2app', 'tag': u'alertTemp', 'deviceId': u'285961', 'content_type': u'json', 'assetName': u'assetpi', 'accountId': u'853', 'device_id': u'285961'})
```
* For IBM destination the data can be expected in the following manner(envelope with original payload with extra object having telemetry details:
```
{
        "actual data" : "not touched"
        "__kinetic__": {
                "dcm_sent_at": "1530144426872",
                "dcm_received_at": "1530144426530",
                "app_sent_at": "1530144426286"
        }
}
```


### Code Walk Through for the Sample Application        	
---   
The **Sample Application** can be found at [RESampleApp](src/main/java/com/cisco/iot/swp/edge/app/RESampleApp.java). The sample application does the following things -   

 * Parse the command line options - 
 ```  
 ApplicationCommandLineOptions appOptions = CommandLineOptionParser.readOptionsFromCommandLine(args);
 ```   
 The command line options are defined in ApplicationCommandLineOptions and CommandLineOptionParser is used to parse these options. (Read more about it at [Command Line Options](#command-line-options)    
 
 * Parse the package_config.ini file to get the user and portal controlled input parameters for the application. Config file can be passed as an input command line option. If not provided in command line options, the package_config.ini in root directory of the project is taken by default. Read more about it at [Config file](#package_config-components-explanation).       
 ```  
 Properties props = ConfigHelper.parseFile(new File(appOptions.getConfigFile()));
 ```    
 
 * When deploying the application in IOx environment, make sure to call    
 ```   
 CreateCustomLogger.changeLogger(<Package_config.ini parsed properties>);
 ```    
 This will ensure the log goes to a file/console and file rotation is enforced to prevent log overflow in resource-limited IOx. Please keep "log_level_console" OFF.   
 
 * Initialize device and gateway attributes obtained from package_config.ini   
 ```  
 Map<String, DeviceAttributes> deviceAttributes = ConfigHelper.getDeviceDetailsFromConfig(props);
 GatewayAttributes gwAttributes = ConfigHelper.getGatewayDetailsFromConfig(props); 
 ```    
 
 * Initialize Rule Engine Processor based on Rule provided in package_config.ini. Refer [Rule Engine](#rule-engine) for more details.     
 ```   
 RuleEngine ruleProcessor = new RuleEngine(ConfigHelper.getRule(props));
 ```   
 
 * Initialize ICloudConnectClient depending on the command line option passed to the application. Please refer **initDCClient()** method to refer how to initialize this client. Read more about it at [MQTT Client](#mqtt-client).       
 ```    
 /*Declare the callback*/
 MqttCallbackExtended callback = new MqttCallbackExtended() {
      @Override
      public void connectionLost(Throwable cause) {
        logger.error("Lost MQTT connection... Handle the lost connection");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        logger.info("Message arrived on topic -> {} with payload {} ", topic, message);
        if (appOptions.isSendDataToDevice()) {
          sendDataToDevice(message);
        }
      }

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {
        // LOG.info("Delivery complete for message...");
      }

      @Override
      public void connectComplete(boolean reconnect, String serverURI) {
        LOG.info("Reconnection status={}, serverURI={}", reconnect, serverURI);

        // Re subscribing when reconnect=true is important otherwise the mqtt connection is lost
        // when establishing actual connection. It is important to re subscriber here as we have set
        // autoReconnect=true and cleanSession=true. So every time it reconnects subscribe info is
        // lost.
        if (reconnect) {
          subscribe();
        }
      }
    };
    dcClient = new MQTTClientEdge();
    logger.info("Initializing the Mqtt client with callback...");
    dcClient.init(props, callback);
    dcClient.connect();
    while (!dcClient.isConnected()) {
      logger.info("Waiting for the connection to established...");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        logger.error("exception during mqtt client initializtion {}.", e.getMessage());
      }
    }
 ```    

 * You can also find special messages like KeepAlive and Diagnostic messages being simulated and sent at a regular frequency of 30s. Check startSpecialMessageMonitoring() and sendSpecialMessageUpstream() methods for the implementation. Refer [SpecialMessage.java](src/main/java/com/cisco/iot/swp/edge/app/SpecialMessage.java) to see usage. Read more about it at [Special Message](#special-message).          
 	* *startSpecialMessageMonitoring()* starts a background thread which sends simulated data every 30s.    
 	* *sendSpecialMessageUpstream()* generates special simulated messages - KeepAliveMessage and DiagnosticMessage and sends them upstream on their appropriate topics using CustomSSLMqttClient.     
 
 * Refer **processDataFromFile()** for reading observations from a file and then sending individual messages immediately upstream. 
	* **Individual Message Sent Upstream** - The messages are sent immediately upstream to cloud using MQTTClientEdge. If there is network connectivity issue at the time of sending and the message was not delivered to cloud for some reason, the message is dropped.    
	```   
	dcClient.publish(topicToPublish, payload);
	```    
 * Refer **processDataFromHttpClient()** for reading observations from a rest client server running on Raspberri-pi. 
	* **Individual Message Sent Upstream** - The messages are sent immediately upstream to cloud using MQTTClientEdge. If there is network connectivity issue at the time of sending and the message was not delivered to cloud for some reason, the message is dropped.    
	```   
	dcClient.publish(topicToPublish, payload);
	```    
 Refer **sendMessageUpstream()** method to see how the messages are sent upstream.  

### **package_config components explanation**    
---   
```  
[_cisco_mqtt_attributes] - This section is modified by portal when application is deployed in gateway iusing portal. When using local IOx VM, application developer updates this section.
mqtt.broker : MQTT broker url with port 8883 for ssl connection. This value can be grabbed from portal.
mqtt.broker_ws : MQTT broker url with port 443 for web socket connection. This value can be grabbed from portal.
gw.id : Gateway ID as mentioned in portal.
gw.password : Gateway password as mentioned in portal.
gw.topic.observation : Gateway observation topic as mentioned in portal.
gw.topic.command :  Gateway command topic as mentioned in portal.

device.number : Number of devices connected to portal.

device1.id : Device ID as mentioned in portal.
device1.tag : Device name as mentioned in portal.
device1.topic.observation : Device observation topic as mentioned in portal.
device1.topic.command : Device command topic as mentioned in portal.

[application] - Application developer updates this section  
polling.interval : The interval at which data is polled from device connected to gateway.
connection.timeout : MQTT connection timeout in seconds. Minimum timeout - 120s
connection.attempts : Number of times MQTT connection is tried. -1 value for infinite re trial strategy
connection.retry.interval.secs : MQTT re trial connection interval
mqtt.connection.mode : MQTT connection mode. Possible values - ssl, wss, tcp

device1.ip : The ip of the device connected to gateway.
device1.port : The port on which application is running on the device(connected to gateway)

[logging] - Application developer updates this section
log_file_name: Log file name. <name>.log
log_level_file:  log level for file. Possible values - FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF 
log_level_console:  log level for console. Possible values - FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF

[rule_processor] - Application developer updates this section
rule_set: The DSL rule to be applied on messages coming from gateway or asset.

```
	
### **Command line options**    
---   
The sample application takes up to 5 command line options which are listed below:

| Command line Option      | Argument      | Use  | 
|:-------------:|:-------------:|:-----:|
| -h | none | Help options | 
| -d | file_name  |   The .data file to read messages in from a file | 
| -c | file_name    |   The package_config.ini file which contains all the necessary parameters required to run the IOx app. | 
| -s | none    |   Send data to mqtt broker in IoT DataConnect cloud using the mqtt client. | 
| -p | none | Post data received from IoT DataConnect cloud to Raspberry Pi. |      

Their overview can be found in [ApplicationCommandLineOptions](src/main/java/com/cisco/iot/swp/batch/app/ApplicationCommandLineOptions.java) and [CommandLineOptionParser](src/main/java/com/cisco/iot/swp/batch/app/CommandLineOptionParser.java).  

### **Message Formats and Methods to Retrieve Data from Messages**<br />  
---   
As was introduced in the overview, the sample application ( [RESampleApp](src/main/java/com/cisco/iot/swp/edge/app/RESampleApp.java) ) supports one way to retrieve data, which could be sent to cloud later:<br />
 - (1) **Read from a local file:**<br />
	- (1.1)*Message Format*: The genericData.data in IOxPackage provides the way to declare messages to be sent to the cloud. The format of the file is
```
<deviceName>;<Delay between Messages>;<JSON message>
```
For example, to read from .data file, the run command can be modified in the launch.sh in IOxPackage to be
```
java -Xmx160m -Djava.net.preferIPv4Stack=true -Djava.util.logging.config.file=logging.properties -jar iotsp-batch-store-manager-<version>-SNAPSHOT-all.jar -d genericData.data
```
Other flags such as -s and -p can be used in the combination of this command as well. <br />  	

  - (1.2)*Methods to Call*: processDataFromFile(). This function takes in the data file, reads in the message in the file line by line, and processes each message read in. The process of reading in data can be repeated at certain frequency, which is set through function **Thread.sleep(Integer.parseInt(sleep))**. To send data to the cloud, initialize a CustomSSLMqttClient by calling the function **initDCClient()**. <br />    
  
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

### **Special Message**  
---   
There are 3 kinds of Special Messages being supported right now:    
 * KeepAlive - Keep-alive messages are generated by Apps in gateways. Apps control devices, and are aware of device state as well as configuration - and this information is reported periodically to data pipeline via keep-alive messages.    
 * Diagnostic - Diagnostic messages contain information about what when wrong with device/gateway.    
 * Ack/Nack	- Acknowledgement message for command received from IBM app.   
 Refer [SpecialMessage](src/main/java/com/cisco/iot/swp/edge/app/SpecialMessage.java) to see usage.     
	```  
	/*Initialize SpecialMessage object for sending KeepAlive, Diagnostic and Ack Messages. */    
	SpecialMessage specialMessageSend = new SpecialMessage(appOptions.isSendDataToCloud(), customMqttClient); 
	
	/*SpecialMessage has following methods available to send KeepAlive, Diagnostic and Ack Messages  */
	/*Send Keep Alive messages*/
	public void sendKeepAlive() throws IoTDeviceSDKCommonException, IoTEdgeDcClientException
	
	/*Send Diagnostic messages*/
	public void sendDiagnosticMessage() throws IoTDeviceSDKCommonException, IoTEdgeDcClientException
	
	/*Send Ack/Nack depending on messageStatus for gateway info provided in setDevice*/
	public void sendAckMessage(boolean messageStatus, DeviceConfiguration<GwConnectedDevice> setDevice) throws Exception
	```        

### **Rule Engine**   
Main methods regarding the Rule Engine in the sample file are explained below:  
```    
/*Instantiate the rule engine object*/
/*If a configuration file is not provided, then the default configuFile will be used*/
RuleEngine ruleProcessor = new RuleEngine(configFile);

/*EFFECTS: passes each message through ProcessData method in the rule engine to get a list of actions generated after applying the rule on the message, where the deviceName is the name of the device as defined in the IoT DataConnect portal. The message is a json string of the message received from Raspberry Pi or read from the file.*/
/*REMessage has the topic, payload and destination as modified by the rule. Refer *processDataFromHttpClient()* or *processDataFromFile()* to see usage.*/
List<REMessage> messages = ruleProcessor.ProcessData(deviceName, message);
 for (REMessage msg : messages) {
     //take action
 }
```    

### **MQTT Client**     
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
