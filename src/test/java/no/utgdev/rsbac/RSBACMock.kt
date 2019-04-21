package no.utgdev.rsbac

import java.util.function.Function
import java.util.function.Supplier

class RSBACMock : RSBAC<Any>, RSBACInstance<Any> {
    override fun permit(message: String, rule: Supplier<Boolean>): RSBACInstance<Any> = this
    override fun permit(message: String, rule: Function<Any, Boolean>): RSBACInstance<Any> = this
    override fun deny(message: String, rule: Supplier<Boolean>): RSBACInstance<Any> = this
    override fun deny(message: String, rule: Function<Any, Boolean>): RSBACInstance<Any> = this
    override fun check(policy: Policy<Any>): RSBACInstance<Any> = this
    override fun check(policyset: PolicySet<Any>): RSBACInstance<Any> = this
    override fun combining(combiningAlgo: CombiningAlgo): RSBACInstance<Any> = this
    override fun bias(bias: DecisionEnums): RSBACInstance<Any> = this
    override fun <S> get(result: Supplier<S>): S = result.get()
}