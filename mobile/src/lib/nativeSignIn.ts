import * as AppleAuthentication from 'expo-apple-authentication';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import { Platform } from 'react-native';

type GoogleModule = typeof import('@react-native-google-signin/google-signin');

const inExpoGo = Constants.executionEnvironment === ExecutionEnvironment.StoreClient;
// The web OAuth client id (the same one the server uses): Google puts it in the ID token the server checks.
const googleWebClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID;

/** Google sign-in needs native code that Expo Go doesn't have, so it only works in a development or store build. */
export const googleAvailable = Platform.OS !== 'web' && !inExpoGo && !!googleWebClientId;

/** Returns Google's ID token, or null if the user cancelled. */
export async function googleIdToken(): Promise<string | null> {
  // Loaded only when used, so the app doesn't crash in Expo Go where the native part is missing.
  const { GoogleSignin, isSuccessResponse } = require('@react-native-google-signin/google-signin') as GoogleModule;
  GoogleSignin.configure({
    webClientId: googleWebClientId,
    iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
  });
  await GoogleSignin.hasPlayServices();
  const response = await GoogleSignin.signIn();
  if (!isSuccessResponse(response)) return null;
  if (!response.data.idToken) throw new Error('Google did not return a sign-in token');
  return response.data.idToken;
}

export async function appleAvailable() {
  return Platform.OS === 'ios' && (await AppleAuthentication.isAvailableAsync());
}

/** Returns Apple's ID token and, on the very first sign-in only, the user's name. Null if cancelled. */
export async function appleIdToken(): Promise<{ idToken: string; name?: string } | null> {
  try {
    const credential = await AppleAuthentication.signInAsync({
      requestedScopes: [
        AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
        AppleAuthentication.AppleAuthenticationScope.EMAIL,
      ],
    });
    if (!credential.identityToken) throw new Error('Apple did not return a sign-in token');
    const name = [credential.fullName?.givenName, credential.fullName?.familyName].filter(Boolean).join(' ');
    return { idToken: credential.identityToken, name: name || undefined };
  } catch (e) {
    if ((e as { code?: string }).code === 'ERR_REQUEST_CANCELED') return null;
    throw e;
  }
}
