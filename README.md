# kwormhole

A tool to help synchronize files between machines.

## API

Kwormhole uses HTTP as its underlying protocol.

### KFR

KFR (Kwormhole File Record) represents a file record of any specific time instant that synchronized by Kwormhole
application.

There are a unique identifier `{path}` for each KFR, and there are 3 variables providing the necessary synchronizing
data, which are `{size}`, `{time}` and `{hash}`.

In most of the requests, user needs to provide `{path}` for locating the corresponding KFR, and the response should
contain `{size}`, `{time}` and `{hash}` in the headers named `Record-Size`, `Record-Time` and `Record-Hash` (only
presents when the response is `200 OK`).

### `HEAD /kfr/{path}`

Requests the headers of a KFR.

The response headers contain `Record-Size`, `Record-Time` and `Record-Hash` of the corresponding KFR.

### `GET /kfr/{path}`

Same as `HEAD /kfr/{path}`, except the body is responded.

The response body is the binary content of the corresponding KFR.

### `PUT /kfr/{path}`

Creates new KFR or replaces an existing KFR.

The request headers must contain `Record-Size`, `Record-Time` and `Record-Hash` of the KSF.

The request body is the binary data of the KSF.

### `GET /all`

Requests all existing KFR's path.

The response body is a list containing all existing KFR's path.

### `WebSocket /event`

Listens for KFR changes.

Each packet contains the changed KFR's path.