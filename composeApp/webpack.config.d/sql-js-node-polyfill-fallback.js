// sql.js's UMD wrapper (node_modules/sql.js/dist/sql-wasm.js) feature-detects Node.js via
// `require("path")`/`require("fs")`/`require("crypto")` at module load time, even though the
// WebWorkerDriver only ever runs it inside a browser Worker. Webpack 5 dropped automatic Node
// core-module polyfills, so without this it fails to bundle with "Module not found". Those three
// require calls are inside an `if (typeof require === "function")`-style guard that's false in a
// browser, so they're genuinely never needed at runtime - `false` stubs them out.
//
// `os` is different: okio's FileSystem companion (pulled in via Coil) calls os.tmpdir() at
// module-init time - a real code path that always runs - so `os: false` throws
// "tmpdir is not a function" at startup instead of a bundling error. It needs an actual
// polyfill (os-browserify, declared as a devNpm in build.gradle.kts) instead.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    path: false,
    fs: false,
    crypto: false,
    os: require.resolve("os-browserify/browser"),
});
