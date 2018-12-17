#!/bin/sh

stack_name=$1

aws cloudformation delete-stack --stack-name $stack_name 



if [ $? -eq 0 ]; then
  echo "Delete in progress"
  aws cloudformation wait stack-create-complete --stack-name $stack_name
  echo "Stack deleted successfully"
  
else
  echo "Failure while deleting stack"
fi
