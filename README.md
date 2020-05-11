# Akka Kotlin

Akka is a toolkit and runtime for building highly concurrent, distributed, and fault-tolerant event-driven applications 
on the JVM.

Actors are the unit of execution in Akka. The Actor model is an abstraction that makes it easier to write correct 
concurrent, parallel and distributed systems. The Hello World example illustrates Akka basics.

## akka-quickstart-kotlin

Disclaimer: This kotlin project is based on the [akka-quickstart-java](https://developer.lightbend.com/guides/akka-quickstart-java/index.html) 
example project made available by [Lightbend](https://www.lightbend.com/).

### Running the application

Run this application with Maven:
```bash
$ mvn compile exec:exec
```

### Experiments

Under `com.fpenim.experiments` you will find a number of experiments that aim to highlight the actor architecture. 
Each one of them has its own main function and should be run individually. 

These examples are based on the ones found in [this guide](https://doc.akka.io/docs/akka/current/typed/guide/tutorial_1.html).

- `Hierarchy` - understand what the actor hierarchy looks like
- `StartStop` - understand the actor life cycle
- `Supervision` - understand the supervisor strategy

## akka-iot

Disclaimer: This kotlin project is based on the [akka iot example project](https://doc.akka.io//docs/akka/current/typed/guide/tutorial.html) 
 made available by [Lightbend](https://www.lightbend.com/).
 
TBC