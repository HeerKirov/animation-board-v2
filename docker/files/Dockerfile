FROM openjdk:11-jre-slim

ADD animation-board-v2-*.jar /
ADD application.properties.template /
ADD startup.sh /
ADD sources.list /etc/apt/

RUN apt-get update && \
    apt-get install -y gettext imagemagick && \
    chmod +x /startup.sh

EXPOSE 8000 8000

entrypoint ["/startup.sh"]