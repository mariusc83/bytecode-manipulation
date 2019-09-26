package com.mc.plugins

import com.android.build.gradle.AppExtension
import com.mc.plugins.transform.CodeTransformer
import org.gradle.api.Plugin
import org.gradle.api.Project

class CodeTransformerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidExt =
            project.extensions.findByName("android") as? AppExtension
        androidExt?.let {
            println("===FoundAndroidExtension===")
            it.registerTransform(CodeTransformer(project, it))
        }
    }
}