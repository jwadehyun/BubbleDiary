import { router } from 'expo-router';
import { type ReactNode, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Avatar } from '@/components/Avatar';
import { TopBar } from '@/components/TopBar';
import { api } from '@/lib/api';
import { confirm } from '@/lib/confirm';
import { pickPhoto } from '@/lib/image';
import { usePosts } from '@/lib/posts';
import { useSession } from '@/lib/session';
import { colors } from '@/lib/theme';
import type { FanVoice, Settings as DiarySettings } from '@/lib/types';

const VOICES: { value: FanVoice; label: string }[] = [
  { value: 'english', label: 'English' },
  { value: 'korean', label: '한국어' },
  { value: 'mixed', label: 'Global' },
];

export default function Settings() {
  const { me, setMe, signOut } = useSession();
  const { removeAll } = usePosts();
  const [form, setForm] = useState<DiarySettings | undefined>(me?.settings);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  if (!me || !form) return null;
  const set = <K extends keyof DiarySettings>(key: K, value: DiarySettings[K]) => setForm({ ...form, [key]: value });

  const done = async () => {
    if (JSON.stringify(form) === JSON.stringify(me.settings)) return router.back();
    setSaving(true);
    setError('');
    try {
      // Empty text clears a field on the server (null would mean "unchanged").
      setMe(
        await api.updateSettings({
          ...form,
          avatar: form.avatar ?? '',
          statusText: form.statusText ?? '',
          profileMessage: form.profileMessage ?? '',
          fanPrompt: form.fanPrompt ?? '',
        }),
      );
      router.back();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  const deleteAccount = async () => {
    const ok = await confirm(
      'Delete your account?',
      'Your profile, every diary entry and all replies are deleted for good.',
      'Delete account',
    );
    if (!ok) return;
    try {
      await api.deleteAccount();
      await signOut();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <SafeAreaView style={styles.screen} edges={['top', 'bottom']}>
      <TopBar>
        <Text style={styles.heading}>Settings</Text>
        {saving ? (
          <ActivityIndicator color={colors.accent} />
        ) : (
          <Pressable onPress={done} hitSlop={10}>
            <Text style={styles.done}>Done</Text>
          </Pressable>
        )}
      </TopBar>

      <ScrollView style={styles.scroll} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {!!error && <Text style={styles.error}>{error}</Text>}

        <Section title="Profile">
          <View style={styles.profileRow}>
            <Pressable
              style={styles.avatarPick}
              onPress={async () => {
                const picked = await pickPhoto(256, true).catch(() => undefined);
                if (picked) set('avatar', picked);
              }}
            >
              <Avatar name={form.displayName} src={form.avatar} size={64} />
              <Text style={styles.small}>Change</Text>
            </Pressable>
            <View style={styles.flex}>
              <Field label="Your name" value={form.displayName} max={40} onChange={(v) => set('displayName', v)} />
              <Field label="Fandom name" value={form.fandom} max={40} onChange={(v) => set('fandom', v)} />
            </View>
          </View>
          {form.avatar && (
            <Text style={styles.link} onPress={() => set('avatar', null)}>
              Remove photo
            </Text>
          )}
          <Field
            label="Status"
            value={form.statusText ?? ''}
            max={60}
            placeholder="in Tokyo 🗼"
            onChange={(v) => set('statusText', v)}
          />
          <Field
            label="Profile message"
            value={form.profileMessage ?? ''}
            max={150}
            placeholder="thank you for always being here 💜"
            onChange={(v) => set('profileMessage', v)}
          />
        </Section>

        <Section title="Fans">
          <Text style={styles.label}>Language & vibe</Text>
          <View style={styles.segments}>
            {VOICES.map((v) => (
              <Pressable
                key={v.value}
                style={[styles.segment, form.voice === v.value && styles.segmentOn]}
                onPress={() => set('voice', v.value)}
              >
                <Text style={[styles.segmentText, form.voice === v.value && styles.segmentTextOn]}>{v.label}</Text>
              </Pressable>
            ))}
          </View>
          <Field
            label="How should your fans talk?"
            value={form.fanPrompt ?? ''}
            max={300}
            multiline
            placeholder="e.g. they call me bread, tease me about my dance practice, and are very dramatic"
            onChange={(v) => set('fanPrompt', v)}
          />
          <Text style={styles.hint}>
            {me.plan === 'premium'
              ? 'Premium: 230 replies per post (30 written by AI) and no anti-fans.'
              : 'Free: 100 replies per post. 15 fans and 1 anti-fan write about your post with AI; the rest are quick reactions.'}
            {!me.aiEnabled && ' The server has no OpenRouter key yet, so only quick reactions appear.'}
          </Text>
        </Section>

        <Section title="Account">
          <Text style={styles.account}>Signed in as {me.email ?? me.name ?? 'you'}</Text>
          <Pressable style={styles.outline} onPress={signOut}>
            <Text style={styles.outlineText}>Sign out</Text>
          </Pressable>
          <Pressable
            style={styles.outline}
            onPress={async () => {
              if (await confirm('Delete all entries?', 'Every diary entry and all replies will be gone.', 'Delete all')) {
                await removeAll();
              }
            }}
          >
            <Text style={[styles.outlineText, styles.danger]}>Delete all entries</Text>
          </Pressable>
          <Pressable style={styles.outline} onPress={deleteAccount}>
            <Text style={[styles.outlineText, styles.danger]}>Delete account</Text>
          </Pressable>
        </Section>
      </ScrollView>
    </SafeAreaView>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {children}
    </View>
  );
}

function Field(props: {
  label: string;
  value: string;
  max: number;
  placeholder?: string;
  multiline?: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{props.label}</Text>
      <TextInput
        style={[styles.input, props.multiline && styles.multiline]}
        value={props.value}
        onChangeText={props.onChange}
        maxLength={props.max}
        placeholder={props.placeholder}
        placeholderTextColor={colors.muted}
        multiline={props.multiline}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  heading: { flex: 1, fontSize: 17, fontWeight: '700', color: colors.text },
  done: { fontSize: 16, fontWeight: '600', color: colors.accent },
  scroll: { flex: 1, backgroundColor: colors.bg },
  content: { padding: 16, gap: 14, paddingBottom: 40 },
  flex: { flex: 1 },
  section: { backgroundColor: colors.surface, borderRadius: 16, padding: 14, gap: 4 },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.6,
    textTransform: 'uppercase',
    color: colors.muted,
    marginBottom: 8,
  },
  profileRow: { flexDirection: 'row', gap: 14 },
  avatarPick: { alignItems: 'center', gap: 4 },
  small: { fontSize: 12, color: colors.accent },
  field: { marginBottom: 10 },
  label: { fontSize: 13, color: colors.text, marginBottom: 6 },
  input: {
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: colors.bg,
    borderRadius: 10,
    paddingHorizontal: 11,
    paddingVertical: 9,
    fontSize: 15,
    color: colors.text,
  },
  multiline: { minHeight: 80, textAlignVertical: 'top' },
  segments: { flexDirection: 'row', backgroundColor: colors.bg, borderRadius: 10, padding: 3, marginBottom: 12 },
  segment: { flex: 1, paddingVertical: 8, borderRadius: 8, alignItems: 'center' },
  segmentOn: { backgroundColor: colors.surface },
  segmentText: { fontSize: 14, color: colors.muted },
  segmentTextOn: { color: colors.accent, fontWeight: '600' },
  hint: { fontSize: 12, lineHeight: 17, color: colors.muted },
  link: { color: colors.accent, fontSize: 13, marginBottom: 10 },
  account: { fontSize: 14, color: colors.text, marginBottom: 8 },
  outline: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 12,
    paddingVertical: 11,
    alignItems: 'center',
    marginTop: 6,
  },
  outlineText: { fontSize: 15, fontWeight: '600', color: colors.text },
  danger: { color: colors.danger },
  error: { color: colors.danger, textAlign: 'center' },
});
