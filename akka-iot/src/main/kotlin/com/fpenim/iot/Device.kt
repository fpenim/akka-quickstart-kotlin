package com.fpenim.iot

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive
import java.util.OptionalDouble

class Device(context: ActorContext<Command>) : AbstractBehavior<Device.Command>(context) {
    interface Command

    data class ReadTemperature(val requestId: Long, val replyTo: ActorRef<RespondTemperature>): Command

    data class RespondTemperature(val requestId: Long, val value: OptionalDouble): Command

    data class RecordTemperature(val requestId: Long, val value: Double, val replyTo: ActorRef<TemperatureRecorded>): Command

    data class TemperatureRecorded(val requestId: Long): Command

    enum class Passive: Command {
        INSTANCE
    }

    private lateinit var groupId: String
    private lateinit var deviceId: String

    private var lastTemperatureReading: OptionalDouble = OptionalDouble.empty()

    private constructor(context: ActorContext<Command>, groupId: String, deviceId: String): this(context) {
        this.groupId = groupId
        this.deviceId = deviceId
        context.log.info("Device actor $groupId-$deviceId started")
    }

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
            .onMessage(ReadTemperature::class.java, this::onReadTemperature)
            .onMessage(RecordTemperature::class.java, this::onRecordTemperature)
            .onMessage(Passive::class.java) { Behaviors.stopped() }
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()

    private fun onReadTemperature(message: ReadTemperature): Behavior<Command> {
        message.replyTo.tell(RespondTemperature(message.requestId, lastTemperatureReading))
        return this
    }

    private fun onRecordTemperature(message: RecordTemperature): Behavior<Command> {
        lastTemperatureReading = OptionalDouble.of(message.value)
        context.log.info("Recorded temperature reading ${message.value} with ${message.requestId}")
        message.replyTo.tell(TemperatureRecorded(message.requestId))
        return this
    }

    private fun onPostStop(): Device {
        context.log.info("Device actor $groupId-$deviceId stopped")
        return  this
    }

    companion object {
        fun create(groupId: String, deviceId: String): Behavior<Command> =
                Behaviors.setup { context -> Device(context, groupId, deviceId) }
    }
}
