package io.factdriven.definition.api

import kotlin.reflect.KClass

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
interface Catching : Executing {

    val catching: KClass<*>

}
