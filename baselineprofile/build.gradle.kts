plugins {
    id("calwp.baselineprofile.producer")
}

// Generates the app's Baseline Profile on a Gradle Managed Device (ADR 0008). Run by the "Baseline Profile"
// workflow, which commits the result; never part of the normal build.
val profileDevice = "pixel6Api34"

android {
    namespace = "io.github.nabin47.calwallpaper.baselineprofile"
    targetProjectPath = ":app"

    testOptions.managedDevices.localDevices {
        create(profileDevice) {
            device = "Pixel 6"
            apiLevel = 34
            // AOSP images allow root, which the generator needs on an emulator (Google images do not).
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    managedDevices += profileDevice
    useConnectedDevices = false
}
