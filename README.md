# device-sdk-java

iotsp device sdk in Java

* System Requirement <br>
  The system requires a Java JDK (not just a JRE) installed, version 6 or higher (to check, use java -version) and JAVA_HOME set properly.<br>
  The java library dependencies are managed by the gradle tool which is included in the package under the directory `gradle/wrapper`.
  The gradle tool will download the dependencies automatically from well-known java library repositories or website specified in file `build.gradle`.<br> To get a detailed list of libraries in use, refer to file `build.gradle`  and section `dependencies`.<br>
  Users could also preinstall the depencies manually but it's less convenient and not recommended.
  

* Build Howto <br>
  The system is using the gradle wrapper as the build tool.<br>
  After a successful build, `./build/libs/device-sdk-java-1.0.0.jar` will be generated.<br>
  To build the system, use 
```
      ./gradlew build
```

* SDK configurations<br>
  The device SDK could work on either device mode or gateway mode (gateway model is not supported
  right now). In device mode, the device app registers the device info to the cloud directly using device SDK.
  In Gateway mode, a gateway app registers the gateway info to the cloud, and the gateway works as a 
  broker, collecting messages from device side over mqtt etc, and posting them to the cloud.

  The mode change could be done by changing the SDK configurations.
  The SDK configuration file is `config/sdkConfig.json`.
  The following are the descriptions of the data field,
```
      "Server"                    /* Registration server FQDN */ 
      "CACertPath"                /* java trustStore Path */           
      "VerifyServer"              /* SSL server verification enabled, 0 - Disable / 1 - Enable */
      "DeviceConnector"           /* Device Connnector type - http or mqtt */ 

      "logFilePath"               /*  Log file path */ 
      "loggingLevel"              /*  Log level */
      "gatewayEnabled"            /*  gateway mode, 0 - Disable / 1 - Enable*/

```

* Code Structure and APIs<br>
  The SDK core code are available at `src/main/java/com/cisco/devicesdk/core/`.<br>
  The major SDK interface functions are:
```Java
      // The system supports uuid only and uuid/macAddress/deviceSerialNumber device registration. 
      thing = new IotspThing(uuid, mac, deviceSerialNum);
      thing.post(message); 
      thing.close(); 
```

* Sample Applications<br>
  The sample apps are available at `src/main/java/com/cisco/devicesdk/sample/`.<br>
  The sample tests are available at `src/test/java/`.<br>
  Before the sample tests are executed, make sure that the server in `config/sdkConfig.json` is properly set, <br>
  and that the device uuids in the `SampleBasic.java` and `data/mockdata.txt` are claimed in the cloud.<br>
  To run the sample test, use
```
  ./gradlew sampleTest
```

* JDK logging<br> 
  The system has the following log levels
```
      SEVERE (highest value)
      WARNING
      INFO
      CONFIG
      FINE
      FINER
      FINEST (lowest value)
```
  
  The logging level could be changed in `config/logging.properties`.
