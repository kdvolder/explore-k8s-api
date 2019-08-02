#!/bin/bash
source secrets.sh
docker run -it \
    -p 8888:8888 \
    -e CF_API="$CF_API" \
    -e CF_USER="$CF_USER" \
    -e CF_PASSWORD="$CF_PASSWORD" \
    -e CF_ORG="$CF_ORG" \
    -e CF_SPACE="$CF_SPACE" \
    -e CF_APP="$CF_APP" \
    -e CF_JMX_PORT="$CF_JMX_PORT" \
    kdvolder/cf-ssh-tunnel
