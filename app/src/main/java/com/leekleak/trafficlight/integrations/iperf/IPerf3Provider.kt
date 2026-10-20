package com.leekleak.trafficlight.integrations.iperf

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class IPerf3Provider(context: Context) {

    private val binaryPath = "${context.applicationInfo.nativeLibraryDir}/libiperf3.so"

    val running: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val stopping: StateFlow<Boolean>
        field = MutableStateFlow(false)

    @Volatile
    private var currentProcess: Process? = null

    private val parser = IntervalStreamParser()

    private fun parseAndDeliver(line: String, callback: IperfCallback) {
        try {
            val results = parser.parseEvent(line)
            callback.onOutput(results)
        } catch (_: Exception) {}
    }

    private val ignoredErrors = listOf(
        "read interrupted by close() on another thread" // Thrown whenever the process is canceled by user
    )

    suspend fun runTest(arguments: Array<String>, callback: IperfCallback) =
        suspendCancellableCoroutine { cont ->
            running.value = true

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val fullArgs = buildList {
                        add(binaryPath)
                        addAll(arguments)
                        add("--json-stream")
                    }

                    val process = ProcessBuilder(fullArgs)
                        .redirectErrorStream(false)
                        .start()
                    currentProcess = process

                    val stderrJob = launch {
                        try {
                            BufferedReader(InputStreamReader(process.errorStream)).useLines { lines ->
                                lines.forEach { line ->
                                    if (line.isNotBlank() && !ignoredErrors.contains(line)) {
                                        callback.onError(line)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                        lines.forEach { line ->
                            if (line.isNotBlank()) {
                                parseAndDeliver(line, callback)
                            }
                        }
                    }

                    val exitCode = process.waitFor()
                    stderrJob.join()

                    if (exitCode != 0 && !stopping.value) {
                        callback.onError("iperf3 exited with code $exitCode")
                    }
                    callback.onComplete()
                } catch (e: Exception) {
                    if (!ignoredErrors.contains(e.message)) {
                        callback.onError(e.message ?: "Unknown error launching iperf3")
                    }
                    callback.onComplete()
                } finally {
                    currentProcess = null
                    running.value = false
                    stopping.value = false
                    if (cont.isActive) cont.resume(Unit)
                }
            }

            cont.invokeOnCancellation {
                stopTest()
            }
        }

    fun stopTest() {
        stopping.value = true
        currentProcess?.destroy()
        CoroutineScope(Dispatchers.IO).launch {
            val p = currentProcess ?: return@launch
            if (!p.waitFor(2, TimeUnit.SECONDS)) {
                p.destroyForcibly()
            }
        }
    }
}

interface IperfCallback {
    fun onOutput(event: IperfEvent)
    fun onError(error: String)
    fun onComplete()
}