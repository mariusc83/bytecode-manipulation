repositories {
    google()
    jcenter()
}

plugins {
    `kotlin-dsl`
}


dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:3.5.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.50")

    implementation("commons-io:commons-io:2.6")
    implementation("org.ow2.asm:asm:7.1")
    testImplementation("junit:junit:4.12")
    testImplementation("org.assertj:assertj-core:3.13.2")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.0.0")
}