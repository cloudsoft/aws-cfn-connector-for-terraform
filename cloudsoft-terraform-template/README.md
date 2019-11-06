# Cloudsoft::Terraform::Template

(TODO: rename Terraform::Configuration)

### Quick Start

1. Build with `mvn clean package`

2. Register with CFN: `cfn-cli submit -v`

3. Set the version to use:
   `aws cloudformation set-type-default-version --type RESOURCE --type-name Cloudsoft::Terraform::Template --version-id 0000000N`
   
   If you update the codebase you must choose an `N` greater than those already installed. 
    
   To retrieve information about the versions of a resource provider:
   `aws cloudformation list-type-versions --type RESOURCE --type-name Cloudsoft::Terraform::Template`

4. Deploy some Terraform, e.g. the file `terraform-example.cfn.yaml`:
   `aws cloudformation create-stack --template-body file://terraform-example.cfn.yaml --stack-name terraform-example`
   
5. Delete it when you're done:
   `aws cloudformation delete-stack --stack-name terraform-example`


### Logging

aws cloudformation register-type \
  --type-name Cloudsoft::Terraform::Template \
  --schema-handler-package s3://denis-examples/cloudsoft-terraform-template.zip \
  --logging-config "{\"LogRoleArn\": \"arn:aws:iam::304295633295:role/CloudFormationManagedUplo-LogAndMetricsDeliveryRol-DQU4AQ5IPTFJ\",\"LogGroupName\": \"uluru_example_test\"}" \
  --type RESOURCE

https://denis-examples.s3.eu-central-1.amazonaws.com/cloudsoft-terraform-template.zip \
  s3://denis-examples/cloudsoft-terraform-template.zip
  
  
### Debugging

force-synchronous