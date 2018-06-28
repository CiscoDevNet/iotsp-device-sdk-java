# iotsp-batching     

## Overview
The functionality of this app consists of four parts: 
- Data extraction: In terms of methods to extract data, this app supports both reading messages from a local file and receiving sensor data from a Raspberry Pi. It is also possible to use other technologies to retrieve data.
- Data processing via rule engine: The rule engine takes in each message and applies a sequence of customizable operations (rules).  
- Data Storage and Compression: Based on the rule applied to each message above, each message can be either immediately sent upstream or can be added to a batch. Once a message is added to a batch it will be grouped together with other message based on batching policy and then compressed. After that the compressed batch will be sent upstream using next ubit. 
- Data transmission from gateway to cloud: MQTT client establishes a connection between the gateway and the cloud to send results in the cloud.   

## Pre-requisites  

- Install JDK, Version 8 is recommended 

- To extract sensor data from Raspberry Pi, then make sure the Raspberry Pi runs the following code in this repository under Pi_senseHat 
https://github.com/CiscoDevNet/iot-developer-demo-kit-python.git 


## Sample Application  

In this application, there are multiple MQTT clients instantiated (usually one per thread). In this example one mqtt client is used for publishing observations, other for handling the batches and another for publishing the special messages. 

Take a look at the sample application under [BatchManagerSampleApp](src/main/java/com/cisco/iot/swp/batch/re/app/BatchManagerSampleApp.java), showing some of the the batching, compression and rule engine's common usages. This getting started guide provides a walk through of the BatchManagerSampleApp. At the end of the process, the directory executed should be the IOxPackage directory, and the structure is shown below:
```  
├── genericData.data			# This file contains the format for sample messages to read from a file instead of from a Raspberry Pi.  
├── launch.sh				# This file is used in the IOxPackage to launch the jar file. This file is modified when changing the command-line options for the jar to run.
├── logging.properties			# The logging.properties file required for java.util.logging
├── package.yaml			# File required for IOxPackaging  
├── <Jar Name>.jar			# The final jar file created by the application
└── package_config.ini			# This file contains the important config variable which can be modified during deployment through IoT DataConnect portal. Everything under [_cisco_mqtt_attributes] is modified by the portal only.
 
```  

## Batching Policy  
For batching and storing the user defines the policies which decide the minimum batch size maintained before it can be compressed and sent. The policies provided are -   
* MAX_NUMBER_OF_ELEMENTS - The number of elements in a batch before it is declared to be ready for compression. If this policy is not defined then a default size policy is taken. (Value - TBD)    
* MAX_SIZE - The size of batch before it is declared to be ready for compression.     
* MAX_TIMEOUT - The timeout of batch before it is declared to be ready for compression. The batch time starts from it's 1st message and is reinitialized after a clear of batch is called.    
 
## Batch Types    
There are 2 types of messages that are received by the library -  
* To be batched according to policy defined     
* To be batched for immediate send - These messages are those which had to be sent immediately but couldn't be processed due to connection errors or some other reasons. These are to be sent as soon as the prevailing condition is resolved.       

## Telemetry data    
Currently the SDK supports adding telemetry data to the messages. This can turned on by setting the application configuration parameter in package_config.ini to "true".  
* The configuration parameter that can used to turn on the telemetry data and turn them off is : "include_meta_data_with_msg"    
* When using the "CustomSSLMqttClient" part of SDK, the users do not need to do anything other than adding this flag into the package_config.ini under application section and set to "true" to obtain telemetry data. The CustomSSLMqttClient inherently checks the "include_meta_data_with_msg" flag and constructs the appropriate payload and topic (thats used to publish to DCM)   
* When not using the "CustomSSLMqttClient" and using the regular  "MQTTClientEdge" part of SDK, users can use helper class to obtain the payload and topic to publish.
```
  com.cisco.iot.swp.device.sdk.common.utils.getPublishTopic(String topic, boolean useTelemetry, boolean isBatch)

  com.cisco.iot.swp.device.sdk.common.utils.getPublishPayload(String payload, boolean useTelemetry, String topicToPublish, String label, String deviceId, int QOS, boolean isBatch)

https://github.com/CiscoDevNet/iotsp-device-sdk-java/blob/ee8bdaa64910006dc7d9e43ece1b5892cfb87f30/examples/iotsp-batching-multi-dcclient/src/main/java/com/cisco/iot/swp/batch/re/app/BatchManagerSampleApp.java#L388

```
* When the "include_meta_data_with_msg" flag is set to true, the actual message payload is sent with the telemetry data to the destination. 
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

