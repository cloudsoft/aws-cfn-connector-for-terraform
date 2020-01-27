# Installation guide

The Terraform resource provider for CloudFormation adds a new CloudFormation resource type, `Cloudsoft::Terraform::Infrastructure`, which allows you to deploy a Terraform infrastructure as a part of a CloudFormation stack using a Terraform configuration that is a part of a CloudFormation template.

This page will guide you on how to install the Terraform resource provider for CloudFormation.

## Prerequisites

### Terraform server

The connector requires a running Terraform (version 0.12 or later) server that:
- runs Linux
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

1. Download the [`resource-role.yaml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/resource-role.yaml) template and create a stack using the command below. 
   Note the ARN of the created execution role for use later.
   ```sh
   aws cloudformation create-stack \
     --template-body "file://resource-role.yaml" \
     --stack-name CloudsoftTerraformInfrastructureExecutionRole \
     --capabilities CAPABILITY_IAM
   ```

1. Download the [`setup.yaml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/setup.yaml) template.
   Edit the parameters as needed. More detail on parameters is below. Note that the following ones (marked `FIXME` in the file) are required.
   
   - `/cfn/terraform/ssh-host`
   - `/cfn/terraform/ssh-username`
   - `/cfn/terraform/ssh-key`
   
1. Create the `setup` stack using the command below. Note the ARN of the created logging role and the log group for use later.
   ```sh
   aws cloudformation create-stack \
     --template-body "file://setup.yaml" \
     --stack-name CloudsoftTerraformInfrastructureSetup \
     --capabilities CAPABILITY_IAM
   ```

1. Register the `Cloudsoft::Terraform::Infrastructure` CloudFormation type, using the command below, with the values returned above.
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

## Configuration Paramters

This resource provider (RP) uses the following parameters:

   - `/cfn/terraform/ssh-host` (required): the hostname or the IP address of the Terraform server
   
   - `/cfn/terraform/ssh-username` (required): the user as which the RP should SSH
   
   - `/cfn/terraform/ssh-key` (required): the SSH key with which the RP should SSH
    
   - `/cfn/terraform/ssh-port` (defaults to 22): the port to which the RP should SSH
   
   - `/cfn/terraform/ssh-fingerprint` (optional): the fingerprint of the Terraform server, for security.
     The value must be in one of the
     [fingerprint formats supported in SSHJ](https://github.com/hierynomus/sshj/blob/master/src/main/java/net/schmizz/sshj/transport/verification/FingerprintVerifier.java#L33).
     For example, a SHA-256 fingerprint of the Ed25519 SSH host key of the current host
     can be computed with `ssh-keygen -E sha256 -lf /etc/ssh/ssh_host_ed25519_key.pub | cut -d' ' -f2`.
    
   - `/cfn/terraform/process-manager` (optional): the server-side remote persistent execution mechanism to use,
     either `nohup` (default) or `systemd`. In the latter case the server
     must run a Linux distribution that uses systemd with support for user mode and linger
     (typically CentOS 8, Fedora 28+, Ubuntu 18.04+, but not Amazon Linux 2)
        
   - `/cfn/terraform/logs-s3-bucket-prefix` (optional): if set, all Terraform logs are shipped to an S3
     bucket created as part of the stack and returned to the user as a URL on success;
     note this can be overridden by the user with a property on the resource (see the [user-guide.md])

Where a parameter is optional, it can be left unset or the special value `default` can be set to tell the RP
to use the default value.  Leaving it unset is fine, but it does cause warnings in the CloudWatch logs 
(which we cannot disable); the CloudFormation in `setup.yaml` uses the keyword `default`.
  
