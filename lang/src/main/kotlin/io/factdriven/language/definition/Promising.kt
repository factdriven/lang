package io.factdriven.language.definition

import kotlin.reflect.KClass

interface Promising: Awaiting, Succeeding, Failing
