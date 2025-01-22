package com.fvlaenix.queemporium.service

interface SearchService {
  suspend fun search(imageUrl: String): List<String>
}