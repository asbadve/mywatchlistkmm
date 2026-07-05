---
name: tmdb-api-guidelines
description: Guidelines and OpenAPI reference for calling TMDB APIs and resolving images
---

# TMDB API Integration Guidelines

Always reference the official TMDB OpenAPI specification when working with TMDB endpoints or building URL helpers.

- **TMDB OpenAPI Docs:** https://developer.themoviedb.org/openapi
- **Image Resolution Policy:** Never hardcode image sizes (like `/w185/` or `/w500/`). Always resolve them dynamically based on target rendering size (in density-independent pixels / DP) and device display scale/density to ensure images are crisp on retina/high-DPI screens while saving bandwidth on low-DPI devices.
