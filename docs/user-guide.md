# User guide

Once the CloudFormation connector for Terraform [is installed](installation-guide.md), you can begin to use the new custom type
`Cloudsoft::Terraform::Infrastructure` to deploy Terraform configuration. This can be added along side any other CloudFormation 
resources.

## Syntax

To declare this type in your CloudFormation template, use the following syntax,
with exactly _one_ `Configuration` property specified.
The URL and S3 variants can point at a `TF` text file or a `ZIP` archive.

### JSON
```json
{
  "Type" : "Cloudsoft::Terraform::Infrastructure",
  "Properties" : {
      "Variables": {
          "variable1": "string1",
          "variable2": "string2",
          "variable3": "string3"
      },
      "ConfigurationContent": "String",
      "ConfigurationUrl": "String",
      "ConfigurationS3Path": "String"
    }
}
```

### YAML
```yaml
Type: Cloudsoft::Terraform::Infrastructure
Properties:
  Variables:
    variable1: string1
    variable2: string2
    variable3: string3
  ConfigurationContent: String
  ConfigurationUrl: String
  ConfigurationS3Path: String
```

## Properties

| Key | Description | Required |
|-----|-------------|----------|
| `ConfigurationContent` | Inlined Terraform configuration text to be uploaded to the Terraform server. | Conditional.<br/><br/>Exactly one of `ConfigurationContent`, `ConfigurationUrl` or `ConfigurationS3Path` must be specified. |
| `ConfigurationUrl` | Public HTTP URL of a Terraform configuration. This will be downloaded from within CloudFormation and uploaded to the Terraform server. | (as above) |
| `ConfigurationS3Path` | S3 path object representing a Terraform configuration. The current account must have access to this resource. This will be downloaded from within CloudFormation and uploaded to the Terraform server. | (as above) |
| `Variables` | Variables to make available to the Terraform configuration by means of an `.auto.tfvars.json` file. | Optional in the CloudFormation template, although may be required by the Terraform configuration. |
| `LogBucketName` | The name of an S3 bucket to create (if not present) and write log files | Optional; useful if the Terraform is not behaving as expected |

## Return Values

The resource provider will set the following outputs on the resource.

| Key | Type | Description |
|-----|------|-------------|
| `Outputs` | Object | All output coming from the Terraform configuration, as a map. |
| `OutputsStringified` | String | All output coming from the Terraform configuration, as a JSON string of the map. |
| `LogBucketUrl` | String | A URL where logs can be found (if the property or RP configuration is set). Note that this is only set if a log bucket is explicitly requested either with the `LogBucketName` property in CFN or a `/cfn/terraform/logs-s3-bucket-prefix` parameter in SSM. |

You can use the `Fn::GetAtt` intrinsic function to access these values,
e.g. in the `Outputs` section of your CloudFormation to set an output on the stack and see it. 
(At present there is no other way to see these values without inspecting the logs.)

It can also sometimes be useful to send the `OutputsStringified` to another Lambda to parse the JSON
and retrieve selected fields for use elsewhere in your stack.
(At present it is not possible using `Fn::GetAtt` to access a specific field within the `Outputs`.)

