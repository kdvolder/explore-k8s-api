#!/bin/bash
set -ev
cf login -a "$CF_API" -u "$CF_USER" -p "$CF_PASSWORD" -o "$CF_ORG" -s "$CF_SPACE"
#cf ssh "$CF_APP" -L "8888:localhost:41609" -N
#cf apps
cf ssh pets -L 0.0.0.0:$CF_JMX_PORT:localhost:$CF_JMX_PORT -N

