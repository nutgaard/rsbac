package no.utgdev.rsbac

typealias Supplier<T> = () -> T
typealias Function<S, T> = (s: S) -> T
typealias Rule<CONTEXT> = CONTEXT.() -> DecisionEnums

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

    fun isApplicable() = this == PERMIT || this == DENY
}

data class Decision(val message: String, val decision: DecisionEnums) {
    fun withBias(bias: DecisionEnums) = Decision(this.message, this.decision.withBias(bias))
}

interface Combinable<CONTEXT> : Function<CONTEXT, DecisionEnums> {
    fun getMessage(context: CONTEXT): String
    override fun invoke(context: CONTEXT): DecisionEnums
    fun <DATA> asGenerator(): Generator<CONTEXT, DATA> = PolicyGenerator({ getMessage(context) }) { invoke(context) }
}

interface Generator<CONTEXT, DATA> {
    fun with(data: DATA): Combinable<CONTEXT>
}

class PolicySet<CONTEXT>(
        val combining: CombiningAlgo = CombiningAlgo.denyOverride,
        val policies: List<Combinable<CONTEXT>>
) : Combinable<CONTEXT> {
    private var result: Decision? = null

    override fun getMessage(context: CONTEXT): String {
        return result!!.message
    }

    override fun invoke(context: CONTEXT): DecisionEnums {
        result = this.combining.combine(this.policies, context)
        return result!!.decision
    }
}

class Policy<CONTEXT> : Combinable<CONTEXT> {
    private val message: CONTEXT.() -> String
    private val rule: Rule<CONTEXT>

    constructor(message: String, rule: Function<CONTEXT, Boolean>, effect: DecisionEnums) : this(
            { message },
            { if (rule.invoke(this)) effect else effect.negate() }
    )

    constructor(message: String, rule: Rule<CONTEXT>) : this(
            { message },
            rule
    )

    constructor(message: CONTEXT.() -> String, rule: Rule<CONTEXT>) {
        this.message = message
        this.rule = rule
    }

    override fun getMessage(context: CONTEXT): String {
        return this.message(context)
    }

    override fun invoke(context: CONTEXT): DecisionEnums {
        return this.rule.invoke(context)
    }
}

class RuleData<CONTEXT, DATA>(val context: CONTEXT, val data: DATA)
class PolicyGenerator<CONTEXT, DATA>(
        private val message: RuleData<CONTEXT, DATA>.() -> String,
        private val rule: Rule<RuleData<CONTEXT, DATA>>
) : Generator<CONTEXT, DATA> {
    override fun with(data: DATA): Policy<CONTEXT> = Policy({ message(RuleData(this, data)) }) {
        rule.invoke(RuleData(this, data))
    }

    constructor(message: String, rule: Rule<RuleData<CONTEXT, DATA>>) : this({ message }, rule)
}

class PolicySetGenerator<CONTEXT, DATA>(
        private val combining: CombiningAlgo = CombiningAlgo.denyOverride,
        private val policies: List<Generator<CONTEXT, DATA>>
) : Generator<CONTEXT, DATA> {

    override fun with(data: DATA): Combinable<CONTEXT> = PolicySet(
            this.combining,
            this.policies.map { it.with(data) }
    )
}
