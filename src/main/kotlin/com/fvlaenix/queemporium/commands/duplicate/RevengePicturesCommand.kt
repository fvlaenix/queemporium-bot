package com.fvlaenix.queemporium.commands.duplicate

import com.fvlaenix.queemporium.configuration.DatabaseConfiguration
import com.fvlaenix.queemporium.database.MessageDuplicateDataConnector
import com.fvlaenix.queemporium.service.AnswerService
import com.fvlaenix.queemporium.service.DuplicateImageService
import net.dv8tion.jda.api.events.session.ReadyEvent

class RevengePicturesCommand(
  databaseConfiguration: DatabaseConfiguration,
  answerService: AnswerService,
  duplicateImageService: DuplicateImageService
) : ReportPictureCommand(databaseConfiguration, answerService, duplicateImageService) {
  private val messageDuplicateDataConnector = MessageDuplicateDataConnector(databaseConfiguration.toDatabase())

  override suspend fun onReadySuspend(event: ReadyEvent) {
    val compressSize = duplicateImageService.checkServerAliveness(event) ?: return

    runOverOld(event.jda, { message ->
      val messageId = message.id
      !messageDuplicateDataConnector.exists(messageId)
    }) { message ->
      getMessage(compressSize, message)
    }
  }
}