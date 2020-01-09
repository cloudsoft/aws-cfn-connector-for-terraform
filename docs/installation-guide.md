# Installation guide

The Terraform resource provider for CloudFormation adds a new CloudFormation resource type, `Cloudsoft::Terraform::Infrastructure`, which allows you to deploy a Terraform infrastructure as a part of a CloudFormation stack using a Terraform configuration that is a part of a CloudFormation template.

This page will guide you on how to install the Terraform resource provider for CloudFormation.

## Prerequisites

### Terraform server

The connector requires a *running* Terraform (version 0.12 or later) server that:
- runs a Linux distribution that uses systemd with support for user mode and linger, for example:
  - CentOS 8
  - Fedora 28+
  - Ubuntu 18.04+
- can accept SSH connections from AWS Lambda
- is configured with the correct credentials for the target clouds
  (for example, if the Terraform server needs to manage resources through its AWS provider,
  the configured Linux user needs to have a valid `~/.aws/credentials` file, even though
  Terraform does not use AWS CLI)
- has the command-line tools to extract archived Terraform configurations (right now this
  is ZIP, which requires `unzip`, which, for example, can be installed on Ubuntu Linux
  with `apt-get install unzip`)

### AWS CLI

You will need to have the AWS CLI installed and configured on your local machine. Please [see the documentation](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) how to achieve this.

## Installation

1. Download the 3 systemd helpers and install them onto your Terraform server:
   ```sh
   mkdir -p ~/.config/systemd/user
   pushd ~/.config/systemd/user
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/server-side-systemd/terraform-apply%40.service
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/server-side-systemd/terraform-destroy%40.service
   wget https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/server-side-systemd/terraform-init%40.service
   popd
   systemctl --user daemon-reload
   ```
1. Download the [`resource-role.yaml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/resource-role.yaml) template and create a stack using the command below. Note the ARN of the created role for step 4:
   ```sh
   aws cloudformation create-stack \
     --template-body "file://resource-role.yaml" \
     --stack-name CloudsoftTerraformInfrastructureExecutionRole \
     --capabilities CAPABILITY_IAM
   ```
1. Download the [`setup.yaml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/setup.yaml) template and create a stack using the command below. Note the ARN of the created role for step 4:
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
   
   The value of `ssh-fingerprint` must be in one of the
   [fingerprint formats supported in SSHJ](https://github.com/hierynomus/sshj/blob/master/src/main/java/net/schmizz/sshj/transport/verification/FingerprintVerifier.java#L33).
   For example, a SHA-256 fingerprint of the Ed25519 SSH host key of the current host
   can be computed as follows:
   ```shell
   ssh-keygen -E sha256 -lf /etc/ssh/ssh_host_ed25519_key.pub | cut -d' ' -f2
   ```
