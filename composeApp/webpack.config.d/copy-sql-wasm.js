// sqljs.worker.js (from @cashapp/sqldelight-sqljs-worker) hardcodes
// `locateFile: file => '/sql-wasm.wasm'` - an absolute root-relative URL - but nothing ever copies
// the real binary (node_modules/sql.js/dist/sql-wasm.wasm) there. Without this, the request 404s
// and, in dev, webpack-dev-server's SPA fallback serves index.html instead of a real 404;
// `WebAssembly.instantiate()` then fails with "expected magic word 00 61 73 6d, found 3c 21 44 4f"
// (the literal bytes of "<!DO..."), which crashes the JS SqlDriver's worker the first time any
// screen actually touches the local database - list screens with no local caching never hit this,
// only detail screens (MovieDetailCacheRepository/NetworkBoundResource) do, on their first query.
//
// `copy-webpack-plugin` (declared as a jsMain devNpm in build.gradle.kts specifically for this)
// was already the intended fix - it just never got wired into an actual webpack config until now.
const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

config.plugins = config.plugins || [];
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: path.join(path.dirname(require.resolve('sql.js/package.json')), 'dist', 'sql-wasm.wasm'),
                to: 'sql-wasm.wasm',
            },
        ],
    }),
);
