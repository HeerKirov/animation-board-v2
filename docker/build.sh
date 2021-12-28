#!/bin/bash

MAVEN_IMAGE=maven:3.8.3-openjdk-17
MAVEN_REPOSITORY=$HOME/.m2
PROJ_PATH="$(dirname "$(realpath "${BASH_SOURCE[0]}")")/.."

sudo docker run -it --rm \
    -v $MAVEN_REPOSITORY:/root/.m2 \
    -v "$PROJ_PATH":/usr/src/mymaven \
    -w /usr/src/mymaven \
    $MAVEN_IMAGE \
    mvn clean package