---
name: tmdb-api-guidelines
description: Guidelines and OpenAPI reference for calling TMDB APIs and resolving images
---

# TMDB API Integration Guidelines

Always reference the official TMDB OpenAPI specification when working with TMDB endpoints or building URL helpers.

- **TMDB OpenAPI Docs:** https://developer.themoviedb.org/openapi
- **Image Resolution Policy:** Never hardcode image sizes (like `/w185/` or `/w500/`). Always resolve them dynamically based on target rendering size (in density-independent pixels / DP) and device display scale/density to ensure images are crisp on retina/high-DPI screens while saving bandwidth on low-DPI devices.

---

## Basic Project Integration Info

- **API host**: `api.themoviedb.org` (see `NetworkConstant.HOST`), always HTTPS.
- **HTTP client**: single Ktor `HttpClient` singleton — `TmdbClient.TmdbApiClient.newInstance` (`network/client/TmdbClient.kt`). Configured with `HttpRequestRetry` (3 retries incl. server errors/timeouts), 30s timeouts (`HttpTimeout`), lenient/ignore-unknown-keys JSON via `ContentNegotiation`, full request/response logging through Napier, and an `HttpResponseValidator` that maps non-2xx responses to a custom `HttpExceptions` with a human-readable failure reason (401 Unauthorized, 403 missing API key, 404 invalid request, etc.).
- **Auth**: API key auth (not bearer token) — appended as the `api_key` query param (`NetworkConstant.API_KEY`) on every request, sourced from `BuildConfig.TMDB_API_KEY`, which is injected at build time from the `MY_WATCH_LIST_TMDB_API_KEY` Gradle property (see root `README.md` for setup). Never hardcode the key or commit it.
- **URL builders** live in `network/builder/MovieNetworkUtil.kt`:
  - `mediaHttpBuilder(path, pageNumber, mediaType)` — for paginated media list endpoints (`now_playing`, `popular`, `top_rated`, `upcoming`, etc. under movie/tv).
  - `trendingMediaHttpBuilder(timeWindow, mediaType, path)` — for `/trending/{media_type}/{time_window}`.
  - Both always set `protocol = HTTPS`, `host = api.themoviedb.org`, `trailingQuery = true`, and append the `api_key` and (where relevant) `page` params.
- **Image resolution** is centralized in `core/ImageConfigResolver.kt` — do not build image URLs manually elsewhere:
  - Fetches live TMDB image configuration (base URL + available sizes per bucket) via `ConfigurationRepository`, cached with a timestamp (`ConfigurationConstants.KEY_CONFIG` / `KEY_TIMESTAMP`, refreshed daily via `DAY_IN_MILLIS`), falling back to `ConfigurationConstants.defaultImagesConfig` if not yet loaded.
  - `resolve(path, type, targetWidthDp, density)` picks the smallest configured size ≥ `targetWidthDp * density` for the given `ImageType` (`POSTER`, `BACKDROP`, `PROFILE`, `STILL`) and returns the full `secure_base_url + size + path` URL.
  - Default fallback sizes: posters `w92..w780,original`; backdrops `w300,w780,w1280,original`; profiles `w45,w185,h632,original`; stills `w92,w185,w300,original`.
- **Feature/network split**: TMDB-calling code lives per feature under `features/{movies,trending,tvshows,person}/network` (and corresponding `repository`), all going through the shared `TmdbClient` and the builders above rather than instantiating their own clients.
