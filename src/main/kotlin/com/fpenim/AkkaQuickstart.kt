package com.fpenim

import akka.actor.typed.ActorSystem
import com.fpenim.greetings.GreeterMain
import java.io.IOException

fun main(args: Array<String>) {
    val greeterMain: ActorSystem<GreeterMain.SayHello> = ActorSystem.create(GreeterMain.create(), "helloakka")

    greeterMain.tell(GreeterMain.SayHello("Flavia"))

    try {
        println(">>> Press ENTER to exit <<<")
        System.`in`.read()
    } catch (ignored: IOException) {
    } finally {
        greeterMain.terminate()
    }
}

