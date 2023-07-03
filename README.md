# Solutions for Gossip Glomers Challenges in Scala using ZIO-Maelstrom

What is Maelstrom?

https://github.com/jepsen-io/maelstrom

Gossip Glomers Challenges?

https://fly.io/dist-sys/

ZIO-Maelstrom?

https://zio-maelstrom.bilal-fazlani.com/

---

## Contents

| Challenge                                                                           | Solution                                                                                               |
| ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| [3d: Efficient Broadcast, Part I](https://fly.io/dist-sys/3d/)                      | [/efficient-broadcast-1/](/efficient-broadcast-1/)                                                     |
| [3e: Efficient Broadcast, Part II](https://fly.io/dist-sys/3e/)                     | [/efficient-broadcast-2/](/efficient-broadcast-2/)                                                     |
| [4: Grow-Only Counter](https://fly.io/dist-sys/4/)                                  | [/grow-only-counter/](/grow-only-counter/)                                                             |
| [5a: Single-Node Kafka-Style Log](https://fly.io/dist-sys/5a/)                      | [/kafka-style-log/](/kafka-style-log/)                                                                 |
| [5b: Multi-Node Kafka-Style Log](https://fly.io/dist-sys/5b/)                       | [/multi-node-kafka-style-log/](/multi-node-kafka-style-log/)                                           |
| [5c: Efficient Kafka-Style Log](https://fly.io/dist-sys/5c/)                        | [/efficient-kafka-style-log/](/efficient-kafka-style-log/)                                             |
| [6a: Single Node, Totally Available Transactions](https://fly.io/dist-sys/6a/)      | [/single-node-totally-available-transactions/](/single-node-totally-available-transactions/)           |
| [6b: Totally-Available, Read Uncommitted Transactions](https://fly.io/dist-sys/6b/) | [/totally-available-read-uncommitted-transactions/](/totally-available-read-uncommitted-transactions/) |
| [6c: Totally-Available, Read Committed Transactions](https://fly.io/dist-sys/6c/)   | [/totally-available-read-committed-transactions/](/totally-available-read-committed-transactions/)     |

## Setup

Beyond challenge 3c, using running jar files does not work because they start too slow and the tests timeout. 
So, I needed to compile them to native binaries using graalvm

In order to create a native binary, I first run the jar file with a very small load with native image agent. 
This captures reflection configs in `resources/META-INF/native-image/`. I then run `natveImage` command to create the native binary in `target` dir.

```bash
#replace with the challenge you want to compile
sbt efficient-broadcast-1/generateReflectConfig
 
sbt efficient-broadcast-1/nativeImage
```

I can now test the native binary with actual load using maelstrom

```
maelstrom test -w broadcast --bin efficient-broadcast-1/target/efficient-broadcast-1-darwin-x86_64 --node-count 25 --time-limit 20 --rate 100 --latency 100
```