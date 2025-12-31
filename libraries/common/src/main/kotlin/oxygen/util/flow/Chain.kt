package oxygen.util.flow

interface ChainOperation<R, L, N> {
    val builder: R.(L) -> N
    fun build() = builder
}

fun <R, L, N> ChainOperation<R, L, N>.end(func: R.(N) -> Unit): ChainEnd<R, L> =
    ChainEnd { last -> func(builder(last)) }

fun <R, L, N, Ne> ChainOperation<R, L, N>.then(next: R.(N) -> Ne): ChainOperation<R, L, Ne> =
    ChainSimple { last -> next(builder(last)) }

@JvmInline
value class ChainSimple<R, L, N>(override val builder: R.(L) -> N) : ChainOperation<R, L, N>

@JvmInline
value class ChainEnd<R, L>(override val builder: R.(L) -> Unit) : ChainOperation<R, L, Unit>

fun <L, R> ChainOperation<R, L, Unit>.invoke(self: R, value: L) {
    self.builder(value)
}

fun <N, R> ChainOperation<R, Unit, N>.invoke(self: R): N = builder.invoke(self, Unit)
fun <L, N, R> ChainOperation<R, L, N>.invoke(self: R, value: L): N = builder.invoke(self, value)

fun <L, N, R> R.invokeChain(chain: ChainOperation<R, L, N>, value: L): N = chain.builder.invoke(this, value)
fun <N, R> R.invokeChain(chain: ChainOperation<R, Unit, N>): N = chain.builder.invoke(this, Unit)

@JvmName("invokeUnit")
fun <N> ChainOperation<Unit, Unit, N>.invoke(): N = Unit.builder(Unit)

@JvmName("invokeUnit")
fun <L, N> ChainOperation<Unit, L, N>.invoke(value: L): N = Unit.builder(value)

//
@JvmInline
value class ChainSeq<R, L, N>(override val builder: R.(L) -> Sequence<N>) : ChainOperation<R, L, Sequence<N>> {
    inline fun <reified T> filterIsInstance(): ChainSeq<R, L, T> =
        ChainSeq { last -> builder(last).filterIsInstance<T>() }
}

@JvmName("seqMap")
fun <L, N, R, Re> ChainOperation<Re, L, Sequence<N>>.map(transform: Re.(N) -> R): ChainSeq<Re, L, R> =
    ChainSeq { last -> builder(last).map { transform(it) } }

@JvmName("seqMapNotNull")
fun <L, N, R, Re> ChainOperation<Re, L, Sequence<N>>.mapNotNull(transform: Re.(N) -> R?): ChainSeq<Re, L, R> =
    ChainSeq { last -> builder(last).mapNotNull { transform(it) } }

@JvmName("seqFilter")
fun <L, N, R> ChainOperation<R, L, Sequence<N>>.filter(predicate: R.(N) -> Boolean): ChainSeq<R, L, N> =
    ChainSeq { last -> builder(last).filter { predicate(it) } }

@JvmName("seqFilterNotNull")
fun <L, N, R> ChainOperation<R, L, Sequence<N?>>.filterNotNull(): ChainSeq<R, L, N> =
    ChainSeq { last -> builder(last).filterNotNull() }

@JvmName("seqAssocitateBy")
fun <L, N, K, R> ChainOperation<R, L, Sequence<N>>.associateBy(keySelector: R.(N) -> K): ChainMap<R, L, K, N> =
    ChainMap { last -> builder(last).associateBy { keySelector(it) } }

@JvmName("seqAssociateWith")
fun <L, N, V, R> ChainOperation<R, L, Sequence<N>>.associateWith(valueSelector: R.(N) -> V): ChainMap<R, L, N, V> =
    ChainMap { last -> builder(last).associateWith { valueSelector(it) } }

@JvmName("seqAssociate")
fun <L, N, K, V, R> ChainOperation<R, L, Sequence<N>>.associate(pairSelector: R.(N) -> Pair<K, V>): ChainMap<R, L, K, V> =
    ChainMap { last -> builder(last).associate<N, K, V> { pairSelector(it) } }

