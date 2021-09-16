**Android OpenTok Publisher with DeepAR integration**

OpenTok Publisher in Android integrated with DeepAR Android SDK

**Description**

This sample application shows how to publish into OpenTok session while using DeepAR filters/effects/masks in Android.

**Getting Started**

_**Prerequisites**_

Android Studio

_**Installation**_

1.  Clone the repository
2.  Go to [https://developer.deepar.ai](https://developer.deepar.ai), sign up, create the project and the Android app, copy the license key and paste it to ApplicationConfig.java (DEEPAR_LICENSEKEY)
3.  Download the SDK from [https://developer.deepar.ai](https://developer.deepar.ai) and copy the deepar.aar into OpenTok-DeepAR-Android-Integration-sampleapp/deepar
4.  Sign up on [Vonage Video API](https://www.vonage.com/communications-apis/video/), create an API KEY, then generate a sessionID and token to run Video session. The token should have Publisher role. Add the details under API_KEY, SESSION_ID and TOKEN in ApplicationConfig.java
5.  Configure mask/effect assets, if required, under MASK in ApplicationConfig.java. Make sure to place the asset file under assets directory
6.  Run the app on an Android device, join the Video Session from other device. You can use [Playground](https://tokbox.com/developer/tools/playground/) or other applications for the second endpoint.
7.  Connect to OpenTok on both endpoints to see the video with DeepAR filter applied.

**Demo video**

[![Demo](https://i.vimeocdn.com/video/1242612072_1280x720)](https://vimeo.com/606027334 "")
