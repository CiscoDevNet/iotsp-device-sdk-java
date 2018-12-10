
#!/bin/sh
cat /etc/hosts
trap process_stop SIGTERM 

process_stop() {
    date >> $CAF_APP_LOG_DIR/stdout.log 2>&1 
    echo 'System exit!' >> $CAF_APP_LOG_DIR/stdout.log 2>&1 
    exit 0
}

echo 'Launch application..... ' >> $CAF_APP_LOG_DIR/stdout.log
echo $HOST_DEV1 >> $CAF_APP_LOG_DIR/stdout.log

echo 'Environment variables:' >> $CAF_APP_LOG_DIR/stdout.log
/usr/bin/env >> $CAF_APP_LOG_DIR/stdout.log

echo 'Application log directory: ' $CAF_APP_LOG_DIR >> $CAF_APP_LOG_DIR/stdout.log
ls -ltr >> $CAF_APP_LOG_DIR/stdout.log
date >> $CAF_APP_LOG_DIR/stdout.log 2>&1

echo 'Application config file location:' $CAF_APP_CONFIG_FILE >>  $CAF_APP_LOG_DIR/stdout.log

echo 'Application location: ' $CAF_APP_PATH >> $CAF_APP_LOG_DIR/stdout.log
ls -ltr $CAF_APP_PATH >> $CAF_APP_LOG_DIR/stdout.log

echo 'Changing location to Application log directory : ' $CAF_APP_LOG_DIR >> $CAF_APP_LOG_DIR/stdout.log
cd $CAF_APP_LOG_DIR
ls -ltr >> $CAF_APP_LOG_DIR/stdout.log

sleep 5

while true; do
echo 'Starting the application' >> $CAF_APP_LOG_DIR/stdout.log
        set -o xtrace
java -Xmx100m -Djava.net.preferIPv4Stack=true -XX:GCTimeRatio=4 -Djava.util.logging.config.file=$CAF_APP_PATH/logging.properties -jar $CAF_APP_PATH/iotsp-batch-store-manager-mdc-1.0.0.1-SNAPSHOT-all.jar -s -d $CAF_APP_PATH/genericData.data -c $CAF_APP_CONFIG_FILE >> $CAF_APP_LOG_DIR/stdout.log 2>&1;
        set +o xtrace
echo 'Finished running the application' >> $CAF_APP_LOG_DIR/stdout.log
    sleep 100
done