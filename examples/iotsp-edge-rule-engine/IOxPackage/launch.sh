#!/bin/sh
cat /etc/hosts
DEST=/tmp/cores
trap process_stop SIGTERM 

process_stop() {
    date >> $CAF_APP_LOG_DIR/stdout.log 2>&1 
    echo 'System exit!' >> $CAF_APP_LOG_DIR/stdout.log 2>&1 
    exit 0
}

echo $CAF_APP_LOG_DIR >> $CAF_APP_LOG_DIR/stdout.log
cp $CAF_APP_PATH $DEST -r
cp $CAF_APP_CONFIG_FILE $DEST/app
date >> $CAF_APP_LOG_DIR/stdout.log 2>&1 
sleep 20  
echo 'Starting the application' >> $CAF_APP_LOG_DIR/stdout.log
cd $DEST/app; java -Xmx100m -Djava.net.preferIPv4Stack=true -XX:GCTimeRatio=4 -Djava.util.logging.config.file=logging.properties -jar iot-edge-rule-engine-0.3.1.0-SNAPSHOT-all.jar -s -p -d genericData.data >> $CAF_APP_LOG_DIR/stdout.log 2>&1;
echo 'Finished running the application' >> $CAF_APP_LOG_DIR/stdout.log
while true; do
    sleep 100
done
