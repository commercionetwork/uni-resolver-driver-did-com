![DIF Logo](https://raw.githubusercontent.com/decentralized-identity/universal-resolver/master/docs/logo-dif.png)

# Universal Resolver Driver: did:com

This is a [Universal Resolver](https://github.com/decentralized-identity/universal-resolver/) driver for **did:com** identifiers.

## Specifications

* [Decentralized Identifiers](https://w3c.github.io/did-core/)
* [DID Method Specification](https://docs.commercio.network/modules/did/)

## Example DIDs

```
did:com:1l6zglh8pvcrjtahsvds2qmfpn0hv83vn8f9cf3
did:com:17rhmdzlv0zjuahw4mvpfhf3u5tuwyjhr4m06dr
```

## Build and Run (Docker)

```
docker build -f ./Dockerfile . -t commercionetwork/driver-did-com
docker run -p 8080:8080 commercionetwork/driver-did-com
curl -X GET http://localhost:8080/1.0/identifiers/did:com:1l6zglh8pvcrjtahsvds2qmfpn0hv83vn8f9cf3
```

### Network specification (Docker)

By default, the docker script connects to the main network of Commercio.
You can specify an alternative network as follows:

```
docker run -p 8080:8080 -e NETWORK=url commercionetwork/driver-did-com
```

## Build (native Java)

Maven build:

    mvn clean install

## Driver Environment Variables

The driver recognizes the following environment variable:

### `uniresolver_driver_did_com_network`

 * Specifies the URL of the Commercio network to contact.
 * Default value: https://lcd-mainnet.commercio.network
