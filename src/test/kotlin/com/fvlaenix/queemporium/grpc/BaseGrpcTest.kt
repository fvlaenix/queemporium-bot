package com.fvlaenix.queemporium.grpc

import com.fvlaenix.duplicate.protobuf.DuplicateImagesServiceGrpcKt
import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.configuration.DuplicateImageServiceConfig
import com.fvlaenix.queemporium.database.GuildInfoConnector
import com.fvlaenix.queemporium.koin.BaseKoinTest
import com.fvlaenix.queemporium.service.DuplicateImageService
import com.fvlaenix.queemporium.service.DuplicateImageServiceImpl
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.koin.core.Koin
import org.koin.dsl.module
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Base class for gRPC services integration testing.
 * Extends BaseKoinTest and provides setup for test gRPC server and test services.
 */
abstract class BaseGrpcTest : BaseKoinTest() {
  private val logger = Logger.getLogger(BaseGrpcTest::class.java.name)

  // Test gRPC server
  protected lateinit var grpcServer: TestGrpcServer

  // Test services
  protected lateinit var duplicateService: TestDuplicateImagesService

  // Port on which the test server runs
  protected var serverPort: Int = 0

  // Koin for service access
  protected lateinit var koin: Koin

  // gRPC test client
  protected lateinit var duplicateImagesClient: DuplicateImagesServiceGrpcKt.DuplicateImagesServiceCoroutineStub
  protected lateinit var clientChannel: ManagedChannel

  /**
   * Sets up test environment before each test.
   */
  @BeforeEach
  fun setupGrpcServer() {
    logger.log(Level.INFO, "Setting up test gRPC server")

    // Initialize test services
    duplicateService = TestDuplicateImagesService()

    // Start server
    grpcServer = TestGrpcServer()
    grpcServer.registerService(duplicateService)
    serverPort = grpcServer.start()

    logger.log(Level.INFO, "Test gRPC server started on port $serverPort")

    // Configure Koin
    koin = setupBotKoinWithGrpc(serverPort)

    // Create client channel for direct API calls
    clientChannel = ManagedChannelBuilder.forAddress("localhost", serverPort)
      .usePlaintext()
      .build()

    duplicateImagesClient = DuplicateImagesServiceGrpcKt
      .DuplicateImagesServiceCoroutineStub(clientChannel)
  }

  /**
   * Releases resources after each test.
   */
  @AfterEach
  fun tearDownGrpcServer() {
    logger.log(Level.INFO, "Shutting down test gRPC server")

    // Close client channel
    if (::clientChannel.isInitialized) {
      clientChannel.shutdown()
    }

    // Stop server
    if (::grpcServer.isInitialized) {
      grpcServer.stop()
    }
  }

  /**
   * Configures Koin to use test gRPC server.
   *
   * @param port Port on which the test server runs
   * @return Configured Koin instance
   */
  private fun setupBotKoinWithGrpc(port: Int): Koin {
    return setupBotKoin {}
      .apply {
        loadModules(listOf(module {
          single {
            DuplicateImageServiceConfig(
              hostname = "localhost",
              port = port
            )
          }

          single<DuplicateImageService> { DuplicateImageServiceImpl(get()) }
        }))
      }
  }

  /**
   * Returns connector for working with guild information.
   */
  protected fun getGuildInfoConnector(): GuildInfoConnector {
    val databaseConfig = koin.get<DatabaseConfiguration>()
    return GuildInfoConnector(databaseConfig.toDatabase())
  }

  // Helper methods for setting up test scenarios

  /**
   * Simulates server unavailability.
   */
  protected fun simulateServerUnavailable() {
    duplicateService.simulateUnavailable = true
  }

  /**
   * Simulates server timeout.
   *
   * @param timeoutMs Timeout in milliseconds
   */
  protected fun simulateServerTimeout(timeoutMs: Long = 10000) {
    duplicateService.simulateTimeout = true
    duplicateService.timeoutDuration = timeoutMs
  }

  /**
   * Restores normal server behavior.
   */
  protected fun restoreServerNormalBehavior() {
    duplicateService.simulateUnavailable = false
    duplicateService.simulateTimeout = false
  }
}
