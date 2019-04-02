package no.utgdev.rsbac

import java.util.function.Function
import java.util.function.Supplier

class RSBACException(override val message: String) : RuntimeException(message)

interface CombiningAlgo {
    fun <CONTEXT> combine(policies: List<Policy<CONTEXT>>, context: CONTEXT): Decision
}

class DenyOverrideCombiningAlgo : CombiningAlgo {
    override  fun <CONTEXT> combine(policies: List<Policy<CONTEXT>>, context: CONTEXT): Decision {
        for (policy: Policy<CONTEXT> in policies) {
            val decision = if (policy.rule.apply(context)) policy.decisionEnums else policy.decisionEnums.negate()
            if (decision == DecisionEnums.DENY) {
                return Decision(policy.message, DecisionEnums.DENY)
            }
        }
        return Decision("", DecisionEnums.PERMIT)
    }
}

enum class DecisionEnums {
    PERMIT, DENY;

    fun negate(): DecisionEnums {
        return if (this == DENY) PERMIT else DENY
    }
}

typealias Rule<CONTEXT> = Function<CONTEXT, Boolean>
data class Decision(val message: String, val decision: DecisionEnums)
data class Policy<CONTEXT>(
        val message: String,
        val rule: Rule<CONTEXT>,
        val decisionEnums: DecisionEnums
)

class RSBAC<CONTEXT>(private val context: CONTEXT) {
    fun permit(message: String, rule: Supplier<Boolean>) =
            RSBACInstance<CONTEXT, Void>(context).permit(message, rule)

    fun permit(message: String, rule: Function<CONTEXT, Boolean>) =
            RSBACInstance<CONTEXT, Void>(context).permit(message, rule)

    fun deny(message: String, rule: Supplier<Boolean>) =
            RSBACInstance<CONTEXT, Void>(context).deny(message, rule)

    fun deny(message: String, rule: Function<CONTEXT, Boolean>) =
            RSBACInstance<CONTEXT, Void>(context).deny(message, rule)

}

class RSBACInstance<CONTEXT, OUTPUT>(val context: CONTEXT) {
    private var combiningAlgo: CombiningAlgo = DenyOverrideCombiningAlgo()
    private var policies: List<Policy<CONTEXT>> = emptyList()

    fun permit(message: String, rule: Supplier<Boolean>) = permit(message, Function { rule.get() })
    fun permit(message: String, rule: Function<CONTEXT, Boolean>): RSBACInstance<CONTEXT, OUTPUT> {
        this.policies = this.policies.plusElement(Policy(message, rule, DecisionEnums.PERMIT))
        return this
    }


    fun deny(message: String, rule: Supplier<Boolean>) = deny(message, Function { rule.get() })
    fun deny(message: String, rule: Function<CONTEXT, Boolean>): RSBACInstance<CONTEXT, OUTPUT> {
        this.policies = this.policies.plusElement(Policy(message, rule, DecisionEnums.DENY))
        return this
    }

    fun <S> get(result: Supplier<S>): S {
        val decision: Decision = combiningAlgo.combine(this.policies, context)

        if (decision.decision == DecisionEnums.DENY) {
            throw RSBACException(decision.message)
        }

        return result.get()
    }
}
