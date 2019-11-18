# Developer guide

## Prerequisites

To build the project, you will first need few things installed on your local machine. For instance:

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- CFN CLI [TODO: Not public yet]

## Build

Once this repository is clone:

1. Activate RPDK with: `source path/to/rpdk/bin/env/activate`
2. Build with: `mvn clean package`
3. Register with CFN: `cfn-cli submit -v`
4. [only if updating] Set the version to use:
   `aws cloudformation set-type-default-version --type RESOURCE --type-name Cloudsoft::Terraform::Template --version-id 0000000N`
   
   If you update the codebase you must choose an `N` greater than those already installed. 
    
   To retrieve information about the versions of a resource provider:
   `aws cloudformation list-type-versions --type RESOURCE --type-name Cloudsoft::Terraform::Template`
   
   If you have so many it gets irritating or you hit the AWS limit (10):
   `aws cloudformation deregister-type --type RESOURCE --type-name Cloudsoft::Terraform::Template --version-id 00000004` 

Alternatively, you can use the script below to perform the tasks described above

```shell
#!/bin/bash

export TYPE_NAME=Cloudsoft::Terraform::Template

mvn clean package && cfn-cli submit -v | tee submit.log && \
REG_TOKEN=$(grep token: submit.log | awk '{print $NF}')

while ( aws cloudformation describe-type-registration --registration-token ${REG_TOKEN} | grep Description | grep IN_PROGRESS ) ; do sleep 2 ; done

aws cloudformation describe-type-registration --registration-token ${REG_TOKEN}

export V=$(aws cloudformation list-type-versions --type RESOURCE --type-name $TYPE_NAME | jq -r .TypeVersionSummaries[].VersionId | sort | tail -1)
aws cloudformation set-type-default-version --type RESOURCE --type-name $TYPE_NAME --version-id $V && \
  echo Set $TYPE_NAME version $V
```

## IDE

The code use [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/)
to enable auto-complete for Lombok-annotated classes.

## Testing

### Unit tests

Unit tests are ran as part of the Maven build. To run only the test, execute:
```sh
mvn clean test
```

### Integration tests

Integration test uses SAM local to simulate handlers execution of the connector by CloudFormation. These are triggered by
passing an event in a form of JSON payload to the `TestEntrypoint`, which will tell the connector which handler to use.

The JSON payload must contain the `Cloudsoft::Terraform::Template` properties within `desiredResourceState`. Provided events
 (i.e. `create.json`, `update.json` and `delete.json`) all use the `ConfigurationUrl` but you can use any property defined
 in the [user guide](./user-guide.md#syntax).
 
 To run the tests:
 1. In one terminal, start SAM local lambda: `sam local start-lambda`
 2. In another terminal, run: `./sam-tests/run.sh --event sam-tests/<event.json>`
    
    To do a full cycle, you can execute in sequence:
    ```sh
    ./sam-tests/run.sh --event ./sam-tests/create.json # about 20 seconds
    ./sam-tests/run.sh --event ./sam-tests/update.json # about 20 seconds
    ./sam-tests/run.sh --event ./sam-tests/delete.json # about 15 seconds
    ```
    _Note that you can specify `--profile` to get credentials from a specific profile (defaults to `default`) and
     `--region` to run the test in a specific region (defaults to `eu-central-1`)._
 
_Note these tests require the a Terraform server to be up and running, as well parameters to be set in parameter store.
See [prerequisites](./installation-guide.md#prerequisites) and [step 3 of the installation guide](./installation-guide.md#installation)._

### End-to-end tests

Once the connector is built and submitted to AWS:

1. Deploy some `Cloudsoft::Terraform::Template` resource, e.g. the file `terraform-example.cfn.yaml`:
   `aws cloudformation create-stack --template-body file://terraform-example.cfn.yaml --stack-name terraform-example`
2. Delete it when you're done:
   `aws cloudformation delete-stack --stack-name terraform-example`

_Note these tests require the a Terraform server to be up and running, as well parameters to be set in parameter store.
See [prerequisites](./installation-guide.md#prerequisites) and [step 3 of the installation guide](./installation-guide.md#installation)._

