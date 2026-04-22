package io.javalin.examples

import io.javalin.Javalin
import io.javalin.http.Header
import io.javalin.http.staticfiles.Location

fun main() {
    val app = Javalin.create { cfg ->
        cfg.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.hostedPath = "/default-cache"
        }

        cfg.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.hostedPath = "/no-cache"
            it.enableCacheControl = false
        }

        cfg.staticFiles.add {
            it.directory = "/public"
            it.location = Location.CLASSPATH
            it.hostedPath = "/long-cache"
            it.headers = mapOf(Header.CACHE_CONTROL to "max-age=31536000, immutable")
        }
    }.start(7070)

    println("Server started on http://localhost:7070")
    println("")
    println("=== 按目录单独控制缓存头示例 ===")
    println("")
    println("1. 默认缓存行为 (enableCacheControl = true):")
    println("   http://localhost:7070/default-cache/script.js")
    println("   Cache-Control: max-age=0")
    println("")
    println("2. 关闭缓存头 (enableCacheControl = false):")
    println("   http://localhost:7070/no-cache/script.js")
    println("   无 Cache-Control 头")
    println("")
    println("3. 自定义缓存值:")
    println("   http://localhost:7070/long-cache/script.js")
    println("   Cache-Control: max-age=31536000, immutable")
    println("")
    println("=== 验证方式 ===")
    println("使用 curl 验证响应头:")
    println("  curl -I http://localhost:7070/default-cache/script.js")
    println("  curl -I http://localhost:7070/no-cache/script.js")
    println("  curl -I http://localhost:7070/long-cache/script.js")
    println("")
    println("=== 配置方式 ===")
    println("""
        // 默认配置（保持原有行为）
        cfg.staticFiles.add("/public", Location.CLASSPATH)
        
        // 关闭缓存头
        cfg.staticFiles.add {
            it.directory = "/public"
            it.enableCacheControl = false
        }
        
        // 自定义缓存值
        cfg.staticFiles.add {
            it.directory = "/public"
            it.headers = mapOf(Header.CACHE_CONTROL to "max-age=31536000, immutable")
        }
        
        // 多个目录独立配置
        cfg.staticFiles.add { it.hostedPath = "/assets"; it.directory = "/public/assets" }  // 默认缓存
        cfg.staticFiles.add { it.hostedPath = "/api-docs"; it.directory = "/public/docs"; it.enableCacheControl = false }  // 无缓存
    """.trimIndent())
}
