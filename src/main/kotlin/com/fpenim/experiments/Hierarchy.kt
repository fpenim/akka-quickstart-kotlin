package com.fpenim.experiments

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import com.fpenim.utils.pressEnterToExit

fun main() {
    val hierarchyMain = ActorSystem.create(Main.create(), "imYourFather")
    hierarchyMain.tell("start")

    pressEnterToExit(hierarchyMain)
}


class Main(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    override fun createReceive(): Receive<String> =
            newReceiveBuilder().onMessageEquals("start", this::start).build()

    private fun start(): Behavior<String> {
        val firstRef: ActorRef<String> = context.spawn(PrintActorRefActor.create(), "first-actor")
        println("First : $firstRef")
        firstRef.tell(PRINT_IT)
        return Behaviors.same()
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::Main)
    }
}


class PrintActorRefActor(context: ActorContext<String>) : AbstractBehavior<String>(context) {

    override fun createReceive(): Receive<String> =
            newReceiveBuilder().onMessageEquals(PRINT_IT, this::printIt).build()

    private fun printIt(): Behavior<String> {
        val secondRef: ActorRef<String> = context.spawn(Behaviors.empty(), "second-actor")
        println("Second: $secondRef")
        return this
    }

    companion object {
        fun create(): Behavior<String> = Behaviors.setup(::PrintActorRefActor)
    }
}

private const val PRINT_IT = "printit"

