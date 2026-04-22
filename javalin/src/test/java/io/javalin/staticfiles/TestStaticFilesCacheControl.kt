package io.javalin.staticfiles

import io.javalin.config.JavalinConfig
import io.javalin.http.Header
import io.javalin.http.HttpStatus.OK
import io.javalin.http.staticfiles.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestStaticFilesCacheControl {

    private val defaultStaticResourceApp = { cfg: JavalinConfig -> cfg.staticFiles.add("/public", Location.CLASSPATH) }

    @Test
    fun `default cache control behavior should remain unchanged`() = testStaticFiles(defaultStaticResourceApp) { _, http ->
        val response = http.get("/script.js")
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=0")
    }

    @Test
    fun `can disable cache control header for a single directory`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.enableCacheControl = false
        }
    }) { _, http ->
        val response = http.get("/script.js")
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isNullOrEmpty()
    }

    @Test
    fun `can set custom cache control value using cacheControl property`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.cacheControl = "max-age=31536000, immutable"
        }
    }) { _, http ->
        val response = http.get("/script.js")
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31536000, immutable")
    }

    @Test
    fun `cacheControl property takes precedence over headers map`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.headers = mapOf(Header.CACHE_CONTROL to "max-age=0")
            it.cacheControl = "max-age=31536000"
        }
    }) { _, http ->
        val response = http.get("/script.js")
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31536000")
    }

    @Test
    fun `multiple directories can have independent cache control settings - one enabled, one disabled`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.hostedPath = "/cached"
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.enableCacheControl = true
        }
        cfg.staticFiles.add {
            it.hostedPath = "/no-cache"
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.enableCacheControl = false
        }
    }) { _, http ->
        val cachedResponse = http.get("/cached/script.js")
        assertThat(cachedResponse.status).isEqualTo(OK.code)
        assertThat(cachedResponse.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=0")

        val noCacheResponse = http.get("/no-cache/script.js")
        assertThat(noCacheResponse.status).isEqualTo(OK.code)
        assertThat(noCacheResponse.headers.getFirst(Header.CACHE_CONTROL)).isNullOrEmpty()
    }

    @Test
    fun `multiple directories can have independent cache control settings - different cache values`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.hostedPath = "/short-cache"
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.cacheControl = "max-age=60"
        }
        cfg.staticFiles.add {
            it.hostedPath = "/long-cache"
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.cacheControl = "max-age=31536000, immutable"
        }
        cfg.staticFiles.add {
            it.hostedPath = "/no-cache"
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.enableCacheControl = false
        }
    }) { _, http ->
        val shortCacheResponse = http.get("/short-cache/script.js")
        assertThat(shortCacheResponse.status).isEqualTo(OK.code)
        assertThat(shortCacheResponse.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=60")

        val longCacheResponse = http.get("/long-cache/script.js")
        assertThat(longCacheResponse.status).isEqualTo(OK.code)
        assertThat(longCacheResponse.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31536000, immutable")

        val noCacheResponse = http.get("/no-cache/script.js")
        assertThat(noCacheResponse.status).isEqualTo(OK.code)
        assertThat(noCacheResponse.headers.getFirst(Header.CACHE_CONTROL)).isNullOrEmpty()
    }

    @Test
    fun `other headers should still be applied when cache control is disabled`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.enableCacheControl = false
            it.headers = mapOf(
                "X-Custom-Header" to "custom-value",
                Header.CACHE_CONTROL to "max-age=0"
            )
        }
    }) { _, http ->
        val response = http.get("/script.js")
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isNullOrEmpty()
        assertThat(response.headers.getFirst("X-Custom-Header")).isEqualTo("custom-value")
    }

    @Test
    fun `webjars should still have long cache control by default`() = testStaticFiles({ it.staticFiles.enableWebjars() }) { _, http ->
        val swaggerVersion = io.javalin.testing.TestDependency.swaggerVersion
        val response = http.get("/webjars/swagger-ui/$swaggerVersion/swagger-ui.css")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31622400")
    }
}
