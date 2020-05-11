package com.fpenim.iot

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class IotSupervisor(context: ActorContext<Any>) : AbstractBehavior<Any>(context) {

    init {
        context.log.info("IoT Application started")
    }

    override fun createReceive(): Receive<Any> = newReceiveBuilder()
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()

    private fun onPostStop(): IotSupervisor {
        context.log.info("IoT Application stopped")
        return this
    }

    companion object {
        fun create(): Behavior<Any> = Behaviors.setup(::IotSupervisor)
    }
}