# Installation guide

The CloudFormation connector for Terraform add a new CloudFormation type, for instance `Cloudsoft::Terraform::Configuration` which allows you to deploy Terraform configuration direction through CloudFormation.

This page will guide you on how to install the CloudFormation connector for Terraform.

## Prerequisites

### Terraform server

The connector requires a *running* Terraform server that:
- is publicly SSH'able
- is configured with the correct credentials for the target clouds

### AWS CLI

You will need to have the AWS CLI installed and configured on your local machine. Please [see the documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) how to achieve this.

## Installation

**Important: registering custom types is available only in region `eu-central-1` and `us-west-2`.**

1. [Download](https://github.com/cloudsoft/aws-cfn-connector-for-terraform/releases) the latest release of the connector
2. Download the [`resource-role.yml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/resource-role.yaml) template and create a stack using the command below. Note the ARN of the created role for step 3:
   ```sh
   aws cloudformation create-stack \
     --region us-west-2 \
     --template-body "file://resource-role.yaml" \
     --stack-name CloudsoftTerraformTemplateExecutionRole
   ```
3. Download the [`setup.yml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/setup.yaml) template and create a stack using the command below. Note the ARN of the created role for step 3:
   ```sh
   aws cloudformation create-stack \
     --region us-west-2 \
     --template-body "file://setup.yaml" \
     --stack-name CloudsoftTerraformTemplateSetup
   ```
4. Register the `Cloudsoft::Terraform::Infrastructure` CloudFormation type, using the command below:
   ```sh
    EXECUTION_ROLE_ARN=...
    LOGGING_ROLE_ARN=...
    LOG_GROUP_NAME=...

    aws cloudformation register-type \
      --type-name Cloudsoft::Terraform::Infrastructure
      --schema-handler-package s3://my-bucket/aws-logs-metricfilter.zip
      --execution-role-arn $EXECUTION_ROLE_ARN
      --logging-config "{\"LogRoleArn\":\"$LOGGING_ROLE_ARN\",\"LogGroupName\": \"$LOG_GROUP_NAME\"}"
 
   ```
5. Download the 3 systemd helper and install them onto your Terraform server:
   ```sh
   mkdir -p ~/.config/systemd/user
   pushd ~/.config/systemd/user
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/server-side-systemd/terraform-apply%40.service
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/server-side-systemd/terraform-destroy%40.service
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/server-side-systemd/terraform-init%40.service
   popd
   systemctl --user daemon-reload
   ```
6. Update as required in parameter store the following parameters:
   - `/cfn/terraform/ssh-host`
   - `/cfn/terraform/ssh-port`
   - `/cfn/terraform/ssh-username`
   - `/cfn/terraform/ssh-key`
   - `/cfn/terraform/ssh-fingerprint`
