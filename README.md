![DIF Logo](https://raw.githubusercontent.com/decentralized-identity/universal-resolver/master/docs/logo-dif.png)

# Universal Resolver Driver: did:com

This is a [Universal Resolver](https://github.com/decentralized-identity/universal-resolver/) driver for **did:com** identifiers.

## Specifications

* [Decentralized Identifiers](https://w3c.github.io/did-core/)
* [DID Method Specification](https://docs.commercio.network/modules/did/)

## Example DIDs

```
did:com:10wkpeq0y5we9z7gygxvhydw53zareyg9d7tcd3
did:com:109l7hvxq4kk0mtarfcl3gy3cdxuypdmt6j50ln
```

## Build and Run (Docker)

```
docker build -f ./docker/Dockerfile . -t universalresolver/driver-did-com
docker run -p 8080:8080 universalresolver/driver-did-com
curl -X GET http://localhost:8080/1.0/identifiers/did:com:10wkpeq0y5we9z7gygxvhydw53zareyg9d7tcd3
```

### Network specification (Docker)

By default, the docker script connects to the development network of Commercio.
You can specify an alternative network as follows:

```
docker run -p 8080:8080 -e NETWORK=url universalresolver/driver-did-com
```

## Build (native Java)

Maven build:

    mvn clean install

