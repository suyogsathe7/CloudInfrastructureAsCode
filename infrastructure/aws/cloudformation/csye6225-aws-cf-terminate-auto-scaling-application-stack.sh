#!/bin/sh

stack_name=$1

bucket_name=$2

bucketName="csye6225-fall2018-$bucket_name.csye6225.com" 

echo deleting stack

aws s3 rb s3://$bucketName --force

aws cloudformation delete-stack --stack-name $stack_name




if [ $? -eq 0 ]; then
  echo "Delete in progress"
  aws cloudformation wait stack-delete-complete --stack-name $stack_name
  echo "Stack deleted successfully"
else
  echo "Failure while deleting stack"
fi
