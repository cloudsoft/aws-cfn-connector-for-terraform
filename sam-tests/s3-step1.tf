# this file is read from GitHub to perform the SAM tests.
# if you wish to use a different example for SAM tests,
# create your own TF files e.g. in a branch or gist or S3,
# and change the JSON files in the folder to point to them.
# local changes will have no effect on tests!

provider "aws" {
  region = "eu-central-1"
}

resource "aws_s3_bucket" "bucket1" {
  bucket = "cfn-terraform-connector-example-bucket1"
  acl    = "private"
}

resource "aws_s3_bucket" "bucket2" {
  bucket = "cfn-terraform-connector-example-bucket2"
  acl    = "private"
}

output "bucket1-id" {
  value = aws_s3_bucket.bucket1.id
}

output "bucket1-arn" {
  value = aws_s3_bucket.bucket1.arn
}

output "bucket1-region" {
  value = aws_s3_bucket.bucket1.region
}

output "bucket2-id" {
  value = aws_s3_bucket.bucket2.id
}

output "bucket2-arn" {
  value = aws_s3_bucket.bucket2.arn
}

output "bucket2-region" {
  value = aws_s3_bucket.bucket2.region
}
