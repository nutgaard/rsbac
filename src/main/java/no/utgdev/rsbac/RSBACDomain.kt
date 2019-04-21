package no.utgdev.rsbac

import java.util.function.Function
import java.util.function.Supplier

class RSBACException(override val message: String) : RuntimeException(message)

enum class DecisionEnums {
    PERMIT, DENY, NOT_APPLICABLE;

    fun negate(): DecisionEnums {
        return when (this) {
            PERMIT -> DENY
            DENY -> PERMIT
            NOT_APPLICABLE -> NOT_APPLICABLE
        }
    }

    fun withBias(bias: DecisionEnums): DecisionEnums {
        return when (this) {
            NOT_APPLICABLE -> bias
            else -> this
        }
    }
}

typealias Rule<CONTEXT> = java.util.function.Function<CONTEXT, DecisionEnums>

data class Decision(val message: String, val decision: DecisionEnums) {
    fun withBias(bias: DecisionEnums) = Decision(this.message, this.decision.withBias(bias))
}

class PolicySet<CONTEXT>(
        val combining: CombiningAlgo = CombiningAlgo.denyOverride,
        val policies: List<Combinable<CONTEXT>>
) : Combinable<CONTEXT> {
    private var result: Decision? = null

    override fun getMessage(): String {
        return result!!.message
    }

    override fun apply(context: CONTEXT): DecisionEnums {
        result = this.combining.combine(this.policies, context)
        return result!!.decision
    }
}

class Policy<CONTEXT> : Combinable<CONTEXT> {
    private val message: String
    private val rule: Rule<CONTEXT>

    constructor(message: String, rule: Rule<CONTEXT>) {
        this.message = message
        this.rule = rule
    }

    constructor(message: String, rule: Supplier<Boolean>, effect: DecisionEnums) {
        this.message = message
        this.rule = Function { if (rule.get()) effect else effect.negate() }
    }

    constructor(message: String, rule: java.util.function.Function<CONTEXT, Boolean>, effect: DecisionEnums) {
        this.message = message
        this.rule = Function { if (rule.apply(it)) effect else effect.negate() }
    }

    override fun getMessage(): String {
        return this.message
    }

    override fun apply(context: CONTEXT): DecisionEnums {
        return this.rule.apply(context)
    }

}