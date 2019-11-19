# User guide

Once the CloudFormation connector for Terraform [is installed](installation-guide.md), you can begin to use the new custom type
`Cloudsoft::Terraform::Infrastructure` to deploy Terraform configuration. This can be added along side any other CloudFormation 
resources.

## Syntax

To declare this type in your CloudFormation template, use the following syntax

### JSON
```json
{
  "Type" : "Cloudsoft::Terraform::Infrastructure",
  "Properties" : {
      "ConfigurationContent": String,
      "ConfigurationUrl": String,
      "ConfigurationS3Path": String
    }
}
```

### YAML
```yaml
Type: Cloudsoft::Terraform::Infrastructure
Properties:
  ConfigurationContent: String,
  ConfigurationUrl: String,
  ConfigurationS3Path: String
```

## Properties

| Key | Type | Description | Required |
|-----|------|-------------|----------|
| `ConfigurationContent` | String | Inlined terraform configuration, passed to the terraform server. | Conditional.<br/><br/>Exactly one of `ConfigurationContent`, `ConfigurationUrl` or `ConfigurationS3Path` must be specified |
| `ConfigurationUrl` | String | Public HTTP URL of a terraform configuration. This will be downloaded and used by the terraform server. | Conditional.<br/><br/>Exactly one of `ConfigurationContent`, `ConfigurationUrl` or `ConfigurationS3Path` must be specified |
| `ConfigurationS3Path` | String | S3 path object representing a terraform configuration. The current account must have access to this resource. This will be downloaded and used by the terraform server. | Conditional.<br/><br/>Exactly one of `ConfigurationContent`, `ConfigurationUrl` or `ConfigurationS3Path` must be specified |

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, `Ref` returns [TODO]

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

| Key | Type | Description |
|-----|------|-------------|
| `Outputs` | Object | Any output coming from the Terraform configuration, as a map. |
| `OutputsStringified` | Object | Any output coming from the Terraform configuration, as a JSON string of the map. |
