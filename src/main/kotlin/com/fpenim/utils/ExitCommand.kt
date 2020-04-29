package com.fpenim.utils

import akka.actor.typed.ActorSystem
import java.io.IOException

fun <T> pressEnterToExit(actorSystem: ActorSystem<T>) {
    try {
        println(">>> Press ENTER to exit <<<")
        System.`in`.read()
    } catch (ignored: IOException) {
    } finally {
        actorSystem.terminate()
    }
}