import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { SessionProvider, useSession } from '@/lib/session';
import { colors } from '@/lib/theme';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <SessionProvider>
        <StatusBar style="dark" />
        <Routes />
      </SessionProvider>
    </SafeAreaProvider>
  );
}

function Routes() {
  const { me, loading, error, retry } = useSession();

  if (loading) {
    return (
      <View style={styles.center}>
        <Text style={styles.logo}>🫧</Text>
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }
  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.logo}>🫧</Text>
        <Text style={styles.error}>{error}</Text>
        <Pressable style={styles.button} onPress={retry}>
          <Text style={styles.buttonText}>Try again</Text>
        </Pressable>
      </View>
    );
  }

  // Signed-in screens are only reachable with a session, and the sign-in screen only without one.
  return (
    <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: colors.bg } }}>
      <Stack.Protected guard={!!me}>
        <Stack.Screen name="(app)" />
      </Stack.Protected>
      <Stack.Protected guard={!me}>
        <Stack.Screen name="sign-in" />
      </Stack.Protected>
    </Stack>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16, padding: 32, backgroundColor: colors.bg },
  logo: { fontSize: 56 },
  error: { color: colors.text, textAlign: 'center', fontSize: 15 },
  button: { backgroundColor: colors.accent, borderRadius: 999, paddingHorizontal: 22, paddingVertical: 11 },
  buttonText: { color: '#fff', fontWeight: '600' },
});
