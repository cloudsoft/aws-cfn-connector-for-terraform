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
