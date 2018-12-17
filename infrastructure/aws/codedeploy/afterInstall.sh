#!/bin/bash

sudo systemctl stop tomcat.service

echo "Updating Lambda Function"
  aws lambda update-function-code --function-name myLambda --region us-east-1 --s3-bucket lambda.$domain.csye6225.com --s3-key LambdaApp-2.2.jar
echo "Update complete"

sudo rm -rf /opt/tomcat/webapps/docs  /opt/tomcat/webapps/examples /opt/tomcat/webapps/host-manager  /opt/tomcat/webapps/manager /opt/tomcat/webapps/ROOT

sudo chown tomcat:tomcat /opt/tomcat/webapps/webapi_assignment3-0.0.1-SNAPSHOT.war

# cleanup log files
sudo rm -rf /opt/tomcat/logs/catalina*
sudo rm -rf /opt/tomcat/logs/*.log
sudo rm -rf /opt/tomcat/logs/*.txt
