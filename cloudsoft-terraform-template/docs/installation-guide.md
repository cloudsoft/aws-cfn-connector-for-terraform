# Installation guide

The Terraform resource provider for CloudFormation adds a new CloudFormation resource type, `Cloudsoft::Terraform::Infrastructure`, which allows you to deploy a Terraform infrastructure as a part of a CloudFormation stack using a Terraform configuration that is a part of a CloudFormation template.

This page will guide you on how to install the Terraform resource provider for CloudFormation.

## Prerequisites

### Terraform server

The connector requires a *running* Terraform server that:
- is publicly SSH'able
- is configured with the correct credentials for the target clouds

### AWS CLI

You will need to have the AWS CLI installed and configured on your local machine. Please [see the documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) how to achieve this.

## Installation

1. Download the 3 systemd helper and install them onto your Terraform server:
   ```sh
   mkdir -p ~/.config/systemd/user
   pushd ~/.config/systemd/user
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/server-side-systemd/terraform-apply%40.service
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/server-side-systemd/terraform-destroy%40.service
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/server-side-systemd/terraform-init%40.service
   popd
   systemctl --user daemon-reload
   ```
1. Download the [`resource-role.yml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/resource-role.yaml) template and create a stack using the command below. Note the ARN of the created role for step 4:
   ```sh
   aws cloudformation create-stack \
     --template-body "file://resource-role.yaml" \
     --stack-name CloudsoftTerraformInfrastructureExecutionRole \
     --capabilities CAPABILITY_IAM
   ```
1. Download the [`setup.yml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/setup.yaml) template and create a stack using the command below. Note the ARN of the created role for step 4:
   ```sh
   aws cloudformation create-stack \
     --template-body "file://setup.yaml" \
     --stack-name CloudsoftTerraformInfrastructureSetup \
     --capabilities CAPABILITY_IAM
   ```
1. Register the `Cloudsoft::Terraform::Infrastructure` CloudFormation type, using the command below:
   ```sh
    EXECUTION_ROLE_ARN=...
    LOGGING_ROLE_ARN=...
    LOG_GROUP_NAME=...

    aws cloudformation register-type \
      --type RESOURCE \
      --type-name Cloudsoft::Terraform::Infrastructure \
      --schema-handler-package https://github.com/cloudsoft/aws-cfn-connector-for-terraform/releases/download/latest/cloudsoft-terraform-infrastructure.zip \
      --execution-role-arn $EXECUTION_ROLE_ARN \
      --logging-config "{\"LogRoleArn\":\"$LOGGING_ROLE_ARN\",\"LogGroupName\": \"$LOG_GROUP_NAME\"}"
   ```
   
   If you are updating the connector, note the version number and use the following command to set the default version:
   ```sh
   aws cloudformation set-type-default-version --type RESOURCE --type-name Cloudsoft::Terraform::Infrastructure --version-id 0000000N
   ```
1. Update as required in parameter store the following parameters:
   - `/cfn/terraform/ssh-host`
   - `/cfn/terraform/ssh-port`
   - `/cfn/terraform/ssh-username`
   - `/cfn/terraform/ssh-key`
   - `/cfn/terraform/ssh-fingerprint`
