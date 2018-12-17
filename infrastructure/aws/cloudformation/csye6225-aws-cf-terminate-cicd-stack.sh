#!/bin/sh
#shell script to delete AWS stack
stack_name=$1

bucket_name=$2
accid=$3

bucketName="code-deploy.$bucket_name.csye6225.com"
bucketName2="lambda.$bucket_name.csye6225.com"



echo deleting the stack

aws s3 rm s3://$bucketName --recursive
aws s3 rb s3://$bucketName --force

aws s3 rm s3://$bucketName2 --recursive
aws s3 rb s3://$bucketName2 --force

# fetching the acc id
accid=$(aws sts get-caller-identity --output text --query 'Account')
echo "AccountId: $accid"


aws iam remove-role-from-instance-profile --instance-profile-name ayyodevre --role-name CodeDeployEC2ServiceRole

aws iam delete-role-policy --role-name CodeDeployEC2ServiceRole --policy-name CodeDeploy-EC2-S3


aws iam delete-role --role-name CodeDeployEC2ServiceRole

aws iam detach-role-policy --role-name CodeDeployServiceRole --policy-arn arn:aws:iam::aws:policy/service-role/AWSCodeDeployRole

aws iam delete-role --role-name CodeDeployServiceRole


# Detaching user policies
aws iam detach-user-policy --user-name travis --policy-arn arn:aws:iam::$accid:policy/Travis-Code-Deploy
aws iam detach-user-policy --user-name travis --policy-arn arn:aws:iam::$accid:policy/Travis-Lambda-Upload-To-S3
aws iam detach-user-policy --user-name travis --policy-arn arn:aws:iam::$accid:policy/Travis-Upload-To-S3

# Deleting user policies
aws iam delete-user-policy --user-name travis --policy-name Travis-Code-Deploy
aws iam delete-user-policy --user-name travis --policy-name Travis-Lambda-Upload-To-S3
aws iam delete-user-policy --user-name travis --policy-name Travis-Upload-To-S3


aws cloudformation delete-stack --stack-name $stack_name


if [ $? -eq 0 ]; then
  echo "Delete in progress"
  aws cloudformation wait stack-delete-complete --stack-name $stack_name
  echo "Stack deleted successfully"
else
  echo "Failure while deleting stack"
fi
