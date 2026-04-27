plugins {
    id("com.android.application")  // describe the this is an android application
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")  // annotation processor
    id("androidx.navigation.safeargs.kotlin")  // plugin for the navigation
    id("com.google.gms.google-services")  // firebase
    id("com.google.firebase.crashlytics")   // crashlytics
}

android {
//    alias : Cert
//    password : 123456
    namespace = "com.orderMate" // "com.orderMate"
    compileSdk = 34  // for clover app we need to compile the app to max version that android supports
    defaultConfig {
        applicationId = "com.orderMate" // "com.orderMate"
        minSdk = 17   // for clover apps we need to setup the minimum support sdk to 17
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 25  // for clover app this is the max target version it supports.
        versionCode = 35
        versionName = "1.0.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/Cert")
            storePassword = "123456"
            keyAlias = "Cert"
            keyPassword = "123456"
            enableV1Signing = true
            enableV2Signing = false
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            multiDexEnabled = true
            signingConfig = signingConfigs.getByName("release")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            multiDexEnabled = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true  // enable the view binding in the project
        buildConfig = true  // enable the build config properties in the project
    }
    packagingOptions {
        resources.excludes.add("META-INF/kotlinx_coroutines_android.version")
        resources.excludes.add("META-INF/com.google.android.material_material.version")
        resources.excludes.add("META-INF/kotlinx_coroutines_core.version")
        resources.excludes.add("META-INF/androidx.*")
        resources.excludes.add("META-INF/services/kotlinx.coroutines.*")
        resources.excludes.add("META-INF/com/android/build/gradle/app-metadata.properties")
        resources.excludes.add("META-INF/services/kotlinx.coroutines.*")
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val lifecycleVersion = "2.5.0"
val archVersion = "2.2.0"
val textDependency = "1.0.6"
val timberVersion = "4.7.1"
val loggerVersion = "4.12.0"
val coreVersion = "1.9.0"
val appCompactVersion = "1.6.1"
val materialVersion = "1.11.0"
val constraintLayoutVersion = "2.1.4"
val junitVersion = "4.13.2"
val retrofitVersion = "2.9.0"
val coroutineDependency = "1.7.3"
val navigationDependency = "2.7.7"
val cloverVersion = "304"
val encryptedSharedPreferenceVersion = "1.1.0-alpha02"

dependencies {
    implementation("androidx.core:core-ktx:${coreVersion}")
    implementation("androidx.appcompat:appcompat:${appCompactVersion}")
    implementation("com.google.android.material:material:${materialVersion}")
    implementation("androidx.constraintlayout:constraintlayout:${constraintLayoutVersion}")
    testImplementation("junit:junit:${junitVersion}")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("com.intuit.sdp:sdp-android:$textDependency")
    implementation("com.intuit.ssp:ssp-android:$textDependency")
    implementation("com.jakewharton.timber:timber:${timberVersion}")
    implementation("com.squareup.okhttp3:logging-interceptor:${loggerVersion}")
    implementation("com.squareup.retrofit2:retrofit:${retrofitVersion}")
    implementation("com.squareup.retrofit2:converter-gson:${retrofitVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${coroutineDependency}")
    implementation("androidx.navigation:navigation-fragment-ktx:${navigationDependency}")
    implementation("androidx.navigation:navigation-ui-ktx:${navigationDependency}")
    implementation("com.clover.sdk:clover-android-sdk:${cloverVersion}")

    // firebase
    implementation(platform("com.google.firebase:firebase-bom:28.0.0"))
    implementation("com.google.firebase:firebase-core")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")


    // messaging Bird
    implementation("com.messagebird:messagebird-api:6.1.7")
    
    // FlexboxLayout for iOS-style chip layouts (#80 requirement)
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    
    // CardView for card-based layouts
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Glide for image loading (#59 - dynamic item icons from Clover)
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")
    
    // WorkManager for scheduled notifications and printing (#83 requirement)
    implementation("androidx.work:work-runtime-ktx:2.7.1")
}