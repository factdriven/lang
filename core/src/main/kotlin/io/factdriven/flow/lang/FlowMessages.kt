package io.factdriven.flow.lang

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.security.MessageDigest
import java.math.BigInteger
import java.util.*
import kotlin.reflect.KClass


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
typealias Fact = Any
typealias FactType<F> = KClass<out F>
typealias FactName = String

typealias MessageId = String
typealias MessageTarget = Pair<AggregateType, AggregateId>

typealias PropertyName = String
typealias PropertyValue = Any?

typealias Messages = List<Fact>
typealias MessagePatterns = Set<MessagePattern>

data class Message<F: Fact>(

    val id: MessageId,
    val name: FactName,
    val fact: F

) {

    fun toJson(): String {
        return jacksonObjectMapper().writeValueAsString(this)
    }

    companion object {

        fun <F: Fact> create(fact: F): Message<F> {
            return Message(UUID.randomUUID().toString(), fact::class.java.simpleName, fact)
        }

        fun <F: Fact> fromJson(json: String, factType: FactType<F>): Message<F> {
            val mapper = jacksonObjectMapper()
            mapper.registerSubtypes(factType.java)
            val type = mapper.typeFactory.constructParametricType(Message::class.java, factType.java)
            return mapper.readValue(json, type)
        }

    }

}

data class MessagePattern(
    val name: FactName,
    val properties: Map<PropertyName, PropertyValue> = emptyMap()
) {

    constructor(
        type: FactType<*>,
        properties: Map<PropertyName, PropertyValue> = emptyMap()
    ): this(type.simpleName!!, properties) // TODO simple name is just fallback

    val hash: String

    init {
        val buffer = StringBuffer(name)
        properties.toSortedMap().forEach {
            buffer.append("|").append(it.key).append("=").append(it.value)
        }
        hash = hash(buffer.toString())
    }

    companion object {

        val digest = MessageDigest.getInstance("MD5")

        private fun hash(input: String): String {
            val bytes = digest.digest(input.toByteArray())
            return String.format("%0" + (bytes.size shl 1) + "x", BigInteger(1, bytes))
        }

    }

}

fun Fact.getProperty(propertyName: PropertyName): Any? {
    return javaClass.getDeclaredField(propertyName).let {
        it.isAccessible = true
        val value = it.get(this)
        return@let value;
    }
}
