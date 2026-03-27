package com.dp.logcatapp.util

interface ProcessStarter {
  fun start(
    cmd: List<String>,
    builder: ProcessBuilder.() -> Unit = {},
  ): Process
}

class DefaultProcessStarter : ProcessStarter {
  override fun start(
    cmd: List<String>,
    builder: ProcessBuilder.() -> Unit
  ): Process {
    val processBuilder = ProcessBuilder(cmd)
    processBuilder.builder()
    return processBuilder.start()
  }
}
