package com.mc.plugins.transform

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.hyperaware.transformer.plugin.asm.ClassInstrumenter
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.*

class CodeTransformer(val appExtension: AppExtension) : Transform() {

    override fun getName(): String {
        return CodeTransformer::class.java.simpleName
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun transform(transformInvocation: TransformInvocation) {
        println("==Starting the Transform==")
        transformInvocation.inputs.forEach {
            println("==Transformation Input ${it}")
            it.directoryInputs.forEach { directoryInput ->
                println("==Directory Input ${directoryInput.file}")
                val outDir = transformInvocation
                    .outputProvider
                    .getContentLocation(
                        directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes,
                        Format.DIRECTORY
                    )
                println("==Directory Output ${outDir}")
                ensureDirectoryExists(outDir)
                FileUtils.cleanDirectory(outDir)
                if (transformInvocation.isIncremental) {
                    directoryInput
                        .changedFiles
                        .asSequence()
                        .filter { it.key.isFile }
                        .forEach { changedFile ->
                            when (changedFile.value) {
                                Status.ADDED, Status.CHANGED -> {
                                    directoryInput.transform(
                                        outDir,
                                        changedFile.key,
                                        transformInvocation
                                    )

                                }
                                Status.REMOVED -> {
                                    val normalisedPath =
                                        normalisePath(directoryInput.file, changedFile.key)
                                    val existingFile = File(outDir, normalisedPath)
                                    println("==Removing ${changedFile.key.absolutePath}")
                                    FileUtils.forceDelete(existingFile)
                                }
                                else -> {
                                }
                            }
                        }
                } else {
                    directoryInput.file.files().forEach {
                        directoryInput.transform(
                            outDir,
                            it,
                            transformInvocation
                        )
                    }
                }


            }
        }
    }


    private fun File.files(): List<File> {
        val mutableList: MutableList<File> = mutableListOf()
        if (this.isDirectory) {
            val queue: LinkedList<File> = LinkedList()
            queue.add(this)
            while (queue.isNotEmpty()) {
                val dir = queue.poll()
                dir.listFiles().forEach {
                    if (it.isDirectory) {
                        queue.add(it)
                    } else {
                        mutableList.add(it)
                    }
                }
            }
        } else {
            mutableList.add(this)
        }

        return mutableList
    }

    private fun DirectoryInput.transform(
        outDir: File,
        file: File,
        transformInvocation: TransformInvocation
    ) {
        val normalisedPath =
            normalisePath(this.file, file)
        val newFile = File(outDir, normalisedPath)
        ensureDirectoryExists(newFile.parentFile)
        println("==File to copy ${file.absolutePath}")
        println("==Relative file ${normalisedPath}")
        println("==New file ${newFile.absolutePath}")

        IOUtils.buffer(file.inputStream()).use { inputStream ->
            IOUtils.buffer(newFile.outputStream()).use { outputStream ->
                try {
                    if (normalisedPath == "com/mc/codetransformer/myapplication/MainActivity.class") {
                        manipulateBytes(
                            inputStream,
                            outputStream,
                            transformInvocation
                        )
                        println("Successfully manipulated $normalisedPath")

                    } else {
                        IOUtils.copy(inputStream, outputStream)
                    }

                } catch (e: Throwable) {
                    println("Error manipulating $normalisedPath")
                }
            }
        }
    }

    private fun normalisePath(parent: File, child: File): String {
        var path = ""
        var currentFile: File? = child
        while (currentFile != null && currentFile != parent) {
            path = "/${currentFile.name}" + path
            currentFile = currentFile.parentFile
        }

        if (path.isEmpty()) {
            return path
        } else {
            return path.substring(1)
        }
    }

    private fun manipulateBytes(
        from: InputStream,
        to: OutputStream,
        invocation: TransformInvocation
    ) {
        val bytes = from.readBytes()
        val newBytes =
            instrumenterProducer(invocation, appExtension.bootClasspath).instrument(bytes)
        to.write(newBytes)

    }

    val instrumenterProducer: (TransformInvocation, List<File>) -> ClassInstrumenter =
        { transformInvocation, classPathCollection ->

            ClassInstrumenter(transformInvocation.runtimeClassPath(classPathCollection))
        }


    fun TransformInvocation.runtimeClassPath(bootClassPath: List<File>): List<URL> {
        val allTransformInputs =
            inputs + referencedInputs
        val allJarsAndDirs =
            allTransformInputs.map { ti -> (ti.directoryInputs + ti.jarInputs).map { i -> i.file } }
        val allClassesAtRuntime = bootClassPath + allJarsAndDirs.flatten()
        return allClassesAtRuntime.map { file -> file.toURI().toURL() }
    }

    fun ensureDirectoryExists(dir: File) {
        if (!((dir.isDirectory && dir.canWrite()) || dir.mkdirs())) {
            throw IOException("Can't write or create ${dir.path}")
        }
    }
}