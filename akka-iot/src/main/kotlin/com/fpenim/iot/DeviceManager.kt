package com.fpenim.iot

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class DeviceManager(context: ActorContext<Command>): AbstractBehavior<DeviceManager.Command>(context) {
    init {
        context.log.info("DeviceManager started")
    }

    interface Command

    data class RequestTrackDevice(val groupId: String, val deviceId: String, val replyTo: ActorRef<DeviceRegistered>): Command, DeviceGroup.Command

    data class DeviceRegistered(val device: ActorRef<Device.Command>): Command

    data class RequestDeviceList(val requestId: Long, val groupId: String, val replyTo: ActorRef<ReplyDeviceList>): Command, DeviceGroup.Command

    data class ReplyDeviceList(val requestId: Long, val ids: Set<String>): Command

    data class DeviceGroupTerminated(val groupId: String): Command

    private val groupIdToActor: MutableMap<String, ActorRef<DeviceGroup.Command>> = mutableMapOf()

    private fun onTrackDevice(message: RequestTrackDevice): DeviceManager {

        val ref = groupIdToActor[message.groupId]
        if (ref != null) {
            ref.tell(message)
        } else {
            context.log.info("Creating device group actor for ${message.groupId}")
            val groupActor = context.spawn(DeviceGroup.create(message.groupId), "group-${message.groupId}")
            context.watchWith(groupActor, DeviceGroupTerminated(message.groupId))
            groupIdToActor[message.groupId] = groupActor
            groupActor.tell(message)
        }
        return this
    }

    private fun onRequestDeviceList(message: RequestDeviceList): DeviceManager {
        val ref = groupIdToActor[message.groupId]
        if (ref != null) {
            ref.tell(message)
        } else {
            message.replyTo.tell(ReplyDeviceList(message.requestId, emptySet()))
        }
        return this
    }

    private fun onTerminated(message: DeviceGroupTerminated): DeviceManager {
        context.log.info("Device group actor for ${message.groupId} has been terminated")
        groupIdToActor.remove(message.groupId)
        return this
    }

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
            .onMessage(RequestTrackDevice::class.java, this::onTrackDevice)
            .onMessage(RequestDeviceList::class.java, this::onRequestDeviceList)
            .onMessage(DeviceGroupTerminated::class.java, this::onTerminated)
            .onSignal(PostStop::class.java) { onPostStop() }
            .build()

    private fun onPostStop(): DeviceManager {
        context.log.info("DeviceManager stopped")
        return this
    }

    companion object {
        fun create(): Behavior<Command> = Behaviors.setup(::DeviceManager)
    }
}