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
    fun `默认目录保持原来的缓存行为 - Cache-Control 返回 max-age=0`() = testStaticFiles(defaultStaticResourceApp) { _, http ->
        val response = http.get("/script.js")
        assertThat(response.status).isEqualTo(OK.code)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=0")
    }

    @Test
    fun `关闭缓存的目录不会返回 Cache-Control 头`() = testStaticFiles({ cfg ->
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
    fun `多个目录同时配置时各自的缓存设置互不影响 - 一个启用一个禁用`() = testStaticFiles({ cfg ->
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
    fun `多个目录同时配置时各自的缓存设置互不影响 - 不同缓存值`() = testStaticFiles({ cfg ->
        cfg.staticFiles.add {
            it.hostedPath = "/short-cache"
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.headers = mapOf(Header.CACHE_CONTROL to "max-age=60")
        }
        cfg.staticFiles.add {
            it.hostedPath = "/long-cache"
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.headers = mapOf(Header.CACHE_CONTROL to "max-age=31536000, immutable")
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
    fun `关闭缓存控制时其他自定义 headers 仍然正常工作`() = testStaticFiles({ cfg ->
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
    fun `WebJars 仍然保持默认的长缓存行为`() = testStaticFiles({ it.staticFiles.enableWebjars() }) { _, http ->
        val swaggerVersion = io.javalin.testing.TestDependency.swaggerVersion
        val response = http.get("/webjars/swagger-ui/$swaggerVersion/swagger-ui.css")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.headers.getFirst(Header.CACHE_CONTROL)).isEqualTo("max-age=31622400")
    }
}