@JvmName("seqToList")
fun <L, N, R> ChainOperation<R, L, Sequence<N>>.toList(): ChainList<R, L, N> =
    ChainList { last -> builder(last).toList() }

@JvmName("seqToSet")
fun <L, N, R> ChainOperation<R, L, Sequence<N>>.toSet(): ChainSet<R, L, N> =
    ChainSet { last -> builder(last).toSet() }

@JvmName("seqEach")
fun <L, N, R> ChainOperation<R, L, Sequence<N>>.each(func: R.(N) -> Unit): ChainEnd<R, L> =
    ChainEnd { last -> builder(last).forEach { func(it) } }

@JvmName("seqToMap")
fun <L, K, V, R> ChainOperation<R, L, Sequence<Pair<K, V>>>.toMap(): ChainMap<R, L, K, V> =
    ChainMap { last -> builder(last).toMap() }

@JvmName("SeqSelf")
fun <L, N, R> ChainOperation<R, L, Sequence<N>>.self(): ChainSeq<R, L, N> = this as ChainSeq<R, L, N>

inline fun <reified T, L, N, R> ChainOperation<R, L, Sequence<N>>.filterIsInstance(): ChainSeq<R, L, T> =
    ChainSeq { last -> builder(last).filterIsInstance<T>() }

@JvmInline
value class ChainList<R, L, N>(override val builder: R.(L) -> List<N>) : ChainOperation<R, L, List<N>>

@JvmName("listMap")
fun <L, N, R, Re> ChainOperation<Re, L, List<N>>.map(transform: Re.(N) -> R): ChainList<Re, L, R> =
    ChainList { last -> builder(last).map { transform(it) } }

@JvmName("listMapNotNull")
fun <L, N, R, Re> ChainOperation<Re, L, List<N>>.mapNotNull(transform: Re.(N) -> R?): ChainList<Re, L, R> =
    ChainList { last -> builder(last).mapNotNull { transform(it) } }

@JvmName("listFilter")
fun <L, N, R> ChainOperation<R, L, List<N>>.filter(predicate: R.(N) -> Boolean): ChainList<R, L, N> =
    ChainList { last -> builder(last).filter { predicate(it) } }

@JvmName("listFilterNotNull")
fun <L, N, R> ChainOperation<R, L, List<N?>>.filterNotNull(): ChainList<R, L, N> =
    ChainList { last -> builder(last).filterNotNull() }

@JvmName("listAssocitateBy")
fun <L, N, K, R> ChainOperation<R, L, List<N>>.associateBy(keySelector: R.(N) -> K): ChainMap<R, L, K, N> =
    ChainMap { last -> builder(last).associateBy { keySelector(it) } }

@JvmName("listAssociateWith")
fun <L, N, V, R> ChainOperation<R, L, List<N>>.associateWith(valueSelector: R.(N) -> V): ChainMap<R, L, N, V> =
    ChainMap { last -> builder(last).associateWith { valueSelector(it) } }

@JvmName("listAssociate")
fun <L, N, K, V, R> ChainOperation<R, L, List<N>>.associate(pairSelector: R.(N) -> Pair<K, V>): ChainMap<R, L, K, V> =
    ChainMap { last -> builder(last).associate<N, K, V> { pairSelector(it) } }

@JvmName("listAsSequence")
fun <R, L, N> ChainOperation<R, L, List<N>>.asSequence(): ChainSeq<R, L, N> =
    ChainSeq { last -> builder(last).asSequence() }

@JvmName("listToSet")
fun <R, L, N> ChainOperation<R, L, List<N>>.toSet(): ChainSet<R, L, N> =
    ChainSet { last -> builder(last).toSet() }

@JvmName("listEach")
fun <R, L, N> ChainOperation<R, L, List<N>>.each(func: R.(N) -> Unit): ChainEnd<R, L> =
    ChainEnd { last -> builder(last).forEach { func(it) } }

@JvmName("listToMap")
fun <L, K, V, R> ChainOperation<R, L, List<Pair<K, V>>>.toMap(): ChainMap<R, L, K, V> =
    ChainMap { last -> builder(last).toMap() }

