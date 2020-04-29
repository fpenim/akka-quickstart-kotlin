package com.fpenim.experiments

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.fpenim.utils.pressEnterToExit

fun main() {
    val startStopMain = ActorSystem.create(StartStopMain.create(), "startstop")
    startStopMain.tell("start")

    pressEnterToExit(startStopMain)
}


class StartStopMain(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    override fun createReceive(): Receive<String> =
            newReceiveBuilder().onMessageEquals("start", this::start).build()

    private fun start(): Behavior<String> {
        val actorOneRef: ActorRef<String> = context.spawn(StartStopActorOne.create(), "first")
        actorOneRef.tell("stop")
        return this
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::StartStopMain)
    }
}


class StartStopActorOne(context: ActorContext<String>) : AbstractBehavior<String>(context) {
    init {
        println("first started")
        context.spawn(StartStopActorTwo.create(), "second")
    }

    override fun createReceive(): Receive<String> = newReceiveBuilder()
            .onMessageEquals("stop") { Behaviors.stopped() }
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()

    private fun onPostStop(): Behavior<String> {
        println("first stopped")
        return this
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::StartStopActorOne)
    }
}


class StartStopActorTwo(context: ActorContext<String>) : AbstractBehavior<String>(context) {
    init {
        println("second started")
    }

    override fun createReceive(): Receive<String> = newReceiveBuilder()
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()

    private fun onPostStop(): Behavior<String> {
        println("second stopped")
        return this
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::StartStopActorTwo)
    }
}