# Solutions for Gossip Glomers Challenges in Scala using ZIO-Maelstrom

What is Maelstrom?

https://github.com/jepsen-io/maelstrom

Gossip Glomers Challenges?

https://fly.io/dist-sys/

ZIO-Maelstrom?

https://zio-maelstrom.bilal-fazlani.com/

---

## Contents

| Status                | Challenge                                                                           | Source code                                                                                            |
| --------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| :green_square:        | [1: Echo](https://fly.io/dist-sys/1/)                                               | [/echo/](/echo/)                                                                                       |
| :green_square:        | [2: Unique ID Generation](https://fly.io/dist-sys/2/)                               | [/unique-id-generation/](/unique-id-generation/)                                                       |
| :green_square:        | [3a: Single-Node Broadcast](https://fly.io/dist-sys/3a/)                            | [/single-node-broadcast/](/single-node-broadcast/)                                                     |
| :green_square:        | [3b: Multi-Node Broadcast](https://fly.io/dist-sys/3b/)                             | [/multi-node-broadcast/](/multi-node-broadcast/)                                                       |
| :green_square:        | [3c: Fault Tolerant Broadcast](https://fly.io/dist-sys/3c/)                         | [/fault-tolerant-broadcast/](/fault-tolerant-broadcast/)                                               |
| :green_square:        | [3d: Efficient Broadcast, Part I](https://fly.io/dist-sys/3d/)                      | [/efficient-broadcast-1/](/efficient-broadcast-1/)                                                     |
| :yellow_circle:       | [3e: Efficient Broadcast, Part II](https://fly.io/dist-sys/3e/)                     | [/efficient-broadcast-2/](/efficient-broadcast-2/)                                                     |
| :white_square_button: | [4: Grow-Only Counter](https://fly.io/dist-sys/4/)                                  | [/grow-only-counter/](/grow-only-counter/)                                                             |
| :white_square_button: | [5a: Single-Node Kafka-Style Log](https://fly.io/dist-sys/5a/)                      | [/kafka-style-log/](/kafka-style-log/)                                                                 |
| :white_square_button: | [5b: Multi-Node Kafka-Style Log](https://fly.io/dist-sys/5b/)                       | [/multi-node-kafka-style-log/](/multi-node-kafka-style-log/)                                           |
| :white_square_button: | [5c: Efficient Kafka-Style Log](https://fly.io/dist-sys/5c/)                        | [/efficient-kafka-style-log/](/efficient-kafka-style-log/)                                             |
| :white_square_button: | [6a: Single Node, Totally Available Transactions](https://fly.io/dist-sys/6a/)      | [/single-node-totally-available-transactions/](/single-node-totally-available-transactions/)           |
| :white_square_button: | [6b: Totally-Available, Read Uncommitted Transactions](https://fly.io/dist-sys/6b/) | [/totally-available-read-uncommitted-transactions/](/totally-available-read-uncommitted-transactions/) |
| :white_square_button: | [6c: Totally-Available, Read Committed Transactions](https://fly.io/dist-sys/6c/)   | [/totally-available-read-committed-transactions/](/totally-available-read-committed-transactions/)     |

## Setup

Beyond challenge 3c, using running jar files does not work because they start too slow and the tests timeout. 
So, I needed to compile them to native binaries using graalvm

There are two commands available on every solution:

1. `maelstromRunAgent`

    In order to create a native binary, I first run the java application with a very small load from maelstrom. Using command `maelstromRunAgent`.
    This runs with graalvm agent and generates reflection configuration in `resources/META-INF/native-image/`

2. `makeNativeImage`

    After reflection configs are generated, I can now compile the application to a native binary using `makeNativeImage` command.

```bash
#replace with the challenge you want to compile
sbt efficient-broadcast-1/maelstromRunAgent
 
sbt efficient-broadcast-1/makeNativeImage
```

I can now test the native binary with actual load using maelstrom

```
maelstrom test -w broadcast --bin efficient-broadcast-1/target/efficient-broadcast-1-darwin-x86_64 --node-count 25 --time-limit 20 --rate 100 --latency 100
```