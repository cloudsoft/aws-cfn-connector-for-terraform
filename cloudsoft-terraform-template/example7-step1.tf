provider "aws" {
  region = "eu-central-1"
}

resource "aws_s3_bucket" "bucket1" {
  bucket = "denis-example7-bucket1"
  acl    = "private"
}

resource "aws_s3_bucket" "bucket2" {
  bucket = "denis-example7-bucket2"
  acl    = "private"
}
