#!/bin/bash

# This initialization script is run as the default command of the Docker image

NETWORK=${NETWORK:-https://lcd-devnet.commercio.network}
echo "Connecting to $NETWORK"
echo "If this is not what you wanted, run docker again and specify the network:"
echo "  docker run -p 8080:8080 -e NETWORK=XXX universalresolver/driver-did-com"
export uniresolver_driver_did_com_network=$NETWORK
java -Djetty.http.port=8080 -jar /usr/local/jetty/start.jar
