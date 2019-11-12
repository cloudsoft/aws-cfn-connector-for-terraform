# Cloudsoft::Terraform::Template

(TODO: rename Terraform::Configuration)

# Usage

Once built and installed to CloudFormation, you can deploy CFN including Terraform as follows:

```
AWSTemplateFormatVersion: 2010-09-09
Description: Terraform in CloudFormation example, using the Terraform Connector for CloudFormation
Resources:
  TerraformEc2Example:
    Type: Cloudsoft::Terraform::Template
    Properties:
      ConfigurationContent: |
      
        resource "aws_instance" "my-test-instance" {
          ami             = "XXXXXXX"
          instance_type   = "t2.micro"
        }
```

The TFM does not need to be in-lined; you can instead use `ConfigurationUrl` or `ConfigurationS3Path` to point at a TFM configuration or a ZIP.

You can then:

* [TODO] View outputs
* [TODO] Update in the usual CFN way
* Delete when done

In short, this lets you re-use your Terraform with CloudFormation!


# Build and Install

### Quick Start

1. Build with `mvn clean package`

2. Register with CFN: `cfn-cli submit -v`

3. [only if updating] Set the version to use:
   `aws cloudformation set-type-default-version --type RESOURCE --type-name Cloudsoft::Terraform::Template --version-id 0000000N`
   
   If you update the codebase you must choose an `N` greater than those already installed. 
    
   To retrieve information about the versions of a resource provider:
   `aws cloudformation list-type-versions --type RESOURCE --type-name Cloudsoft::Terraform::Template`
   
   If you have so many it gets irritating or you hit the AWS limit (15):
   `aws cloudformation deregister-type --type RESOURCE --type-name Cloudsoft::Terraform::Template --version-id 00000004` 

4. Set parameters in AWS Parameter Store letting the resource provider know how to connect to Terraform.

5. Deploy some Terraform, e.g. the file `terraform-example.cfn.yaml`:
   `aws cloudformation create-stack --template-body file://terraform-example.cfn.yaml --stack-name terraform-example`
   
6. Delete it when you're done:
   `aws cloudformation delete-stack --stack-name terraform-example`


### Script

```shell
export TYPE_NAME=Cloudsoft::Terraform::Template
export TYPE_NAME=Cloudsoft::Terraform::Template2
export TYPE_NAME=AWS2::Logs::MetricFilter

cfn-cli generate && \
mvn clean package && cfn-cli submit -v | tee submit.log && \
REG_TOKEN=$(grep token: submit.log | awk '{print $NF}')

while ( aws cloudformation describe-type-registration --registration-token ${REG_TOKEN} | grep Description | grep IN_PROGRESS ) ; do sleep 2 ; done

aws cloudformation describe-type-registration --registration-token ${REG_TOKEN}

export V=$(aws cloudformation list-type-versions --type RESOURCE --type-name $TYPE_NAME | jq -r .TypeVersionSummaries[].VersionId |  sort | tail -1)
aws cloudformation set-type-default-version --type RESOURCE --type-name $TYPE_NAME --version-id $V && \
  echo Set $TYPE_NAME version $V

aws cloudformation list-types

aws cloudformation create-stack --template-body file://terraform-example.cfn.yaml --stack-name terraform-example
aws cloudformation create-stack --template-body file://stack.json --stack-name metrics-example

aws cloudformation delete-stack --stack-name terraform-example
```

## Detail

### Logging

```shell
aws cloudformation register-type \
  --type-name Cloudsoft::Terraform::Template \
  --schema-handler-package s3://denis-examples/cloudsoft-terraform-template.zip \
  --logging-config "{\"LogRoleArn\": \"arn:aws:iam::304295633295:role/CloudFormationManagedUplo-LogAndMetricsDeliveryRol-DQU4AQ5IPTFJ\",\"LogGroupName\": \"uluru_example_test\"}" \
  --type RESOURCE

https://denis-examples.s3.eu-central-1.amazonaws.com/cloudsoft-terraform-template.zip \
  s3://denis-examples/cloudsoft-terraform-template.zip
```
  
### Debugging

* force-synchronous


### IDE

The code use [Lombok](https://projectlombok.org/), and [you may have to install
IDE integrations](https://projectlombok.org/) to enable auto-complete for
Lombok-annotated classes.

## HOW TO run tests
### Create/update the parameters in the region if necessary (once per region)

```shell
aws cloudformation create-stack \
--stack-name cloudsoft-terraform-template-parameters \
--template-body file://parameters.yml
```

### synchronous tests with SAM

#### S3 props (public access)
* http://denis-examples.s3-website.eu-central-1.amazonaws.com/example7-step1.tf
  (essential: create two S3 buckets)
* http://denis-examples.s3-website.eu-central-1.amazonaws.com/example7-step2.tf
  (optional: replace one of the buckets with a new one)

#### Terraform server
* EC2 t2.micro
* AMI: ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server-20191002 (ami-0cc0a36f626a4fdf5)
* SSH key pair name: terraform-denis-20191104
* Terraform v0.12.13
* AWS CLI not installed
* temporary (MFA-based) AWS credentials in `~ubuntu/.aws/credentials`
* Install systemd helpers:
  ```shell
  mkdir -p ~/.config/systemd/user
  # Copy terraform*.service files from this repository into ~/.config/systemd/user
  systemctl --user daemon-reload
  ```

#### development laptop
* OS: Ubuntu Linux 18.04
* RPDK nightly SHA-1 sum: `9c01f82abfc105036d174416c138414975150303` (cfn-cli 0.1)
* SAM CLI, version 0.23.0 (`apt-get install python3-pip && pip3 install aws-sam-cli`)
* Docker version 18.09.7, build 2d0083d (`apt-get install docker.io`)

```shell
# If you want to use your own key pair, hard-code the private key into
# TerraformInterfaceSSH.java and add the public key to
# ~ubuntu/.ssh/authorized_keys on the Terraform server. Please use a key that
# you can afford to lose, i.e. a dedicated key for this project development
# server, NOT a key that allows access to something else, such as your laptop!
cfn-cli generate
# Make sure the Terraform server is listed in TerraformInterfaceSSH.java and
# accepts 22/tcp from the current host.
mvn package
# As you run the tests below, you will see the commands sent to the server and
# their respective stdout. It helps to look into the S3 console to see the
# progress within AWS and to run "watch -n 1 ls -lRA ~/tfdata" on the Terraform
# server to see the filesystem changes as they are happening.
sam local invoke TestEntrypoint --event sam-tests/create.json # about 20 seconds
sam local invoke TestEntrypoint --event sam-tests/update.json # about 20 seconds
sam local invoke TestEntrypoint --event sam-tests/delete.json # about 15 seconds
```
