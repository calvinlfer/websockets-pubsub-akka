# WebSockets and Publish-Subscribe with Akka #
This is a minimal demonstration on how you can build a streaming system that is able to provide bi-directional 
soft-realtime updates to frontends via WebSockets in order to build better UIs. 

The following technologies are used:
* WebSockets
* Akka
    - Actors
    - Streams
    - Distributed Publish Subscribe
    - HTTP (WebSocket flows)

## Usage ##
Start the application via `sbt run` which will run on `http://127.0.0.1:9001`

Use a WebSocket UI like [Dark WebSocket Terminal](https://dwst.github.io) to connect to the system. For example,
`/connect ws://127.0.0.1:9001/ws/bob` will create a new WebSocket user that connects to the system. You can send
calculation commands and receive calculation results for your commands and for other users interacting with the system.

For example, here's an addition command:
```json
{"operandA":1000,"operandB":10,"operator":"+"}
```

The system will respond with
```json
{"description":"1000 + 10","answer":1010,"username":"bob"}
```

If multiple users connect to the system, for example:
`/connect ws://127.0.0.1:9001/ws/bob`
`/connect ws://127.0.0.1:9001/ws/bill`

`bob` will receive results for himself as well as `bill` and the same goes for `bill`. Let's see an example

`bob`'s perspective:
```
/connect ws://127.0.0.1:9001/ws/bob
sent: {"operandA":1000,"operandB":10,"operator":"+"}
received: {"description":"1000 + 10","answer":1010,"username":"bob"}
received: {"description":"Error processing request: / by zero","answer":null,"username":"bill"}
received: Keep-alive message sent to WebSocket recipient
```

`bill`'s perspective:
```
/connect ws://127.0.0.1:9001/ws/bill
received: {"description":"1000 + 10","answer":1010,"username":"bob"}
received: Keep-alive message sent to WebSocket recipient
sent: {"operandA":1000,"operandB":0,"operator":"/"}
received: {"description":"Error processing request: / by zero","answer":null,"username":"bill"}
received: Keep-alive message sent to WebSocket recipient
```

## Architecture ##

### Communicating with WebSocket clients ###
A `WebSocketUser` actor is created for every single WebSocket user connection and the lifecycle of the `WebSocketUser`
actor is tied to the WebSocket connection. WebSockets in Akka HTTP are represented by an Akka Streams 
`Flow[Message, Message, Any]`. You can integrate Streams with Actors using `Sink.actorRef` (use `Sink.actorRefWithAck` 
for back-pressure) and `Source.actorRef` (use `Source.queue` for back-pressure). `Sink.actorRef` expects you to provide
an existing Actor and any messages you send to the Sink will end up being sent to that Actor. The `Sink` represents a 
way to receive messages from WebSocket clients. In order to send messages to the WebSocket clients, you need a `Source`.
We use `Source.actorRef` which produces an `ActorRef` that is a handle to the WebSocket client. Any messages sent to 
that `ActorRef` will get sent on the Stream to the WebSocket client. The `WebSocketUser` uses this `ActorRef` handle to
communicate back to the WebSocket client. The `WebSocketUser` actor is able to receive and send messages to the WebSocket
clients.

### Distributing events ###
WebSocket clients who are connected to the system can send commands and receive results for those commands. In addition
to receiving events for your own commands, you receive events for other users interacting with the system. This 
capability is provided by Akka's Distributed Publish Subscribe module and is designed to work for a cluster of machines
representing your system. The `WebSocketUser` actor subscribes to an events topic and is a publisher as well as a 
subscriber to this topic. Whenever the `WebSocketUser` actor performs a calculation, it publishes the result to the 
topic in addition to sending the result back to the WebSocket user. Please note that Distributed Publish Subscribe 
provides at-most-once-delivery semantics.
