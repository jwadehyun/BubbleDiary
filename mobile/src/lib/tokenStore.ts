import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import type { Tokens } from './types';

const KEY = 'bubblediary.tokens';

/**
 * Keeps the sign-in tokens between launches: in the iOS Keychain / Android Keystore on phones. The web preview
 * (development only) has no secure storage, so it uses localStorage.
 */
export async function loadTokens(): Promise<Tokens | null> {
  const raw = Platform.OS === 'web' ? localStorage.getItem(KEY) : await SecureStore.getItemAsync(KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Tokens;
  } catch {
    return null;
  }
}

export async function saveTokens(tokens: Tokens | null) {
  if (Platform.OS === 'web') {
    if (tokens) localStorage.setItem(KEY, JSON.stringify(tokens));
    else localStorage.removeItem(KEY);
  } else if (tokens) {
    await SecureStore.setItemAsync(KEY, JSON.stringify(tokens));
  } else {
    await SecureStore.deleteItemAsync(KEY);
  }
}
