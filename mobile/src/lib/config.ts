import Constants from 'expo-constants';
import { Platform } from 'react-native';

/**
 * Where the Spring Boot server is. Set EXPO_PUBLIC_API_URL for a deployed server. In development it's assumed to
 * run on the same computer as the Expo dev server, on port 8080, so a phone on the same Wi-Fi can reach it.
 */
export function apiBaseUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_URL;
  if (fromEnv) return fromEnv.replace(/\/$/, '');
  if (Platform.OS === 'web') return `${window.location.protocol}//${window.location.hostname}:8080`;
  // e.g. "192.168.1.20:8081": the computer running "npx expo start".
  const devHost = Constants.expoConfig?.hostUri?.split(':')[0];
  if (devHost) return `http://${devHost}:8080`;
  // The Android emulator reaches the computer at 10.0.2.2.
  return Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
}
