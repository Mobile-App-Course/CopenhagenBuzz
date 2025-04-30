plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")

    // Add the Google Maps services Gradle plugin
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")

}

android {
    namespace = "dk.itu.moapd.copenhagenbuzz.ralc.nhca"
    compileSdk = 35

    defaultConfig {
        applicationId = "dk.itu.moapd.copenhagenbuzz.ralc.nhca"
        minSdk = 27
        targetSdk = 35
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        // Or alternatively disable just this specific error:
        // disable += "NotificationPermission"
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.javafaker)
    implementation (libs.picasso)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.dotenv.kotlin)

    implementation("com.github.bumptech.glide:glide:4.15.1")

    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Google Play Services Location
    implementation(libs.gms.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location.v2101)
    implementation(libs.google.maps.services)

    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.ui.auth)
    implementation(libs.firebase.ui.database)
    // Add the dependencies for any other desired Firebase products
    implementation(libs.firebase.bom.v3380)
    implementation(libs.firebase.ui.storage)
    implementation(libs.google.firebase.storage.ktx)
    // https://firebase.google.com/docs/android/setup#available-libraries


}