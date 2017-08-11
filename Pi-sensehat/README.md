# Sense HAT Data Web App

This app allows for access to the Sense HAT sensor data. It allows the data to be displayed in a flask web app and also allows for access to the Sense hat LED lights. Code from this repository should be installed on the Raspberry Pi itself. 

## Requirements

### Hardware

- Raspberry Pi 
- Sense HAT

### Software

- Flask
- Sense HAT Python library


## Installation
 - Git clone this repository
```
    git clone <this repo>
```
 - To install all of the requirements, go into the directory Pi-sensehat run the following command to install all the requirements
```
    sudo pip install -r requirements.txt 
```
 - Copy the sensehat-app directory and all of its contents(app.py and sense.py) into the raspberry pi's home folder
``` 
    /home/pi/projects
```

Make sure the Raspberry Pi has the correct system time before any pip installation is performed.
Having the wrong system time can cause remote repo certificate authentication problems and connection errors.

## Running the Application


Make sure the Raspberry pi is connected to the internet.

1. Find the Pi's IP address (this is necessary to use the sensehat APIs):
    - `ifconfig eth0`
    - Look for the inet (ipv4) address. This is your IP.
1. Run the web app:
    -  cd `sensehat-app`
    - `python app.py` or `python app.py -p <port>`
   The port number is by default 5000 if not specified.
1. Navigate to the IP address in a web browser on any device on your network (e.g. `http://192.168.1.3:5000`)
    - You should be able to see the freeboard UI in the browser
1. Available URLs are as following
    -  `http://<IP_Address>:<port>/status - This shows the device id`
    -  `http://<IP_Address>:<port>/sensehat/<type>`
      - The different types that are available are:  
        - temperature               
        - humidity                  
        - barometricpressure      
        - magnetometer         
        - acceleromete       
        - gyroscope 
        - ping (flashes the senseHat LEDs briefly)
    -  `http://<IP_Address>:<port>/message/<message> - This allows a custom message to be displayed on the SensehatLED Display (8 chars max)
       NOTE - Since this operation is not asynchronous, there can be a delay of up to 10 seconds before seeing a response.`

## Additional Notes

1. To have the Raspberry Pi automatically run the sensehat app on startup of the raspberry pi, place the following lines in your raspberry pi's rc.local file

   -   ` /usr/bin/python /home/pi/projects/sensehat-app/app.py > /home/pi/Pi_senseHat.log 2>&1 & exit 0`

## Questions and Feedback

- Please email all questions and feedback to dataconnect-support@cisco.com
