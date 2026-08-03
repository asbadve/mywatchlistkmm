package com.ajinkyabadve.kmmmywatchlist.core.image

import coil3.PlatformContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Lives in desktopTest, not commonTest: it asserts against the desktop `imageCacheDir` actual and
// uses JVM-only PlatformContext.INSTANCE / System.getProperty, neither of which exists in common.
class ImageLoaderFactoryTest {
    @Test
    fun testImageCacheDir_isPersistentUnderUserHome_notTempDir() {
        val dir = imageCacheDir(PlatformContext.INSTANCE)

        assertNotNull(dir, "desktop should have a filesystem-backed disk cache directory")
        assertTrue(dir.name == "image_cache", "expected the cache dir to be named image_cache, was $dir")
        // The whole point of the actual: a persistent per-user location, not the volatile temp dir.
        val userHome = System.getProperty("user.home")
        assertTrue(dir.toString().startsWith(userHome), "cache dir should live under user home ($userHome), was $dir")
        val tempDir = System.getProperty("java.io.tmpdir")
        assertTrue(!dir.toString().startsWith(tempDir), "cache dir must not be under the system temp dir ($tempDir), was $dir")
    }

    @Test
    fun testNewImageLoader_configuresBothMemoryAndDiskCache() {
        val loader = newImageLoader(PlatformContext.INSTANCE)

        assertNotNull(loader.memoryCache, "an in-memory cache should be configured")
        val diskCache = loader.diskCache
        assertNotNull(diskCache, "a disk cache should be configured on desktop")
        assertEquals(256L * 1024 * 1024, diskCache.maxSize, "disk cache budget should be 256 MB")
    }
}
