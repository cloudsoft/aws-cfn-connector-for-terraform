# Cloudsoft::Terraform::Infrastructure

## Quick start

First, you need to install the custom type into CloudFomation. See the [installation guide](./docs/installation-guide.md) documentation.

Once done, you can use Terraform in CloudFormation templates by specifying the type `Cloudsoft::Terraform::Infrastructure`.
For example:

```
AWSTemplateFormatVersion: 2010-09-09
Description: Terraform in CloudFormation example, using the Terraform Connector for CloudFormation
Resources:
  TerraformEc2Example:
    Type: Cloudsoft::Terraform::Infrastructure
    Properties:
      ConfigurationContent: |
        resource "aws_instance" "my-test-instance" {
          ami             = "XXXXXXX"
          instance_type   = "t2.micro"
        }
```

The Terraform configuration does not need to be in-lined; you can instead use 
`ConfigurationUrl` or `ConfigurationS3Path` to point at a configuration.

You can then:

* Use Terraform in AWS Service Catalog
* Mix and match Terraform with CloudFormation in IaC templatees

Features:

* View Terraform outputs as CloudFormation outputs
* Drive Terraform updates through CloudFormation
* Read and delete

For more information on how to use the custom type, see the [user guide](./docs/user-guide.md) documentation.

## Development

To setup your local environment, please see the [developer guide](./docs/developer-guide.md) documentation.
