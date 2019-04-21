package no.utgdev.rsbac

import java.util.function.Function

interface Combinable<CONTEXT> : Function<CONTEXT, DecisionEnums> {
    fun getMessage(): String
}

abstract class CombiningAlgo {
    abstract fun <CONTEXT> combine(policies: List<Combinable<CONTEXT>>, context: CONTEXT): Decision

    companion object {
        @JvmField val denyOverride: CombiningAlgo = DenyOverride()
        @JvmField val firstApplicable: CombiningAlgo = FirstApplicable()

    }
}

// Inspirert av https://www.axiomatics.com/blog/understanding-xacml-combining-algorithms/
private class DenyOverride : CombiningAlgo() {
    override fun <CONTEXT> combine(policies: List<Combinable<CONTEXT>>, context: CONTEXT): Decision {
        var combinedDecision = Decision("No matching rule found", DecisionEnums.NOT_APPLICABLE)
        for (policy: Combinable<CONTEXT> in policies) {
            val ruleDecision = policy.apply(context)

            combinedDecision = when (combinedDecision.decision) {
                DecisionEnums.DENY -> combinedDecision
                else -> Decision(policy.getMessage(), ruleDecision)
            }
        }
        return combinedDecision
    }
}

private class FirstApplicable : CombiningAlgo() {
    override fun <CONTEXT> combine(policies: List<Combinable<CONTEXT>>, context: CONTEXT): Decision {
        var combinedDecision = Decision("No matching rule found", DecisionEnums.NOT_APPLICABLE)
        for (policy: Combinable<CONTEXT> in policies) {
            val ruleDecision = policy.apply(context)

            combinedDecision = when (combinedDecision.decision) {
                DecisionEnums.DENY, DecisionEnums.PERMIT -> combinedDecision
                DecisionEnums.NOT_APPLICABLE -> Decision(policy.getMessage(), ruleDecision)
            }
        }
        return combinedDecision
    }
}