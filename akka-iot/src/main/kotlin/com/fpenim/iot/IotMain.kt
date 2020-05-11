package com.fpenim.iot

import akka.actor.typed.ActorSystem

fun main() {
    ActorSystem.create(IotSupervisor.create(), "iot-system")
}
