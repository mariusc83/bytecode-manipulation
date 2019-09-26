package com.mc.plugins.transform

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.hyperaware.transformer.plugin.asm.ClassInstrumenter
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

class CodeTransformer(val project: Project, val appExtension: AppExtension) : Transform() {

    val logger = Logging.getLogger(CodeTransformer::class.java)
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
        logger.debug("==Starting the Transform==")
        println("==Starting the Transform==")
        transformInvocation.inputs.forEach {
            logger.debug("==Transformation Input ${it}")
            println("==Transformation Input ${it}")
            it.directoryInputs.forEach { directoryInput ->
                logger.debug("==Directory Input ${directoryInput.file.absolutePath}")
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

                            println("==Changed file ${changedFile.key.absolutePath}")
                            when (changedFile.value) {
                                Status.ADDED, Status.CHANGED -> {
                                    val normalisedPath =
                                        normalisePath(directoryInput.file, changedFile.key)

                                    val newFile = File(outDir, normalisedPath)
                                    ensureDirectoryExists(newFile.parentFile)
                                    println("==File to copy ${changedFile.key.absolutePath}")
                                    println("==Relative file ${normalisedPath}")
                                    changedFile.key.inputStream().use { input ->
                                        newFile.outputStream().use { output ->
                                            try {
                                                if (normalisedPath == "com/mc/codetransformer/myapplication/MainActivity.class") {
                                                    manipulateBytes(
                                                        input,
                                                        output,
                                                        transformInvocation
                                                    )
                                                    println("Successfully manipulated $normalisedPath")
                                                } else {
                                                    IOUtils.copy(input, output)
                                                }

                                            } catch (e: Exception) {
                                                println("Error manipulating $normalisedPath")
                                            }
                                        }
                                    }

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
                    println("  Copying ${directoryInput.file} to $outDir")
                    for (file in FileUtils.iterateFiles(directoryInput.file, null, true)) {
                        val relativeFile = normalisePath(directoryInput.file, file)
                        println("==File to copy ${file.absolutePath}")
                        println("==Relative file ${relativeFile}")
                        val destFile = File(outDir, relativeFile)
                        ensureDirectoryExists(destFile.parentFile)
                        IOUtils.buffer(file.inputStream()).use { inputStream ->
                            IOUtils.buffer(destFile.outputStream()).use { outputStream ->
                                try {
                                    if (relativeFile == "com/mc/codetransformer/myapplication/MainActivity.class") {
                                        manipulateBytes(
                                            inputStream,
                                            outputStream,
                                            transformInvocation
                                        )
                                        println("Successfully manipulated $relativeFile")

                                    } else {
                                        IOUtils.copy(inputStream, outputStream)
                                    }

                                } catch (e: Exception) {
                                    println("Error manipulating $relativeFile")
                                }
                            }
                        }
                    }
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