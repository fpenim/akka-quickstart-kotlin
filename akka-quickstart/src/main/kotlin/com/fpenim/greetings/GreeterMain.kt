package com.fpenim.greetings

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class GreeterMain(context: ActorContext<SayHello>) : AbstractBehavior<GreeterMain.SayHello>(context) {

    private val greeter: ActorRef<Greeter.Greet> = context.spawn(Greeter.create(), "greeter")

    data class SayHello(val name: String)

    override fun createReceive(): Receive<SayHello> =
            newReceiveBuilder().onMessage(SayHello::class.java, this::onSayHello).build()

    private fun onSayHello(command: SayHello): Behavior<SayHello> {
        val replyTo: ActorRef<Greeter.Greeted> = context.spawn(GreeterBot.create(3), command.name)
        greeter.tell(Greeter.Greet(command.name, replyTo))
        return this
    }

    companion object {
        fun create(): Behavior<SayHello> = Behaviors.setup(::GreeterMain)
    }
}