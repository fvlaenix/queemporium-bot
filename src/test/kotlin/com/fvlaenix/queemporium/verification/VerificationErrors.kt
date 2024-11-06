package com.fvlaenix.queemporium.verification

class VerificationErrors {
  private val errors = mutableListOf<String>()

  fun addError(error: String) {
    errors.add(error)
  }

  fun hasErrors(): Boolean = errors.isNotEmpty()

  fun getErrors(): List<String> = errors.toList()

  fun clear() {
    errors.clear()
  }
}