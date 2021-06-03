#!/bin/sh

envsubst < application.properties.template > application.properties

java -jar animation-board-v2-*.jar