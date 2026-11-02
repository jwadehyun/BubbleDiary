import * as AppleAuthentication from 'expo-apple-authentication';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { api } from '@/lib/api';
import { appleAvailable, appleIdToken, googleAvailable, googleIdToken } from '@/lib/nativeSignIn';
import { useSession } from '@/lib/session';
import { colors } from '@/lib/theme';

export default function SignIn() {
  const { signIn } = useSession();
  const [apple, setApple] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [devName, setDevName] = useState('');

  useEffect(() => {
    appleAvailable().then(setApple);
  }, []);

  /** getTokens resolves to null when the user cancelled the Google/Apple sheet. */
  const run = async (getTokens: () => Promise<Awaited<ReturnType<typeof api.signIn>> | null>) => {
    setBusy(true);
    setError('');
    try {
      const tokens = await getTokens();
      if (tokens) await signIn(async () => tokens);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const withGoogle = () =>
    run(async () => {
      const idToken = await googleIdToken();
      return idToken ? api.signIn('google', idToken) : null;
    });

  const withApple = () =>
    run(async () => {
      const result = await appleIdToken();
      return result ? api.signIn('apple', result.idToken, result.name) : null;
    });

  const asDev = () => run(() => api.devSignIn(devName.trim() || 'Dev'));

  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.hero}>
        <Text style={styles.logo}>🫧</Text>
        <Text style={styles.title}>BubbleDiary</Text>
        <Text style={styles.subtitle}>Write your diary like a star.{'\n'}Your fans are waiting.</Text>
      </View>

      <View style={styles.actions}>
        {apple && (
          <AppleAuthentication.AppleAuthenticationButton
            buttonType={AppleAuthentication.AppleAuthenticationButtonType.CONTINUE}
            buttonStyle={AppleAuthentication.AppleAuthenticationButtonStyle.BLACK}
            cornerRadius={999}
            style={styles.appleButton}
            onPress={withApple}
          />
        )}
        {googleAvailable && (
          <Pressable style={[styles.button, styles.google]} onPress={withGoogle} disabled={busy}>
            <Text style={styles.googleText}>Continue with Google</Text>
          </Pressable>
        )}

        {__DEV__ && (
          <View style={styles.dev}>
            <Text style={styles.devLabel}>Development sign-in (needs DEV_LOGIN=true on the server)</Text>
            <TextInput
              style={styles.input}
              value={devName}
              onChangeText={setDevName}
              placeholder="Your name"
              placeholderTextColor={colors.muted}
              autoCapitalize="words"
              onSubmitEditing={asDev}
            />
            <Pressable style={styles.button} onPress={asDev} disabled={busy}>
              <Text style={styles.buttonText}>Sign in for testing</Text>
            </Pressable>
          </View>
        )}

        {busy && <ActivityIndicator color={colors.accent} />}
        {!!error && <Text style={styles.error}>{error}</Text>}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg, justifyContent: 'space-between', padding: 28 },
  hero: { alignItems: 'center', marginTop: 80, gap: 10 },
  logo: { fontSize: 72 },
  title: { fontSize: 30, fontWeight: '800', color: colors.text },
  subtitle: { fontSize: 15, color: colors.muted, textAlign: 'center', lineHeight: 22 },
  actions: { gap: 12, marginBottom: 20 },
  appleButton: { width: '100%', height: 48 },
  button: { backgroundColor: colors.accent, borderRadius: 999, height: 48, alignItems: 'center', justifyContent: 'center' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  google: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line },
  googleText: { color: colors.text, fontSize: 16, fontWeight: '600' },
  dev: { gap: 8, padding: 14, borderRadius: 16, borderWidth: 1, borderStyle: 'dashed', borderColor: colors.accent },
  devLabel: { fontSize: 12, color: colors.muted },
  input: {
    backgroundColor: colors.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.line,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    color: colors.text,
  },
  error: { color: colors.danger, textAlign: 'center' },
});
