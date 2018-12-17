#!/bin/sh

stack_name=$1

netStack_name=$2

myS3Bucket=$3

#CICDstack_name=$4

bucketname="${myS3Bucket}"


stack=$(aws cloudformation describe-stacks --stack-name $netStack_name --query Stacks[0].StackId --output text)
vpc_name="${netStack_name}-csye6225-vpc"


vpcId=$(aws ec2 describe-vpcs --filters "Name=tag:aws:cloudformation:stack-id,Values=$stack" --query 'Vpcs[0].VpcId' --output text)


subnet1_name="Public-Subnet-1"
subnet2_name="Public-Subnet-2"
subnet3_name="Public-Subnet-3"


Subnet1=$(aws ec2 describe-subnets --filters "Name=tag:Name,Values=$subnet1_name" --query 'Subnets[0].SubnetId' --output text)
Subnet2=$(aws ec2 describe-subnets --filters "Name=tag:Name,Values=$subnet2_name" --query 'Subnets[0].SubnetId' --output text)
Subnet3=$(aws ec2 describe-subnets --filters "Name=tag:Name,Values=$subnet3_name" --query 'Subnets[0].SubnetId' --output text)

#EC2_role=$(aws iam get-instance-profile --instance-profile-name ayyodevre --query Roles.RoleId)

#echo "$EC2_role"

aws cloudformation create-stack --stack-name $stack_name --template-body file://csye6225-cf-application.json --parameters ParameterKey=myBucketName,ParameterValue=$bucketname ParameterKey=VPC,ParameterValue=$vpcId ParameterKey=Subnet1,ParameterValue=$Subnet1 ParameterKey=Subnet2,ParameterValue=$Subnet2 --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM
#ParameterKey=ListS3BucketsRole,ParameterValue=$EC2_role

if [ $? -eq 0 ]; then
  echo "Creating progress"
  aws cloudformation wait stack-create-complete --stack-name $stack_name
  echo "Stack created successfully"
else
  echo "Failure while creating stack"
fi
