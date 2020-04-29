package com.fpenim.experiments

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.PreRestart
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.fpenim.utils.pressEnterToExit
import java.lang.RuntimeException

fun main() {
    val supervisionMain = ActorSystem.create(SupervisionMain.create(), "supervisor")
    supervisionMain.tell("start")

    pressEnterToExit(supervisionMain)
}


class SupervisionMain(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    override fun createReceive(): Receive<String> =
            newReceiveBuilder().onMessageEquals("start", this::start).build()

    private fun start(): Behavior<String> {
        val supervisingRef = context.spawn(SupervisingActor.create(), "supervising-actor")
        supervisingRef.tell("failChild")
        return this
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::SupervisionMain)
    }

}


class SupervisingActor(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    private val child: ActorRef<String> = context.spawn(
            Behaviors.supervise(SupervisedActor.create()).onFailure(SupervisorStrategy.restart()), "supervised-actor"
    )

    override fun createReceive(): Receive<String> = newReceiveBuilder()
            .onMessageEquals("failChild") { onFailChild() }
            .build()

    private fun onFailChild(): Behavior<String> {
        child.tell("fail")
        return this
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::SupervisingActor)
    }
}


class SupervisedActor(context: ActorContext<String>) : AbstractBehavior<String>(context) {
    init {
        println("Supervised actor started")
    }

    override fun createReceive(): Receive<String> = newReceiveBuilder()
            .onMessageEquals("fail") { fail() }
            .onSignal(PreRestart::class.java) { preRestart() }
            .onSignal(PostStop::class.java) { postStop() }
            .build()

    private fun fail(): Behavior<String> {
        println("supervised actor fails now")
        throw RuntimeException("I failed!")
    }

    private fun preRestart(): Behavior<String> {
        println("supervised will be restarted")
        return this
    }

    private fun postStop(): Behavior<String> {
        println("supervised stopped")
        return this
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::SupervisedActor)
    }
}