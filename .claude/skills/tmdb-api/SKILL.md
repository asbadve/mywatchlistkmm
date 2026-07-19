---
name: tmdb-api
description: How to look up TMDB API v3 endpoint schemas (OpenAPI definitions) when adding or extending API models in this app - spec URLs, append_to_response rules, and how to ground-truth against the live API.
---

# TMDB API reference for this project

Use this whenever adding fields to API models (`features/*/model/*.kt`) or new endpoints in
repositories. Always check the OpenAPI definition AND a live response - the published examples
can lag behind real payloads (e.g. `episode_type` is returned live but missing from the docs
example).

## Official OpenAPI definitions (per endpoint)

There is no single public `openapi.json` download (the old
`developer.themoviedb.org/openapi/<id>` URLs 404). Instead, every reference page has a
Markdown twin containing the full OpenAPI 3.1 definition for that endpoint, including the
complete response schema:

```
https://developer.themoviedb.org/reference/<slug>.md
```

Examples: `tv-episode-details.md`, `tv-season-details.md`, `tv-series-details.md`,
`movie-details.md`, `tv-episode-credits.md`. The machine-readable index of all pages is
`https://developer.themoviedb.org/llms.txt`. Fetch with plain `curl` - no auth needed.

## Ground-truthing against the live API

The API key is the Gradle property `MY_WATCH_LIST_TMDB_API_KEY` (in `~/.gradle/gradle.properties`),
exposed to code as `BuildConfig.TMDB_API_KEY`.

```bash
KEY=$(grep MY_WATCH_LIST_TMDB_API_KEY ~/.gradle/gradle.properties | cut -d= -f2 | tr -d ' ')
curl -s "https://api.themoviedb.org/3/tv/1399/season/1/episode/1?api_key=$KEY&append_to_response=credits,external_ids,images,translations,videos" | jq 'keys'
```

Good stable test id: series 1399 (Game of Thrones).

## append_to_response support per namespace (verified)

- **Movie / TV series** (`/3/movie/{id}`, `/3/tv/{id}`): wide support - credits, images, videos,
  external_ids, keywords, recommendations, similar, content_ratings, translations, ...
- **TV season** (`/3/tv/{id}/season/{n}`): credits, images, external_ids, videos, translations,
  account_states, aggregate_credits, changes. Unsupported values are silently ignored (no error).
- **TV episode** (`/3/tv/{id}/season/{n}/episode/{n}`): credits, external_ids, images,
  translations, videos (+ account_states with a user session). The base episode payload ALSO
  contains top-level `crew` and `guest_stars` buckets duplicating the appended credits content.

## Project conventions

- Models are `@Serializable` data classes with `@SerialName` for snake_case and defaults on every
  field (client uses `ignoreUnknownKeys = true` - see `network/client/TmdbClient.kt`).
- Append lists live as constants in the repository impls (e.g.
  `TvRepositoryImpl.EPISODE_APPEND_TO_RESPONSE`).
- When adding fields, add a deserialization test with a realistic payload
  (see `EpisodeDetailSerializationTest`).
