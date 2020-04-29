package com.fpenim

import akka.actor.typed.ActorSystem
import com.fpenim.greetings.GreeterMain
import com.fpenim.utils.pressEnterToExit

fun main() {
    val greeterMain: ActorSystem<GreeterMain.SayHello> = ActorSystem.create(GreeterMain.create(), "helloakka")

    greeterMain.tell(GreeterMain.SayHello("Flavia"))

    pressEnterToExit(greeterMain)
}

