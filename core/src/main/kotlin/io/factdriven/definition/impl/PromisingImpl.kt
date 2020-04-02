package io.factdriven.definition.impl

import io.factdriven.definition.api.Executing
import io.factdriven.definition.api.Promising
import kotlin.reflect.KClass

open class PromisingImpl(parent: Executing): Promising, ConsumingImpl(parent) {

    override var succeeding: KClass<*>? = null

}