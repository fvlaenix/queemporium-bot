package com.fvlaenix.queemporium.grpc

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Universal test gRPC server for use in integration tests.
 * Allows dynamic service registration and server lifecycle management.
 *
 * @param port Port on which the server will run. If 0 is specified, a random available port will be chosen.
 */
class TestGrpcServer(private val port: Int = 0) {
    private val logger = Logger.getLogger(TestGrpcServer::class.java.name)
    private lateinit var server: Server
    private val services = mutableMapOf<String, BindableService>()

    /**
     * Registers a gRPC service for use on the server.
     *
     * @param service gRPC service implementation to register.
     */
    fun registerService(service: BindableService) {
        logger.log(Level.INFO, "Registering service: ${service.javaClass.name}")
        services[service.javaClass.name] = service
    }

    /**
     * Starts gRPC server with all registered services.
     *
     * @return Actual port on which the server is running (especially important if port=0)
     */
    fun start(): Int {
        logger.log(Level.INFO, "Starting gRPC server on port: $port")

        val serverBuilder = ServerBuilder.forPort(port)
        services.values.forEach { serverBuilder.addService(it) }

        server = serverBuilder.build()
        server.start()

        val actualPort = server.port
        logger.log(Level.INFO, "gRPC server started on port: $actualPort")

        // Add hook for proper shutdown when JVM stops
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.log(Level.INFO, "Shutting down gRPC server (shutdown hook)")
            stop()
        })

        return actualPort
    }

    /**
     * Stops gRPC server and releases resources.
     */
    fun stop() {
        if (::server.isInitialized) {
            logger.log(Level.INFO, "Stopping gRPC server")
            server.shutdown()
            try {
                // Wait for server to terminate gracefully
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.log(Level.WARNING, "gRPC server did not terminate within timeout period, forcing shutdown")
                    server.shutdownNow()
                }
            } catch (e: InterruptedException) {
                logger.log(Level.SEVERE, "Interruption while stopping gRPC server", e)
                server.shutdownNow()
                // Restore interrupt flag of current thread
                Thread.currentThread().interrupt()
            }
            logger.log(Level.INFO, "gRPC server stopped")
        }
    }

    /**
     * Checks if the server is running.
     *
     * @return true if server is running, false otherwise
     */
    fun isRunning(): Boolean {
        return ::server.isInitialized && !server.isShutdown && !server.isTerminated
    }

    /**
     * Returns a service by its type if it was registered.
     *
     * @param T Service type
     * @return Service instance or null if service is not registered
     */
    internal inline fun <reified T : BindableService> getService(): T? {
        return services[T::class.java.name] as? T
    }
}
