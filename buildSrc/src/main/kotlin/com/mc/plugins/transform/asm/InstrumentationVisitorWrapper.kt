/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hyperaware.transformer.plugin.asm

import org.gradle.api.logging.Logging
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

class InstrumentationVisitorWrapper(
    private val classVisitor: ClassVisitor
) : ClassVisitor(ASM_API_VERSION, classVisitor) {

    private var wasFieldAdded = false

    companion object {
        private const val ASM_API_VERSION = Opcodes.ASM7
    }

    private val logger = Logging.getLogger(InstrumentationVisitorWrapper::class.java)

    override fun visitMethod(
        access: Int,                 // public / private / final / etc
        methodName: String,          // e.g. "openConnection"
        methodDesc: String,          // e.g. "()Ljava/net/URLConnection;
        signature: String?,          // for any generics
        exceptions: Array<String>?   // declared exceptions thrown
    ): MethodVisitor {
        // Get a MethodVisitor using the ClassVisitor we're decorating
        val mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions)
        // Wrap it in a custom MethodVisitor
        return MethodVisitorWrapper(ASM_API_VERSION, mv, access, methodName, methodDesc)
    }

    override fun visitEnd() {

        if (!wasFieldAdded) {
            val visitField =
                visitField(
                    Opcodes.ACC_PUBLIC,
                    "mTestField",
                    "Ljava/util/HashMap<java.lang.String,java.lang.String>;",
                    null,
                    null
                )
            visitField.visitEnd()
            wasFieldAdded = true
        } else {
            super.visitEnd()
        }

    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        println(
            """Visiting field: ${name}
            |with desc: $descriptor
            |with signature $signature
            |with value $value
        """.trimMargin()
        )
        return super.visitField(access, name, descriptor, signature, value)
    }


    private inner class MethodVisitorWrapper(
        api: Int,
        mv: MethodVisitor,
        access: Int,
        methodName: String,
        methodDesc: String
    ) : AdviceAdapter(api, mv, access, methodName, methodDesc) {

        override fun visitMethodInsn(
            opcode: Int,    // type of method call this is (e.g. invokevirtual, invokestatic)
            owner: String,  // containing object
            name: String,   // name of the method
            desc: String,   // signature
            itf: Boolean
        ) {
            println("==Visiting method: $owner+$name+$desc")
            if (owner == "android/util/Log"
                && name == "d"
                && desc == "(Ljava/lang/String;Ljava/lang/String;)I"
            ) {
                println("==Modifying method: $owner+$name+$desc")
                super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/mc/codetransformer/utils/LoggerInterceptor",
                    "log",
                    "(Ljava/lang/String;Ljava/lang/String;)I",
                    false
                )

            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf)
            }
        }

        override fun visitJumpInsn(opcode: Int, label: Label?) {
            super.visitJumpInsn(opcode, label)
        }

    }

}