## Code Walk Through for the Sample Application:  

### **Sample Application**        	
---   
The **Sample Application** can be found at [BatchManagerSampleApp](src/main/java/com/cisco/iot/swp/batch/re/app/BatchManagerSampleApp.java). It uses [BatchToCloudClient](src/main/java/com/cisco/iot/swp/batch/controller/BatchToCloudClient.java) class for instantiating and using BatchManager, continuously polling BatchManager for compressed batch availability and for sending the available compressed batches upstream. The sample application does the following things -   

 * Parse the command line options - 
 ```  
 ApplicationCommandLineOptions appOptions = CommandLineOptionParser.readOptionsFromCommandLine(args);
 ```   
 The command line options are defined in BatchManagerAppOptions and OptionsParser is used to parse these options. (Read more about it at [Command Line Options](#command-line-options)    
 
 * Parse the package_config.ini file to get the user and portal controlled input parameters for the application. Config file can be passed as an input command line option. If not provided in command line options, the package_config.ini in root directory of the project is taken by default. Read more about it at [Config file](#package_config-components-explanation).       
 ```  
 Properties props = ConfigHelper.parseFile(new File(appOptions.getConfigFile()));
 ```    
 
 * When deploying the application in IOx environment, make sure to call    
 ```   
 CreateCustomLogger.changeLogger(<Package_config.ini parsed properties>);
 ```    
 This will ensure the log goes to a file/console and file rotation is enforced to prevent log overflow in resource-limited IOx. Please keep "log_level_console" OFF.   
 
 * Initialize Rule Engine Processor based on rule provided in package_config.ini   
 ```   
 RuleEngine ruleProcessor = new RuleEngine(ConfigHelper.getRule(props));
 ```   
 Read more about rule engine in [Rule Engine](#rule-engine).   
 
 * Initialize Compression utility required by Batching Unit as given below. Read more about it in [Compression](#compression).    
 ```      
 ICompressionUtils compressionUtils = new CompressionUtilsZlib();   
 ```     
 
 * Initialize gateway and device parameters from package-config.ini. The Properties are obtained from the package_config.ini as explained above.    
 ```   
 	// Initialize gateway and device parameters
    Map<String, DeviceAttributes> deviceAttributes = ConfigHelper.getDeviceDetailsFromConfig(props);
    GatewayAttributes gwAttributes = ConfigHelper.getGatewayDetailsFromConfig(props);
    // Important step = Create the batch gateway topic and add it to properties obtained earlier from package_config.ini
    props.setProperty(BaseConstantsUserParams.GATEWAY_OBV_BATCH_TOPIC,
        ConfigHelper.getBatchZipTopicForGateway(gwAttributes));
 ```   
 
 * Initialize ICloudConnectClient depending on the command line option passed to the application. Please refer **initDCClient()** method to refer how to initialize this client. Read more about it at [MQTT Client](#mqtt-client).   
 ```    
 	/*Declare the callback*/
 	MqttCallbackExtended callback = new MqttCallbackExtended() {
      @Override
      public void connectionLost(Throwable cause) {
        LOG.error("Lost MQTT connection... Handle the lost connection");
      }

      @Override
      public void messageArrived(String topic, MqttMessage message) throws Exception {
        LOG.info("Message arrived on topic -> {} with payload {} ", topic, message);
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
        subscribe();
      }
    };
    LOG.info("Initializing the Mqtt client...");
    String clientId = gwAttributes.getGatewayId() + "-obsv-client"; 
    dcClient = new MQTTClientEdge(clientId);
    LOG.info("Initializing the Mqtt client with callback...");
    dcClient.init(props, callback);
    dcClient.connect();
    while (!dcClient.isConnected()) {
      LOG.info("Waiting for the connection to established...");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        LOG.error("exception during mqtt client initializtion {}.", e.getMessage());
      }
    }
    LOG.info("Done Initializing the Mqtt client...");  
 ```         
 
 * You can also find special messages like KeepAlive, Ack and Diagnostic messages being simulated and sent at a regular frequency of 30s. Check startSpecialMessageMonitoring() and sendSpecialMessageUpstream() methods for the implementation. Refer [SpecialMessage.java](src/main/java/com/cisco/iot/swp/batch/re/app/SpecialMessage.java) to see usage. Read more about it at [Special Message](#special-message).          
 	* *startSpecialMessageMonitoring()* starts a background thread which sends simulated data every 30s.    
 	* *sendSpecialMessageUpstream()* generates special simulated messages - KeepAliveMessage, Ack and Diagnostic and sends them upstream on their appropriate topics using ICloudConnectClient.     
 
 * To use Batch manager for batching, storing and forwarding messages see [BatchToCloudClient](src/main/java/com/cisco/iot/swp/batch/controller/BatchToCloudClient.java). To use this class, initialize as the code shown below -       
 ```     
 /*Initialize BatchManager helper class*/
 String clientId = gwAttributes.getGatewayId() + "-batch-xyz-mqtt-client";
 BatchToCloudClient batchClient = new BatchToCloudClient(props, compressionUtils, clientId);
 ```     
 Refer [Batch Manager](#batch-manager) for more details.   

 * Refer **processDataFromFile()** for reading observations from a file or **processDataFromHttpClient()** to read messages from Raspberri-pi and then either batching messages and compressing them to send upstream or send individual messages immediately upstream. The Rule mentioned in package_config.ini decides whether a message will be batched or not. When a message/observation is processed -     *batchOrSendMsgBasedOnRule()* is called to act upon the message.   
	* **Batched message** - If the message was supposed to be batched, then the message is converted into *BatchMessage* using *constructBatchMessage()*. Once the *BatchMessage* is created, this message is added to the buffer using BatchToCloudClient explained above.   
	```    
	batchClient.putMessageInBatch(BatchMessage msg)
	```     
	* **Individual Message Sent Upstream** - If the message is not supposed to be batched, then it is sent immediately upstream to cloud using ICloudConnectClient. If there is network connectivity issue at the time of sending and the message was not delivered to cloud for some reason, the message can be added to batch to be re-delivered later.  
	```   
	dcClient.publish(topic, payload);
	```    

## package_config components explanation    
  
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

batch_policy.size_bytes : The size in bytes of a batch before it is declared to be ready for compression.
batch_policy.num_msg : The number of elements in a batch before it is declared to be ready for compression.
batch_policy.timeout_in_sec: The timeout in seconds of a batch before it is declared to be ready for compression.

include_meta_data_with_msg : true/false , When true this flag will add meta data like (telemetry details: timestamp when the message is sent from application to DCM)

[logging] - Application developer updates this section
log_file_name: Log file name. <name>.log
log_level_file:  log level for file. Possible values - FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF 
log_level_console:  log level for console. Possible values - FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF

[rule_processor] - Application developer updates this section
rule_set: The DSL rule to be applied on messages coming from gateway or asset.

```	

### Command line options  
    
The sample application takes up to 5 command line options which are listed below:

| Command line Option      | Argument      | Use  | 
|:-------------:|:-------------:|:-----:|
| -h | none | Help options | 
| -d | file_name  |   The .data file to read messages in from a file | 
| -c | file_name    |   The package_config.ini file which contains all the necessary parameters required to run the IOx app. | 
| -s | none    |   Send data to mqtt broker in IoT DataConnect cloud using the mqtt client. | 
| -p | none | Post data received from IoT DataConnect cloud to Raspberry Pi. |      


Their overview can be found in [ApplicationCommandLineOptions](src/main/java/com/cisco/iot/swp/batch/commandline/ApplicationCommandLineOptions.java) and [CommandLineOptionParser](src/main/java/com/cisco/iot/swp/batch/commandline/CommandLineOptionParser.java).  

### Message Formats and Methods to Retrieve Data from Messages   

As was introduced in the overview, the sample application supports two ways to retrieve data, which could be sent to cloud later:<br />
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

  - (1.2)*Methods to Call*: processDataFromFile(). This function takes in the data file, reads in the message in the file line by line, and processes each message read in. The process of reading in data can be repeated at certain frequency, which is set through function **Thread.sleep(Integer.parseInt(sleep))**. To send data to the cloud, initialize a ICloudConnectClient by calling the function **initDCClient()**. <br />

 - (2) **Read sensor data from Raspberri Pi:**<br /> 
	- (2.1)*Message Format*: If the users have a Raspberry Pi connected to some gateway, then note down the ip address of the Raspberry Pi. The sensehat app, mentioned in the second point of the prerequisite, runs a sever on Raspberry Pi and extracts sensor data or posts data to the Raspberry Pi. The following APIs are supported:
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
  - (2.2)*Methods to call*: processDataFromHttpClient(). This function starts with establishing a connection between the gateway and the device by calling the function **initHTTPClient()**. To post data to the device, initialize an object of HTTPClientPost by calling **initHTTPClientPost()**; to send data from gateway to the cloud, initialize an object of ICloudConnectClient by calling **initDCClient()**. The frequency of polling data from the device is defined in the application section of the config file. After that is set, data can be retrieved from Raspberry Pi by calling the function **getObservationUsingHTTPClient()**. The rest of the procedure is the same as that of processing data from a file. <br />

### Rule Engine    
Rule Engine is used for 2 purposes -   
 - Filtering or processing the input data.  
 - Adding messages to batch according to batch policy.  
  
```    
/*Instantiate the rule engine object*/
/*If a configuration file is not provided, then the default configuFile will be used*/
RuleEngine ruleProcessor = new RuleEngine(configFile);

/*EFFECTS: passes each message through ProcessData method in the rule engine to get a list of actions generated after applying the rule on the message, where the deviceName is the name of the device as defined in the IoT DataConnect portal. The message is a json string of the message received from Raspberry Pi or read from the file.*/
/*REMessage has the topic, payload and destination as modified by the rule. Refer *processDataFromHttpClient()* or *processDataFromFile()* to see usage.*/
List<REMessage> messages = ruleProcessor.ProcessData(deviceName, message);

Each message received in List<REMessage> contains a parameter which basically specifies if user wants to batch this particular message or not depending on the rule specified. So for example if the rule mentioned had following snippet:    
WHEN msg(assetpi.temperature) < 50 THEN { SEND TO "internal-rmq" AS_BATCH TOPIC "alertTemp" JSON assetpi scol }   
that means the message should be batched. Then using the getAsBatch() method in REMessage, we can determine if a message is to be batched or not. The following code explains the concept below:   
for (REMessage msg : messages) {
	if (msg.getAsBatch()) {
      // add message to batch
     } else {
     	// take action on message immediately
     }
 }
```    

### MQTT Client      

Main methods regarding the in-built MQTT client in the sample file are explained below:
```    
/*EFFECTS: client object instantiation*/ 

/*To use the already provided implementation, initialize the ICloudConnectClient as*/
String clientId = "gwid-testclient";
ICloudConnectClient dcClient = new MQTTClientEdge(clientId);

/*To use a custom MQTT implementation, implement the "ICloudConnectClient" interface and then initialize the custom object as follows:*/
ICloudConnectClient dcClient = new SomeCustomMQTTImplementation();

/*EFFECTS: initializes the client with Properties and MqttCallbackExtended, where props is a properties object which reads the package_config.ini to populate mqtt credentials. The variable props is populated using an in-built method *ConfigHelper.parseFile(new File(configFile))* where configFile is the package_config.ini file.
*/
dcClient.init(props, callback);

/*EFFECTS: establishes the mqtt connection from the sample app to IoT Data Connect Cloud */
dcClient.connect();

/*EFFECTS: publishes 'msg' on 'topic' for mqtt clientsendSpecialMessageUpstream in gateway to broker in IoT DataConnect Cloud */
dcClient.publish(topic, msg);

/*EFFECTS: subscribes to 'topicToSubscribe' for mqtt client in gateway from broker in IoT DataConnect Cloud */
dcClient.subscribe(topicToSubscribe);
```    

### Compression   

The Batch Manager comes with an in-built compression library. Most of the APIs use interface ICompressionUtils. If application developer want to utilize their custom compression library, just implement the interface ICompressionUtils. Or they can use in-built CompressionUtilsZlib() for compression.  
```   
ICompressionUtils compressionUtils = new CompressionUtilsZlib();
```    

### Batch Manager       

* To use Batch Manager APIs, refer [BatchToCloudClient](src/main/java/com/cisco/iot/swp/batch/controller/BatchToCloudClient.java). BatchToCloudClient is a helper class which initializes BatchManager and accepts the incoming messages by storing them in batches. BatchToCloudClient also starts  a background thread that checks if compressed batches are available and if yes, it sends them upstream to the cloud using ICloudConnectClient. Important methods in BatchToCloudClient -  
	* *checkBatchReady()* - This method checks if any compressed batch is ready. It starts a background thread which checks every 3s from BatchManager if a compressed batch is available. If yes, it sends it upstream by calling sendBatch(). This method calls getCompressedBatch() from BatchManager to get a compressed batch. getCompressedBatch() returns null if no compressed batch is available.      
	* *sendBatch()* - Depending on whether a ICloudConnectClient object is provided to this class as a constructor input, this method publishes the given compressed batch onto the gateway's batching topic or writes log to a file. The publish call is synchronized as the messages which are not being batched are also being sent upstream.  
	* *putMessageInBatch()* - This method takes a *BatchMessage* as input and adds it to the underlying batch buffer. It calls addMessageToBatch() method available in BatchManager which adds a batch to the underlying buffer and returns whether the addition was successful or not.    
	
### **Special Message**  
---   
There are 3 kinds of Special Messages being supported right now:    
 * KeepAlive - Keep-alive messages are generated by Apps in gateways. Apps control devices, and are aware of device state as well as configuration - and this information is reported periodically to data pipeline via keep-alive messages.    
 * Diagnostic - Diagnostic messages contain information about what when wrong with device/gateway.    
 * Ack/Nack	- Acknowledgement message for command received from IBM app.   
 Refer [SpecialMessage](src/main/java/com/cisco/iot/swp/batch/re/app/SpecialMessage.java) to see usage.     
	```  
	/*Initialize SpecialMessage object for sending KeepAlive, Diagnostic and Ack Messages. */    
	SpecialMessage specialMessageSend = new SpecialMessage(appOptions.isSendDataToCloud(), dcClient, gwAttributes,
        deviceAttributes);
	
	/*SpecialMessage has following methods available to send KeepAlive, Diagnostic and Ack Messages  */
	/*Send Keep Alive messages*/
	public void sendKeepAlive() throws IoTDeviceSDKCommonException, IoTEdgeDcClientException
	
	/*Send Diagnostic messages*/
	public void sendDiagnosticMessage() throws IoTDeviceSDKCommonException, IoTEdgeDcClientException
	
	/*Send Ack/Nack depending on messageStatus for gateway info provided in setDevice*/
	public void sendAckMessage() throws Exception
	```         
	
	
