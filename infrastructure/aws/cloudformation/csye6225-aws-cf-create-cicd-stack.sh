#!/bin/bash
stack_name=$1
bucket_name=$2

aws cloudformation create-stack --stack-name $stack_name --template-body file://csye6225-cf-cicd.json --capabilities CAPABILITY_NAMED_IAM --parameters ParameterKey=S3Bucket,ParameterValue=$bucket_name



if [ $? -eq 0 ]; then
  echo "Creating progress"
  aws cloudformation wait stack-create-complete --stack-name $stack_name
  echo "Stack created successfully"
else
  echo "Failure while creating stack"
fi
