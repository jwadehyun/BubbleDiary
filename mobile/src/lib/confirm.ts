import { Alert, Platform } from 'react-native';

/** Asks before something destructive. Resolves to true if the user confirmed. */
export function confirm(title: string, message: string, action: string): Promise<boolean> {
  // React Native's Alert has no buttons on web.
  if (Platform.OS === 'web') return Promise.resolve(window.confirm(`${title}\n\n${message}`));
  return new Promise((resolve) =>
    Alert.alert(title, message, [
      { text: 'Cancel', style: 'cancel', onPress: () => resolve(false) },
      { text: action, style: 'destructive', onPress: () => resolve(true) },
    ]),
  );
}