//
@JvmInline
value class ChainSet<R, L, N>(override val builder: R.(L) -> Set<N>) : ChainOperation<R, L, Set<N>>

@JvmName("setAssociateBy")
fun <L, N, K, R> ChainOperation<R, L, Set<N>>.associateBy(keySelector: R.(N) -> K): ChainMap<R, L, K, N> =
    ChainMap { last -> builder(last).associateBy { keySelector(it) } }

@JvmName("setAssociateWith")
fun <L, N, V, R> ChainOperation<R, L, Set<N>>.associateWith(valueSelector: R.(N) -> V): ChainMap<R, L, N, V> =
    ChainMap { last -> builder(last).associateWith { valueSelector(it) } }

@JvmName("setAssociate")
fun <L, N, K, V, R> ChainOperation<R, L, Set<N>>.associate(pairSelector: R.(N) -> Pair<K, V>): ChainMap<R, L, K, V> =
    ChainMap { last -> builder(last).associate<N, K, V> { pairSelector(it) } }

@JvmName("setAsSequence")
fun <L, N, R> ChainOperation<R, L, Set<N>>.asSequence(): ChainSeq<R, L, N> =
    ChainSeq { last -> builder(last).asSequence() }

@JvmName("setToList")
fun <L, N, R> ChainOperation<R, L, Set<N>>.toList(): ChainList<R, L, N> =
    ChainList { last -> builder(last).toList() }

@JvmName("setEach")
fun <L, N, R> ChainOperation<R, L, Set<N>>.each(func: R.(N) -> Unit): ChainEnd<R, L> =
    ChainEnd { last -> builder(last).forEach { func(it) } }

@JvmName("setToMap")
fun <L, K, V, R> ChainOperation<R, L, Set<Pair<K, V>>>.toMap(): ChainMap<R, L, K, V> =
    ChainMap { last -> builder(last).toMap() }

@JvmInline
value class ChainMap<R, L, K, V>(override val builder: R.(L) -> Map<K, V>) : ChainOperation<R, L, Map<K, V>> {
    val keys: ChainSet<R, L, K>
        get() = ChainSet { last -> builder(last).keys }
    val values: ChainSeq<R, L, V>
        get() = ChainSeq { last -> builder(last).values.asSequence() }
}

@JvmName("mapKeys")
fun <L, K, V, R> ChainOperation<R, L, Map<K, V>>.getKeys(): ChainSet<R, L, K> =
    ChainSet { last -> builder(last).keys }

@JvmName("mapValues")
fun <L, K, V, R> ChainOperation<R, L, Map<K, V>>.getValues(): ChainSeq<R, L, V> =
    ChainSeq { last -> builder(last).values.asSequence() }

@JvmName("mapToList")
fun <L, K, V, R> ChainOperation<R, L, Map<K, V>>.toList(): ChainList<R, L, Pair<K, V>> =
    ChainList { last -> builder(last).toList() }

@JvmName("mapAsSequence")
fun <L, K, V, R> ChainOperation<R, L, Map<K, V>>.asSequence(): ChainSeq<R, L, Pair<K, V>> =
    ChainSeq { last -> builder(last).entries.asSequence().map { it.key to it.value } }

@JvmName("mapEach")
fun <L, K, V, R> ChainOperation<R, L, Map<K, V>>.each(func: R.(Pair<K, V>) -> Unit): ChainEnd<R, L> =
    ChainEnd { last -> builder(last).forEach { func(it.key to it.value) } }

fun <T> chainFromSeq(value: Sequence<T>): ChainSeq<Unit, Unit, T> = ChainSeq { _ -> value }
fun <T> chainFromList(value: List<T>): ChainList<Unit, Unit, T> = ChainList { _ -> value }
fun <T> chainFromSet(value: Set<T>): ChainSet<Unit, Unit, T> = ChainSet { _ -> value }
fun <K, V> chainFromMap(value: Map<K, V>): ChainMap<Unit, Unit, K, V> = ChainMap { _ -> value }

fun <S> chainStart(): ChainSimple<Unit, S, S> = ChainSimple { it }
