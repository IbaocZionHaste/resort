plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.resort"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.resort"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


    buildFeatures {
        dataBinding = true
    }

    // Add this block to exclude the conflicting files
    packagingOptions {
        exclude ("META-INF/DEPENDENCIES")
    }

}



dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.litert.metadata)
    implementation(libs.swiperefreshlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.firebase.messaging)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.viewpager2)
    implementation (libs.material.v190)
    implementation ("com.firebaseui:firebase-ui-database:7.2.0")


    // 🔥 Additional Dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation ("com.makeramen:roundedimageview:2.3.0")
    implementation ("com.google.maps.android:android-maps-utils:2.2.0")
    implementation ("com.hbb20:ccp:2.5.0")
    implementation ("com.google.android.gms:play-services-maps:19.0.0")
    implementation ("com.google.android.gms:play-services-location:20.0.0")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation ("androidx.navigation:navigation-ui-ktx:2.8.3")
    implementation ("com.github.simformsolutions:SSCustomBottomNavigation:1.0")
    implementation ("com.github.Foysalofficial:NafisBottomNav:5.0")
    implementation ("com.google.android.gms:play-services-auth-api-phone:18.0.1")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.twilio.sdk:twilio:8.0.0")
    implementation ("com.google.firebase:firebase-functions:20.4.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.0")


}