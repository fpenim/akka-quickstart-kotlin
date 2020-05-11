package com.fpenim.iot

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.testkit.typed.javadsl.TestProbe
import akka.actor.typed.ActorRef
import org.junit.ClassRule
import org.junit.Test
import java.util.OptionalDouble
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DeviceTest {

    @Test
    fun testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
        val probe: TestProbe<Device.RespondTemperature> = testKit.createTestProbe(Device.RespondTemperature::class.java)
        val deviceActor: ActorRef<Device.Command> = testKit.spawn(Device.create("living-room", "ac"))

        deviceActor.tell(Device.ReadTemperature(42L, probe.ref))
        val response: Device.RespondTemperature = probe.receiveMessage()

        assertEquals(42L, response.requestId)
        assertEquals(OptionalDouble.empty(), response.value)
    }

    @Test
    fun testReplyWithLatestTemperatureReading() {
        val recordProbe: TestProbe<Device.TemperatureRecorded> = testKit.createTestProbe(Device.TemperatureRecorded::class.java)
        val readProbe: TestProbe<Device.RespondTemperature> = testKit.createTestProbe(Device.RespondTemperature::class.java)
        val deviceActor: ActorRef<Device.Command> = testKit.spawn(Device.create("master-suite", "1"))

        deviceActor.tell(Device.RecordTemperature(1L, 23.4, recordProbe.ref))
        assertEquals(1L, recordProbe.receiveMessage().requestId)

        deviceActor.tell(Device.ReadTemperature(2L, readProbe.ref))
        val response1 = readProbe.receiveMessage()
        assertEquals(2L, response1.requestId)
        assertEquals(OptionalDouble.of(23.4), response1.value)

        deviceActor.tell(Device.RecordTemperature(3L, 18.0, recordProbe.ref))
        assertEquals(3L, recordProbe.receiveMessage().requestId)

        deviceActor.tell(Device.ReadTemperature(4L, readProbe.ref))
        val response2 = readProbe.receiveMessage()
        assertEquals(4L, response2.requestId)
        assertEquals(OptionalDouble.of(18.0), response2.value)
    }

    @Test
    fun testReplyToRegistrationRequests() {
        val registeredProbe: TestProbe<DeviceManager.DeviceRegistered> = testKit.createTestProbe(DeviceManager.DeviceRegistered::class.java)
        val groupActor: ActorRef<DeviceGroup.Command> = testKit.spawn(DeviceGroup.create("group"))

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1", registeredProbe.ref))
        val deviceRegistered1 = registeredProbe.receiveMessage()

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2", registeredProbe.ref))
        val deviceRegistered2 = registeredProbe.receiveMessage()
        assertNotEquals(deviceRegistered1.device, deviceRegistered2.device)

        val recordProbe: TestProbe<Device.TemperatureRecorded> = testKit.createTestProbe(Device.TemperatureRecorded::class.java)
        deviceRegistered1.device.tell(Device.RecordTemperature(1L, 23.4, recordProbe.ref))
        assertEquals(1L, recordProbe.receiveMessage().requestId)

        deviceRegistered2.device.tell(Device.RecordTemperature(3L, 18.0, recordProbe.ref))
        assertEquals(3L, recordProbe.receiveMessage().requestId)
    }

    @Test
    fun testIgnoreWrongRegistrationRequests() {
        val registeredProbe: TestProbe<DeviceManager.DeviceRegistered> = testKit.createTestProbe(DeviceManager.DeviceRegistered::class.java)
        val groupActor: ActorRef<DeviceGroup.Command> = testKit.spawn(DeviceGroup.create("group"))

        groupActor.tell(DeviceManager.RequestTrackDevice("wrong-group", "device", registeredProbe.ref))
        registeredProbe.expectNoMessage()
    }

    @Test
    fun testReturnSameActorForSameDeviceId() {
        val registeredProbe: TestProbe<DeviceManager.DeviceRegistered> = testKit.createTestProbe(DeviceManager.DeviceRegistered::class.java)
        val groupActor: ActorRef<DeviceGroup.Command> = testKit.spawn(DeviceGroup.create("group"))

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1", registeredProbe.ref))
        val deviceRegistered1 = registeredProbe.receiveMessage()

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1", registeredProbe.ref))
        val deviceRegistered2 = registeredProbe.receiveMessage()

        assertEquals(deviceRegistered1.device, deviceRegistered2.device)
    }

    @Test
    fun testListActiveDevices() {
        val registeredProbe: TestProbe<DeviceManager.DeviceRegistered> = testKit.createTestProbe(DeviceManager.DeviceRegistered::class.java)
        val groupActor: ActorRef<DeviceGroup.Command> = testKit.spawn(DeviceGroup.create("group"))

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1", registeredProbe.ref))
        registeredProbe.receiveMessage()

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2", registeredProbe.ref))
        registeredProbe.receiveMessage()

        val deviceListProbe: TestProbe<DeviceManager.ReplyDeviceList> = testKit.createTestProbe(DeviceManager.ReplyDeviceList::class.java)

        groupActor.tell(DeviceManager.RequestDeviceList(1L, "group", deviceListProbe.ref))
        val reply = deviceListProbe.receiveMessage()

        assertEquals(1L, reply.requestId)
        assertEquals(setOf("device1", "device2"), reply.ids)
    }

    @Test
    fun testListActiveDevicesAfterOneShutsDown() {
        val registeredProbe: TestProbe<DeviceManager.DeviceRegistered> = testKit.createTestProbe(DeviceManager.DeviceRegistered::class.java)
        val groupActor: ActorRef<DeviceGroup.Command> = testKit.spawn(DeviceGroup.create("group"))

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1", registeredProbe.ref))
        val reg1 = registeredProbe.receiveMessage()

        groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2", registeredProbe.ref))
        val reg2 = registeredProbe.receiveMessage()

        val toShutDown = reg1.device

        val deviceListProbe: TestProbe<DeviceManager.ReplyDeviceList> = testKit.createTestProbe(DeviceManager.ReplyDeviceList::class.java)

        groupActor.tell(DeviceManager.RequestDeviceList(1L, "group", deviceListProbe.ref))
        val reply = deviceListProbe.receiveMessage()

        assertEquals(1L, reply.requestId)
        assertEquals(setOf("device1", "device2"), reply.ids)

        toShutDown.tell(Device.Passive.INSTANCE)
        registeredProbe.expectTerminated(toShutDown, registeredProbe.remainingOrDefault)

        registeredProbe.awaitAssert {
            groupActor.tell(DeviceManager.RequestDeviceList(2L, "group", deviceListProbe.ref))
            val reply = deviceListProbe.receiveMessage()

            assertEquals(2L, reply.requestId)
            assertEquals(setOf("device2"), reply.ids)
        }
    }
}

@ClassRule
val testKit = TestKitJunitResource()