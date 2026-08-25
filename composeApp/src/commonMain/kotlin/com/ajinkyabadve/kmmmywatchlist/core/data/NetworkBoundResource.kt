package com.ajinkyabadve.kmmmywatchlist.core.data

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network

/**
 * Kotlin/[Flow] port of Google's `NetworkBoundResource` abstract class (the "Guide to app
 * architecture" sample / Android Sunflower) - every detail screen (Movie/Tv, and any future one)
 * repeats this exact shape by hand: peek the local cache, decide whether to fetch, fetch and save
 * on success, fall back to the cache on failure. A concrete instance implements the four template
 * methods; [asFlow] does the orchestration once, for all of them.
 *
 * [fetchFromNetwork]/[saveCallResult] are deliberately two separate methods, not one fused
 * "refresh" call - that split is the actual point of this pattern (it lets [asFlow] emit
 * `Resource.Loading(data)` between them, and gives [saveCallResult] a plain value to persist
 * rather than a suspend call it has to also perform the fetch inside of).
 */
abstract class NetworkBoundResource<ResultType, RequestType> {
    fun asFlow(): Flow<Resource<ResultType>> =
        flow {
            emit(Resource.Loading())
            val data = loadFromDb().firstOrNull()

            if (shouldFetch(data)) {
                emit(Resource.Loading(data))
                try {
                    saveCallResult(fetchFromNetwork())
                    emitAll(loadFromDb().mapNotNull { it?.let { d -> Resource.Success(d) } })
                } catch (e: HttpExceptions) {
                    onFetchFailed(e)
                    emit(Resource.Error(e, UiText.Plain(e.message), data))
                } catch (e: IOException) {
                    onFetchFailed(e)
                    emit(Resource.Error(e, UiText.Resource(Res.string.error_network), data))
                } catch (e: ContentConvertException) {
                    onFetchFailed(e)
                    emit(Resource.Error(e, malformedResponseMessage(), data))
                } catch (e: SerializationException) {
                    onFetchFailed(e)
                    emit(Resource.Error(e, malformedResponseMessage(), data))
                }
            } else {
                emitAll(loadFromDb().mapNotNull { it?.let { d -> Resource.Success(d) } })
            }
        }

    /** Called when [fetchFromNetwork]/[saveCallResult] throws - override to log with a specific
     *  tag/context; does nothing by default. */
    protected open fun onFetchFailed(throwable: Throwable) = Unit

    /** The message a [ContentConvertException]/[SerializationException] maps to - override per
     *  screen; falls back to the same network-error copy [IOException] uses. */
    protected open fun malformedResponseMessage(): UiText = UiText.Resource(Res.string.error_network)

    /** Whether to fetch at all, given whatever is currently cached. Every instance in this
     *  codebase returns `true` today (no staleness/TTL policy exists here yet) - kept abstract
     *  rather than defaulted so a future caller that wants one has an obvious seam. */
    protected abstract fun shouldFetch(data: ResultType?): Boolean

    /** Local-only, no network - the repository's own `observe`-style Flow. */
    protected abstract fun loadFromDb(): Flow<ResultType?>

    /** Just the API call - no cache write. */
    protected abstract suspend fun fetchFromNetwork(): RequestType

    /** Just the cache write - no network call. */
    protected abstract suspend fun saveCallResult(item: RequestType)
}
