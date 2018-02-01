# device-sdk-java  

This repository contains Cisco's IoT device SDK written in Java. Device SDKs are the applications which run on Cisco's gateways and enable you to extract observations from client devices connected to Cisco's gateways and send the information to Cisco Kinetic Data Delivery Platform. The Cisco gateways provide the application users with internet connectivity, application running on gateway which helps in device management and a secure platform to connect to Cisco's cloud.  

To get started, clone this repository.

* [Overview](#overview)
* [How to use the Cisco IoT SDKs for Java](#how-to-use-the-cisco-iot-sdks-for-java)
* [Java Docs for Libraries](#java-docs-for-libraries)
* [Sample Application](#sample-application)  
* [Writing Rules](#writing-rules)  
* [Directory Structure](#directory-structure)
* [Support](#support)  

## Overview
This repository contains sample applications and libraries for  

| S.No          | Application Name     | Explanation  | 
|:-------------:|:--------------------|:------------|
| 1       | iotsp-edge-rule-engine | The iotsp-edge-rule-engine allows application developers to filter, process and send their data to different destinations. Sample project for [Rule Engine On Edge](examples/iotsp-edge-rule-engine)|  
| 2       | iotsp-batching | The iotsp-batching allows application developers to filter, process and send their data to different destinations. The observations being sent to Cisco's cloud platform can be batched and compressed to save network bandwidth or it can be sent as it is immediately. Sample project for [Batching](examples/iotsp-batching)| 
| 3      | Pi-sensehat | The Pi-Sensehat directory contains all the materials necessary to install the sensehat application on the raspberry pi. In this directory the readme contains instructions to install the application. The sensehat-app directory is to be installed into the home/pi/projects directory on the raspberry pi. This application is required on raspberri-pi if developers use raspberri-pi in above applications. Sample project for [Pi-sensehat](Pi-sensehat)|  

## How to use the Cisco IoT SDKs for Java   
Connect your device to the gateway. The IoT device SDKs enable you to implement applications for a gateway. These gateways can be connected to any sort of device and can extract and process information received from these devices.  

The application will run on Mac, Linux, IOx. To start developing these applications, refer documentation as shown below.   

| S.No          | Application Name     | Documentation  | 
|:-------------:|:--------------------|:------------|
| 1       | iotsp-edge-rule-engine | [Getting Started](examples/iotsp-edge-rule-engine/GettingStarted.md), [Readme](examples/iotsp-edge-rule-engine/README.md)|  
| 2       | iotsp-batching |  [Getting Started](examples/iotsp-batching/GettingStarted.md), [Readme](examples/iotsp-batching/README.md)| 
| 3      | Pi-sensehat | [Readme](Pi-sensehat/README.md)|  

## Java docs for libraries    

There are a number of libraries required in order to run sample applications - iotsp-edge-rule-engine and iotsp-batching. These libraries are stored [here](libs/lib-jars). The Javadoc for these libraries can be found [here](libs/javadoc-jars). In order to access any libraries Javadoc, run the following commands from root of the folder.  
```   
cd libs/javadoc-jars
mkdir tmpFolder
cp <Jar name of library whose java doc is to be accessed> tmpFolder/
cd tmpFolder/
jar -xvf <Jar name of library whose java doc is to be accessed>
```   
Then navigate to this "tmpFolder" from your finder and open "index.html" using any web browser.  

## Sample Application    

In this repository, you can find simple sample implementations that will kickstart your development.  

| S.No          | Application Name     | Sample Application  |     
|:-------------:|:--------------------|:------------|   
| 1       | iotsp-edge-rule-engine | Sample application for [Rule Engine On Edge](examples/iotsp-edge-rule-engine/src/main/java/com/cisco/iot/swp/edge/app/RESampleApp.java)|      
| 2       | iotsp-batching | Sample application for [Batching](examples/iotsp-batching/src/main/java/com/cisco/iot/swp/batch/re/app/BatchManagerSampleApp.java)|          

## Writing Rules  
To write rules, please refer Cisco's tutorial on their [DSL](https://github.com/CiscoDevNet/iot-state-processing-dsl).  

## Directory Structure    
### Pi-Sensehat
  - The Pi-Sensehat directory contains all the materials necessary to install the sensehat application on the raspberry pi. In this directory the readme contains instructions to install the application. The sensehat-app directory is to be installed into the home/pi/projects directory on the raspberry pi. 
  
### examples
  - The examples directory contains two subdirectories - iotsp-batching and iotsp-edge-rule-engine. These directories contain information on how to install and run the sample applications. They also provide APIs and documentation on different options the sample applications provide and use.
  
### libs
This directory contains libraries and their java-docs, necessary for runnning the above sample applications. It contains 2 folders:  
* [lib-jars](libs/lib-jars) - This folder contains the library jars
* [javadoc jars](libs/javadoc-jars) - This folder contains java doc jars for all the libraries.     

## Change Log   

### Pi-Sensehat   
Aug 08, 2017		Initial Check in 

### iotsp-edge-rule-engine   
Aug 08, 2017		Initial Check in   
Nov 22, 2017		Update jars, Special Message, Document update  
Jan 31, 2018		Update jars with Javadoc  	   	   

### iotsp-batching      
Aug 08, 2017		Initial Check in   
Nov 22, 2017		Update jars, Special Message, Document update   
Jan 31, 2018		Update jars with Javadoc  	  

## Support  
Please email all questions and feedback to dataconnect-support@cisco.com