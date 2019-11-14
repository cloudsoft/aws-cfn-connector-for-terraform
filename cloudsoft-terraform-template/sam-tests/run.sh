#!/bin/bash

# Variables
event=
profile=default
region=eu-central-1

# Functions
usage() {
  echo "Usage: $0 --event event.json [--profile AWS-PROFILE-NAME] [--region AWS-REGION]"
}

while [ "$1" != "" ]; do
  case $1 in
  --event)
    shift
    event=$1
    ;;
  --profile)
    shift
    profile=$1
    ;;
  --region)
    shift
    region=$1
    ;;
  -h | --help)
    usage
    exit
    ;;
  *)
    usage
    exit 1
    ;;
  esac
  shift
done

if [ -z "$event" ]
then
  echo "Event JSON file is required"
  usage
  exit 1
fi

echo "=> Getting temporary credentials from profile \"$profile\""
tempCreds=$(aws sts get-session-token --profile "$profile" --region "$region")

export AWS_DEFAULT_REGION=$region

echo "=> Invoking test with JSON event \"$event\" in region \"$region\""
cat $event | jq ". + $tempCreds" | sam local invoke TestEntrypoint
