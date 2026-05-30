package com.pascal.ocr_libs.data.tensorflow

import java.lang.reflect.Method
import java.nio.ByteBuffer

class TensorFlowLiteInterpreter(modelBuffer: ByteBuffer) {
    private val delegate: Any
    private val runMethod: Method
    private val runForMultipleInputsOutputsMethod: Method
    private val closeMethod: Method

    init {
        val interpreterClass = runCatching { Class.forName("org.tensorflow.lite.Interpreter") }
            .getOrElse {
                throw IllegalStateException(
                    "TensorFlow Lite runtime is not on the classpath. Add a 16 KB page-size compatible TensorFlow Lite runtime dependency in the app module before using TensorFlowLiteOcrEngine.",
                    it
                )
            }
        delegate = interpreterClass.getConstructor(ByteBuffer::class.java).newInstance(modelBuffer)
        runMethod = interpreterClass.getMethod("run", Any::class.java, Any::class.java)
        runForMultipleInputsOutputsMethod = interpreterClass.getMethod(
            "runForMultipleInputsOutputs",
            Array<Any>::class.java,
            MutableMap::class.java
        )
        closeMethod = interpreterClass.getMethod("close")
    }

    fun run(input: Any, output: Any) {
        runMethod.invoke(delegate, input, output)
    }

    fun runForMultipleInputsOutputs(inputs: Array<Any>, outputs: MutableMap<Int, Any>) {
        runForMultipleInputsOutputsMethod.invoke(delegate, inputs, outputs)
    }

    fun close() {
        closeMethod.invoke(delegate)
    }
}
