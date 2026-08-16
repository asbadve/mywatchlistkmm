package com.ajinkyabadve.kmmmywatchlist.network.constant

object NetworkConstant {
    // TMDB's alias for the same CloudFront distribution as api.themoviedb.org - identical paths,
    // auth and payloads. Deliberately NOT api.themoviedb.org: some ISPs (observed on Reliance Jio,
    // 2026-08-06) DNS-hijack the whole themoviedb.org zone and answer with a blackhole address of
    // their own, so every API call fails to connect while image.tmdb.org keeps working - the app
    // renders its layouts with every card empty. The tmdb.org zone is not on those blocklists.
    const val HOST = "api.tmdb.org"
    const val API_KEY = "api_key"
    const val PAGE = "page"
    const val SESSION_ID = "session_id"
}
