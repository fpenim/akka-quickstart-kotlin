package com.fpenim.iot

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.javadsl.AbstractBehavior
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.javadsl.Receive

class DeviceGroup(context: ActorContext<Command>) : AbstractBehavior<DeviceGroup.Command>(context) {
    interface Command

    data class DeviceTerminated(val device: ActorRef<Device.Command>, val groupId: String, val deviceId: String): Command

    private lateinit var groupId: String

    private val deviceIdToActor: MutableMap<String, ActorRef<Device.Command>> = mutableMapOf()

    private constructor(context: ActorContext<Command>, groupId: String): this(context) {
        this.groupId = groupId
        context.log.info("DeviceGroup $groupId started")
    }

    private fun onTrackDevice(trackMessage: DeviceManager.RequestTrackDevice): DeviceGroup {
        if (this.groupId == trackMessage.groupId) {
            val deviceActor = deviceIdToActor[trackMessage.deviceId]
            if (deviceActor != null) {
                trackMessage.replyTo.tell(DeviceManager.DeviceRegistered(deviceActor))
            } else {
                context.log.info("Creating device actor for ${trackMessage.deviceId}")

                val deviceActor =
                        context.spawn(Device.create(groupId, trackMessage.deviceId), "device-${trackMessage.deviceId}")

                context.watchWith(deviceActor, DeviceTerminated(deviceActor,groupId, trackMessage.deviceId))

                deviceIdToActor[trackMessage.deviceId] = deviceActor
                trackMessage.replyTo.tell(DeviceManager.DeviceRegistered(deviceActor))
            }
        } else {
            context.log.info("Ignoring TrackDevice request for ${trackMessage.groupId}. This actor is responsible for $groupId.")
        }
        return this
    }

    private fun onDeviceList(request: DeviceManager.RequestDeviceList): DeviceGroup {
        request.replyTo.tell(DeviceManager.ReplyDeviceList(request.requestId, deviceIdToActor.keys))
        return this
    }

    private fun onPostStop(): DeviceGroup {
        context.log.info("DeviceGroup $groupId stopped.")
        return this
    }

    private fun onTerminated(terminated: DeviceTerminated): DeviceGroup {
        context.log.info("Device actor for ${terminated.deviceId} has been terminated.")
        deviceIdToActor.remove(terminated.deviceId)
        return this
    }

    override fun createReceive(): Receive<Command> = newReceiveBuilder()
            .onMessage(DeviceManager.RequestTrackDevice::class.java, this::onTrackDevice)
            .onMessage(DeviceManager.RequestDeviceList::class.java,
                    { request -> request.groupId == this.groupId },
                    this::onDeviceList)
            .onMessage(DeviceTerminated::class.java, this::onTerminated)
            .onSignal(PostStop::class.java) { _ -> onPostStop() }
            .build()

    companion object {
        fun create(groupId: String): Behavior<Command> =
                Behaviors.setup { context -> DeviceGroup(context, groupId) }
    }
}