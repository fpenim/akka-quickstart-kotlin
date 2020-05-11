package com.fpenim

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.testkit.typed.javadsl.TestProbe
import akka.actor.typed.ActorRef
import com.fpenim.greetings.Greeter
import org.junit.ClassRule
import org.junit.Test

class AkkaQuickstartTest {

    @Test
    fun testGreeterActorSendingOfGreeting() {
        val testProbe: TestProbe<Greeter.Greeted> = testKit.createTestProbe()
        val underTest: ActorRef<Greeter.Greet> = testKit.spawn(Greeter.create(), "greeter")

        underTest.tell(Greeter.Greet("Flavia", testProbe.ref))
        testProbe.expectMessage(Greeter.Greeted("Flavia", underTest))
    }

}

@ClassRule
val testKit = TestKitJunitResource()