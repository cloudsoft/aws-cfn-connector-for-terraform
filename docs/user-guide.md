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

## Return Values

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

| Key | Type | Description |
|-----|------|-------------|
| `Outputs` | Object | All output coming from the Terraform configuration, as a map. |
| `OutputsStringified` | String | All output coming from the Terraform configuration, as a JSON string of the map. |

