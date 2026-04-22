package io.javalin.examples

import io.javalin.Javalin
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
            it.cacheControl = "max-age=31536000, immutable"
        }
    }.start(7070)

    println("Server started on http://localhost:7070")
    println("")
    println("Available endpoints:")
    println("  http://localhost:7070/default-cache/script.js  - Default cache (max-age=0)")
    println("  http://localhost:7070/no-cache/script.js       - No cache header")
    println("  http://localhost:7070/long-cache/script.js     - Long cache (max-age=31536000, immutable)")
    println("")
    println("You can verify the cache headers using curl:")
    println("  curl -I http://localhost:7070/default-cache/script.js")
    println("  curl -I http://localhost:7070/no-cache/script.js")
    println("  curl -I http://localhost:7070/long-cache/script.js")
}
