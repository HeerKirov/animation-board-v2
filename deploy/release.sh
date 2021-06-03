#!/bin/sh

PROJ_PATH="${BASH_SOURCE%/*}/.."
WORK_DIR="$(mktemp -d)"
VERSION="${$1:-"dev"}"

cp -f docker/Dockerfile $WORK_DIR
cp -f docker/application.properties.template $WORK_DIR
cp -f docker/startup.sh $WORK_DIR
cp -f docker/sources.list $WORK_DIR
cp -f $PROJ_PATH/target/animation-board-v2-*.jar $WORK_DIR

sudo docker build $WORK_DIR -t animation-board-v2:$VERSION

rm -rf $WORK_DIR