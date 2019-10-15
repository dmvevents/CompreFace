#!/bin/bash -xe

# File pre-processing (CRLF endings in certain files cause `docker-compose up` to crash)
dos2unix ./*

# Build and launch containers
docker-compose build
docker-compose up -d

# Run test
curl --silent --fail http://localhost:5001/status

# Teardown
docker-compose down

# Confirm test result
echo "TEST: SUCCESS"
