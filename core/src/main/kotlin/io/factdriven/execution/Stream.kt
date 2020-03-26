package io.factdriven.execution

import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
data class StreamId (val name: String, val id: String?) {
    constructor(type: KClass<*>, id: String? = null): this(type.name.toString(), id)
}