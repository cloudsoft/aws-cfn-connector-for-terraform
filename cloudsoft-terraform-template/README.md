# Cloudsoft::Terraform::Template

Congratulations on starting development! Next steps:

1. Write the JSON schema describing your resource, `cloudsoft-terraform-template.json`
2. The RPDK will automatically generate the correct resource model from the
   schema whenever the project is built via Maven. You can also do this manually
   with the following command: `cfn-cli generate`
3. Implement your resource handlers


Please don't modify files under `target/generated-sources/rpdk`, as they will be
automatically overwritten.

The code use [Lombok](https://projectlombok.org/), and [you may have to install
IDE integrations](https://projectlombok.org/) to enable auto-complete for
Lombok-annotated classes.

# HOW TO run synchronous SSH tests with SAM
## S3 props (public access)
* http://denis-examples.s3-website.eu-central-1.amazonaws.com/example5.tf (one VM)
* http://denis-examples.s3-website.eu-central-1.amazonaws.com/example5.2.tf (two VMs)

## Terraform server
* EC2 t2.micro
* AMI: ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server-20191002 (ami-0cc0a36f626a4fdf5)
* SSH key pair name: terraform-denis-20191104
* Terraform v0.12.13
* AWS CLI not installed
* temporary (MFA-based) AWS credentials in `~ubuntu/.aws/credentials`

## development laptop
* OS: Ubuntu Linux 18.04
* RPDK nightly SHA-1 sum: `af51034244c1a1f443ef4b634b30ac281a8c27bb` (cfn-cli 0.1)
* SAM CLI, version 0.23.0 (`apt-get install python3-pip && pip3 install aws-sam-cli`)
* Docker version 18.09.7, build 2d0083d (`apt-get install docker.io`)

```shell
git checkout 7875a65
cp ~/.ssh/terraform-denis-20191104.pem ./src/main/resources/
# If you want to use your own key pair, copy the private key to the directory
# above and declare it as a resource in pom.xml so the key gets into the
# temporary container in the resuting JAR. Also modify TerraformInterfaceSSH.java
# to use the new key and add the public key to ~ubuntu/.ssh/authorized_keys on
# the Terraform server.
cfn-cli generate
# Make sure the Terraform server is listed in TerraformInterfaceSSH.java and
# accepts 22/tcp from the current host.
mvn package
# As you run the tests below, you will see the commands sent to the server and
# their respective stdout. It helps to look into the EC2 console to see the
# progress within AWS and to run "watch -n 1 ls -lRA ~/tfdata" on the Terraform
# server to see the filesystem changes as they are happening.
sam local invoke TestEntrypoint --event sam-tests/create.json # usually 20-30 seconds
sam local invoke TestEntrypoint --event sam-tests/update.json # usually 20-30 seconds
sam local invoke TestEntrypoint --event sam-tests/delete.json # usually 40 seconds
```
