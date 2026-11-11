package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Remembers the app's [TopLevelBackStack] across Activity recreation (theme, locale or font-scale
 * changes) and process death, starting on [startKey] the first time.
 *
 * Compose's own [rememberSerializable] does the saving, through [TopLevelBackStackSerializer]. A
 * plain `remember` lost every open screen whenever the Activity was recreated - switching the
 * device theme with the Account dialog open landed back on Trending.
 */
@Composable
fun rememberTopLevelBackStack(startKey: AppKey): TopLevelBackStack =
    rememberSerializable(serializer = TopLevelBackStackSerializer) { TopLevelBackStack(startKey) }

/**
 * Owns one child back stack per bottom-nav tab (so switching tabs preserves each tab's own
 * navigation depth, matching the old Navigation 2 popUpTo/saveState/restoreState behavior) and
 * flattens them into the single list `NavDisplay` needs. Modeled on Android's own Navigation 3
 * "top level back stack" recipe for bottom navigation.
 *
 * Saved and restored with [rememberTopLevelBackStack]. Navigation 3's own saveable stack,
 * `rememberNavBackStack(configuration, ...)` - one per tab, as in the official "multiple back
 * stacks" recipe - was the platform option, and it does not fit: it saves one flat list, while this
 * class's state is also *which* tabs have a stack and the *order* they were visited in. Back from a
 * tab's root returns to the previously visited tab, not to a fixed start tab, and a tab the user
 * backed out of is dropped and starts fresh next time. [snapshot] captures all of that as the tab
 * stacks in visit order, and [TopLevelBackStackSerializer] saves that one list - still through the
 * same Compose saving mechanism `rememberNavBackStack` uses underneath.
 */
class TopLevelBackStack private constructor(
    tabStacks: List<List<AppKey>>,
) {
    constructor(startKey: AppKey) : this(listOf(listOf(startKey)))

    private val topLevelStack = ArrayDeque<AppKey>()
    private val topLevelChildStacks = mutableMapOf<AppKey, NavBackStack<AppKey>>()

    var topLevelKey by mutableStateOf(tabStacks.last().first())
        private set

    val backStack = NavBackStack<AppKey>()

    init {
        require(tabStacks.isNotEmpty() && tabStacks.none { it.isEmpty() }) { EMPTY_SNAPSHOT_MESSAGE }
        tabStacks.forEach { stack ->
            topLevelStack.addLast(stack.first())
            topLevelChildStacks[stack.first()] = NavBackStack<AppKey>().apply { addAll(stack) }
        }
        updateBackStack()
    }

    private fun updateBackStack() {
        backStack.clear()
        backStack.addAll(topLevelStack.flatMap { topLevelChildStacks.getValue(it) })
    }

    fun switchTopLevel(key: AppKey) {
        dismissTransientTop()
        if (topLevelChildStacks[key] == null) {
            topLevelChildStacks[key] = NavBackStack(key)
        } else {
            topLevelStack.remove(key)
        }
        topLevelStack.addLast(key)
        topLevelKey = key
        updateBackStack()
    }

    /**
     * Drops [SearchKey] or [AccountKey] off the tab being left, when it's what the user is
     * currently looking at.
     *
     * Every other pushed screen belongs to the tab it was opened from - a trending movie's detail
     * really is somewhere inside Trending, so restoring it when the user backs out of another tab
     * is right. Search and Account aren't: both are launched from the top bar that every tab
     * shares, so nesting either under whichever tab happened to be showing is an implementation
     * detail. Leaving it in the stack meant `Search -> Movies tab -> back` surfaced Search again,
     * which reads as a glitch.
     *
     * Only the *top* of the stack is dropped. If the user drilled from search into a result
     * (Search -> MovieDetail), the search entry stays put so that backing out of that detail still
     * returns to the results the user came from.
     */
    private fun dismissTransientTop() {
        val childStack = topLevelChildStacks[topLevelKey] ?: return
        val top = childStack.lastOrNull()
        if (childStack.size > 1 && (top == SearchKey || top == AccountKey)) {
            childStack.removeAt(childStack.lastIndex)
        }
    }

    fun add(key: AppKey) {
        topLevelChildStacks.getValue(topLevelKey).add(key)
        updateBackStack()
    }

    fun removeLast() {
        if (backStack.size <= 1) return
        val childStack = topLevelChildStacks.getValue(topLevelKey)
        if (childStack.size > 1) {
            childStack.removeAt(childStack.lastIndex)
        } else {
            topLevelStack.removeLast()
            topLevelChildStacks.remove(topLevelKey)
            topLevelKey = topLevelStack.last()
        }
        updateBackStack()
    }

    /**
     * The whole navigation state as plain lists: one per open tab, in the order the tabs were
     * visited, each starting with its tab key. The current tab is the last one.
     */
    internal fun snapshot(): List<List<AppKey>> = topLevelStack.map { topLevelChildStacks.getValue(it).toList() }

    internal companion object {
        private const val EMPTY_SNAPSHOT_MESSAGE = "A TopLevelBackStack needs at least one non-empty tab stack"

        /** Rebuilds a back stack from [snapshot]'s output. */
        fun fromSnapshot(tabStacks: List<List<AppKey>>): TopLevelBackStack = TopLevelBackStack(tabStacks)
    }
}

/**
 * Saves a [TopLevelBackStack] as its [TopLevelBackStack.snapshot]. [AppKey] is sealed, so its
 * serializer is generated at compile time and this works on every target without reflection.
 */
internal object TopLevelBackStackSerializer : KSerializer<TopLevelBackStack> {
    private const val SERIAL_NAME = "com.ajinkyabadve.kmmmywatchlist.navigation.TopLevelBackStack"

    private val snapshotSerializer = ListSerializer(ListSerializer(AppKey.serializer()))

    // Wraps the list's descriptor under this type's own name - the documented way to delegate a
    // custom serializer to another one.
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor(SERIAL_NAME, snapshotSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: TopLevelBackStack,
    ) = encoder.encodeSerializableValue(snapshotSerializer, value.snapshot())

    override fun deserialize(decoder: Decoder): TopLevelBackStack =
        TopLevelBackStack.fromSnapshot(decoder.decodeSerializableValue(snapshotSerializer))
}
