package tech.aliorpse.tyriskic

import org.ntqqrev.milky.Event
import org.ntqqrev.milky.IncomingMessage
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.saltify.core.SaltifyApplication
import org.ntqqrev.saltify.entity.SaltifyComponentType
import org.ntqqrev.saltify.util.coroutine.saltifyComponent
import tech.aliorpse.tyriskic.plugin.biliparser.biliParser
import java.io.PrintStream
import java.nio.charset.StandardCharsets

suspend fun main() {
    System.setOut(PrintStream(System.`out`, true, StandardCharsets.UTF_8))

    SaltifyApplication {
        addressBase = "http://localhost:3355"

        install(biliParser)

        plugin {
            on<Event.MessageReceive> {
                when (val data = it.data) {
                    is IncomingMessage.Group -> {
                        println("Group message from ${data.senderId} in ${data.group.groupId}:")
                        println(milkyJsonModule.encodeToString(data.segments))
                    }
                    is IncomingMessage.Friend -> {
                        println("Private message from ${data.senderId}:")
                        println(milkyJsonModule.encodeToString(data.segments))
                    }
                    else -> {}
                }
            }
        }
    }.use { client ->
        println("Tyriskic starting...")
        client.connectEvent()

        client.exceptionFlow.collect { (context, e) ->
            val component = context.saltifyComponent!!

            when (component.type) {
                SaltifyComponentType.Application -> throw e
                else -> println(
                    "Component ${component.name}(${component.type}) occurred an exception: " +
                        e.stackTraceToString()
                )
            }
        }
    }
}
