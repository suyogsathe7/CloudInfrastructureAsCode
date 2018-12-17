#!/bin/sh
#shell script to create AWS network infrastructures

stack_name=$1


aws cloudformation create-stack --stack-name $stack_name --template-body file://csye6225-cf-networking.json


if [ $? -eq 0 ]; then
  echo "Creating progress"
  aws cloudformation wait stack-create-complete --stack-name $stack_name
  echo "Stack created successfully"
else
  echo "Failure while creating stack"
fi
