package com.fpenim.greetings

import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class GreeterBot(context: ActorContext<Greeter.Greeted>, val max: Int) : AbstractBehavior<Greeter.Greeted>(context) {

    private var greetingCounter: Int = 0

    override fun createReceive(): Receive<Greeter.Greeted> =
            newReceiveBuilder().onMessage(Greeter.Greeted::class.java, this::onGreeted).build()

    private fun onGreeted(message: Greeter.Greeted): Behavior<Greeter.Greeted> {
        greetingCounter++
        context.log.info("Greeting $greetingCounter for ${message.whom}")

        return if (greetingCounter == max) {
            Behaviors.stopped()
        } else {
            message.from.tell(Greeter.Greet(message.whom, context.self))
            this
        }
    }

    companion object {
        fun create(max: Int): Behavior<Greeter.Greeted> = Behaviors.setup { context -> GreeterBot(context, max) }
    }
}