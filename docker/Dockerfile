# Dockerfile for universalresolver/driver-did-com

FROM maven:3-jdk-11 AS build
MAINTAINER Fausto Spoto <fausto.spoto@univr.it>

# build driver-did-com

ADD . /opt/driver-did-com
RUN cd /opt/driver-did-com && mvn clean install package -P war -N -DskipTests

FROM jetty:9.4-jre11-slim
MAINTAINER Fausto Spoto <fausto.spoto@univr.it>

# install dependencies

USER root

RUN export DEBIAN_FRONTEND=noninteractive && \
    apt-get -y update && \
    apt-get install -y --no-install-recommends software-properties-common gnupg

USER jetty

# variables

ENV uniresolver_driver_did_com_network="https://lcd-devnet.commercio.network"

# copy from build stage

COPY --from=build --chown=jetty /opt/driver-did-com/target/*.war /var/lib/jetty/webapps/ROOT.war

# done

EXPOSE 8080
CMD java -Djetty.http.port=8080 -jar /usr/local/jetty/start.jar
