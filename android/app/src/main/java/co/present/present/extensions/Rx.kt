package co.present.present.extensions

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.*
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

fun <T> applyFlowableSchedulers(): FlowableTransformer<T, T> {
    return FlowableTransformer { upstream ->
        upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun <T> applySingleSchedulers(): SingleTransformer<T, T> {
    return SingleTransformer { upstream ->
        upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun <T> applyMaybeSchedulers(): MaybeTransformer<T, T> {
    return MaybeTransformer { upstream ->
        upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun applyCompletableSchedulers(): CompletableTransformer {
    return CompletableTransformer { upstream ->
        upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun <T> PublishSubject<T>.toFlowable(): Flowable<T> = this.toFlowable(BackpressureStrategy.LATEST)

data class Quad<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D) : Any() {
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

data class Quint<out A, out B, out C, out D, out E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E) : Any() {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}

data class Sextuple<out A, out B, out C, out D, out E, out F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F) : Any() {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth)"
}

data class Septuple<out A, out B, out C, out D, out E, out F, out G>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F, val seventh: G) : Any() {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh)"
}

data class Octuple<out A, out B, out C, out D, out E, out F, out G, out H>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F, val seventh: G, val eighth: H) : Any() {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth)"
}

// This is probably getting to be a bad idea, but what a cool name
data class Nonuple<out A, out B, out C, out D, out E, out F, out G, out H, out I>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F, val seventh: G, val eighth: H, val ninth: I) : Any() {
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth, $seventh, $eighth, $ninth)"
}

fun <A : Any, B : Any, C : Any> Single<A>.zipWith(single1: Single<B>, single2: Single<C>)
        : Single<Triple<A, B, C>> {
    return Single.zip(this, single1, single2,
            Function3<A, B, C, Triple<A, B, C>> { a, b, c ->
                Triple(a, b, c)
            })
}

/**
 * Combine latest operator that produces [Quad]
 */
fun <A : Any, B : Any, C : Any, D : Any> Flowable<A>.combineLatest(flowable1: Flowable<B>, flowable2: Flowable<C>, flowable3: Flowable<D>)
        : Flowable<Quad<A, B, C, D>> {
    return Flowable.combineLatest(this, flowable1, flowable2, flowable3,
            Function4<A, B, C, D, Quad<A, B, C, D>> { a, b, c, d ->
        Quad(a, b, c, d)
    })
}

/**
 * Combine latest operator that produces [Quint]
 */
fun <A : Any, B : Any, C : Any, D : Any, E: Any> Flowable<A>.combineLatest(flowable1: Flowable<B>,
                                                                           flowable2: Flowable<C>,
                                                                           flowable3: Flowable<D>,
                                                                           flowable4: Flowable<E>)
        : Flowable<Quint<A, B, C, D, E>> {
    return Flowable.combineLatest(this, flowable1, flowable2, flowable3, flowable4,
            Function5<A, B, C, D, E, Quint<A, B, C, D, E>> { a, b, c, d, e ->
                Quint(a, b, c, d, e)
            })
}

/**
 * Combine latest operator that produces [Sextuple]
 */
fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any> Flowable<A>
        .combineLatest(flowable1: Flowable<B>,
                       flowable2: Flowable<C>,
                       flowable3: Flowable<D>,
                       flowable4: Flowable<E>,
                       flowable5: Flowable<F>)
        : Flowable<Sextuple<A, B, C, D, E, F>> {
    return Flowable.combineLatest(this, flowable1, flowable2, flowable3, flowable4, flowable5, Function6<A, B, C, D, E, F, Sextuple<A, B, C, D, E, F>> { a, b, c, d, e, f ->
        Sextuple(a, b, c, d, e, f)
    })
}

/**
 * Combine latest operator that produces [Sept]
 */
fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any> Flowable<A>
        .combineLatest(flowable1: Flowable<B>,
                       flowable2: Flowable<C>,
                       flowable3: Flowable<D>,
                       flowable4: Flowable<E>,
                       flowable5: Flowable<F>,
                       flowable6: Flowable<G>)
        : Flowable<Septuple<A, B, C, D, E, F, G>> {
    return Flowable.combineLatest(this, flowable1, flowable2, flowable3, flowable4, flowable5, flowable6, Function7<A, B, C, D, E, F, G, Septuple<A, B, C, D, E, F, G>> { a, b, c, d, e, f, g ->
        Septuple(a, b, c, d, e, f, g)
    })
}

/**
 * Combine latest operator that produces [Nonuple]
 */
fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H: Any> Flowable<A>
        .combineLatest(flowable1: Flowable<B>,
                       flowable2: Flowable<C>,
                       flowable3: Flowable<D>,
                       flowable4: Flowable<E>,
                       flowable5: Flowable<F>,
                       flowable6: Flowable<G>,
                       flowable7: Flowable<H>)
        : Flowable<Octuple<A, B, C, D, E, F, G, H>> {
    return Flowable.combineLatest(this, flowable1, flowable2, flowable3, flowable4, flowable5, flowable6, flowable7, Function8<A, B, C, D, E, F, G, H, Octuple<A, B, C, D, E, F, G, H>> { a, b, c, d, e, f, g, h ->
        Octuple(a, b, c, d, e, f, g, h)
    })
}

/**
 * Combine latest operator that produces [Nonuple]
 */
fun <A : Any, B : Any, C : Any, D : Any, E : Any, F : Any, G : Any, H: Any, I: Any> Flowable<A>
        .combineLatest(flowable1: Flowable<B>,
                       flowable2: Flowable<C>,
                       flowable3: Flowable<D>,
                       flowable4: Flowable<E>,
                       flowable5: Flowable<F>,
                       flowable6: Flowable<G>,
                       flowable7: Flowable<H>,
                       flowable8: Flowable<I>)
        : Flowable<Nonuple<A, B, C, D, E, F, G, H, I>> {
    return Flowable.combineLatest(this, flowable1, flowable2, flowable3, flowable4, flowable5, flowable6, flowable7, flowable8, Function9<A, B, C, D, E, F, G, H, I, Nonuple<A, B, C, D, E, F, G, H, I>> { a, b, c, d, e, f, g, h, i ->
        Nonuple(a, b, c, d, e, f, g, h, i)
    })
}
