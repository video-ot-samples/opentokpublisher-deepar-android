Android OpenTok Publisher with DeepAR integration

This example demonstrates how to publish into OpenTok session while using DeepAR face filters.

Description

TODO

Getting Started

Prerequisites

Android Studio

Installation

    Clone the repository
    Go to https://developer.deepar.ai, sign up, create the project and the Android app, copy the license key and paste it to ApplicationConfig.java (DEEPAR_LICENSEKEY)
    Download the SDK from https://developer.deepar.ai and copy the deepar.aar into OpenTok-DeepAR-Android-Integration-sampleapp/deepar
    Sign up on Vonage Video API, create an API KEY, then generate a sessionID and token to run Video session. The token should have Publisher role. Add the details under API_KEY, SESSION_ID and TOKEN in ApplicationConfig.java
    Configure mask/effect assets, if required, under MASK in ApplicationConfig.java. Make sure to place the asset file under assets directory
    Run the app on an Android device, join the Video Session from other device. You can use Playground or other applications for the second endpoint.
    Connect to OpenTok on both endpoints to see the video with DeepAR filter applied.

*Demo video and/or screenshots

TODO

