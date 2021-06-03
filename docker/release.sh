#!/bin/bash

PROJ_PATH="$(dirname "$(realpath "${BASH_SOURCE[0]}")")/.."
WORK_DIR="$(mktemp -d)"
VERSION="${$1:-"dev"}"

cp -f $PROJ_PATH/docker/files/Dockerfile $WORK_DIR
cp -f $PROJ_PATH/docker/files/application.properties.template $WORK_DIR
cp -f $PROJ_PATH/docker/files/startup.sh $WORK_DIR
cp -f $PROJ_PATH/docker/files/sources.list $WORK_DIR
cp -f $PROJ_PATH/target/animation-board-v2-*.jar $WORK_DIR

sudo docker build $WORK_DIR -t animation-board-v2:$VERSION

rm -rf $WORK_DIR