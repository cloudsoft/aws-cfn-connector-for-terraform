# Developer guide

## Prerequisites

To build the project, you will first need few things installed on your local machine. For instance:

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- [CFN CLI](https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-setup.html)

## Build

Once this repository is cloned:

1. Build with: 
   ```sh
   mvn clean package
   ```
1. Register in CloudFormation with:
   ```sh
   cfn submit --set-default -v
   ```

The connector requires few parameters in parameter store. If you haven't installed the connector, you can use the
[`setup.yaml`](https://raw.githubusercontent.com/cloudsoft/aws-cfn-connector-for-terraform/master/cloudsoft-terraform-template/setup.yaml)
template to create a stack by downloading that file, editing it, and using the command below:

```sh
aws cloudformation create-stack \
    --template-body "file://setup.yaml" \
    --stack-name CloudsoftTerraformInfrastructureSetup \
    --capabilities CAPABILITY_IAM
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

The JSON payload must contain the `Cloudsoft::Terraform::Infrastructure` properties within `desiredResourceState`. Provided events
 (i.e. `create.json`, `update.json` and `delete.json`) all use the `ConfigurationUrl` but you can use any property defined
 in the [user guide](./user-guide.md#syntax).
 
 To run the tests:
 1. In one terminal, start SAM local lambda: `sam local start-lambda`
 2. In another terminal, run: `cfn invoke --max-reinvoke 10 {CREATE,READ,UPDATE,DELETE,LIST} path/to/event.json`
    
    For instance to do a full cycle of the tests for this project, execute:
    ```sh
    cfn invoke --max-reinvoke 10 CREATE ./sam-tests/create.json
    cfn invoke --max-reinvoke 10 READ ./sam-tests/read.json
    cfn invoke --max-reinvoke 10 UPDATE ./sam-tests/update.json
    cfn invoke --max-reinvoke 10 READ ./sam-tests/read.json
    cfn invoke --max-reinvoke 10 DELETE ./sam-tests/delete.json
    ```
    _Note that `cfn` doesn't support yet profiles so you will need to have the `default` profile setup for your `aws` CLI.
    However, you can specify `--region` to run the test in a specific region._
 
_Note these tests require the a Terraform server to be up and running, as well parameters to be set in parameter store.
See [prerequisites](./installation-guide.md#prerequisites) and [step 3 of the installation guide](./installation-guide.md#installation)._

### End-to-end tests

Once the connector is built and submitted to AWS:

1. Deploy some `Cloudsoft::Terraform::Infrastructure` resource, e.g. the file `terraform-example.cfn.yaml`:
   ```sh
   aws cloudformation create-stack --template-body file://terraform-example.cfn.yaml --stack-name terraform-example
   ```
2. Delete it when you're done:
   `aws cloudformation delete-stack --stack-name terraform-example`

_Note these tests require the a Terraform server to be up and running, as well parameters to be set in parameter store.
See [prerequisites](./installation-guide.md#prerequisites) and [step 3 of the installation guide](./installation-guide.md#installation)._

