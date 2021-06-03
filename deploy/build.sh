#!/bin/sh

MAVEN_IMAGE=maven:3.8.1-openjdk-11
MAVEN_REPOSITORY=$HOME/.m2
PROJ_PATH="${BASH_SOURCE%/*}/.."

sudo docker run -it --rm \
    -v $MAVEN_REPOSITORY:/root/.m2 \
    -v "$PROJ_PATH":/usr/src/mymaven \
    -w /usr/src/mymaven \
    $MAVEN_IMAGE \
    mvn clean package