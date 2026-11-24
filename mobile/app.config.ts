import type { ExpoConfig } from 'expo/config';

// Your app's id in the App Store and Play Store. It must be unique and can't change after the first release.
const appId = process.env.APP_ID ?? 'com.jwadehyun.bubblediary';
// From your Google iOS OAuth client ("iOS URL scheme"), e.g. com.googleusercontent.apps.1234-abcd.
// Google sign-in is left out of the build until it's set.
const googleIosUrlScheme = process.env.GOOGLE_IOS_URL_SCHEME;

const config: ExpoConfig = {
  name: 'BubbleDiary',
  slug: 'bubblediary',
  scheme: 'bubblediary',
  version: '1.0.0',
  orientation: 'portrait',
  icon: './assets/icon.png',
  userInterfaceStyle: 'light',
  ios: {
    bundleIdentifier: appId,
    supportsTablet: false,
    usesAppleSignIn: true,
  },
  android: {
    package: appId,
    adaptiveIcon: {
      backgroundColor: '#F4F1FF',
      foregroundImage: './assets/android-icon-foreground.png',
      backgroundImage: './assets/android-icon-background.png',
      monochromeImage: './assets/android-icon-monochrome.png',
    },
    predictiveBackGestureEnabled: false,
  },
  web: {
    favicon: './assets/favicon.png',
  },
  plugins: [
    'expo-router',
    'expo-status-bar',
    'expo-secure-store',
    'expo-image',
    'expo-apple-authentication',
    ['expo-image-picker', { photosPermission: 'Allow BubbleDiary to share your photos with your fans.' }],
    ...(googleIosUrlScheme
      ? [['@react-native-google-signin/google-signin', { iosUrlScheme: googleIosUrlScheme }] as [string, object]]
      : []),
  ],
  experiments: {
    typedRoutes: true,
  },
};

export default config;
