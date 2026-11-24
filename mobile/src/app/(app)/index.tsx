import { Image } from 'expo-image';
import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Avatar } from '@/components/Avatar';
import { PostBubble } from '@/components/PostBubble';
import { TopBar } from '@/components/TopBar';
import { compact, formatDay } from '@/lib/format';
import { pickPhoto } from '@/lib/image';
import { usePosts } from '@/lib/posts';
import { useSession } from '@/lib/session';
import { colors } from '@/lib/theme';

/** The diary: your posts as chat bubbles, newest at the bottom, with a composer like a messenger. */
export default function Feed() {
  const { me } = useSession();
  const { posts, loading, error, reload, send, retry, notice, clearNotice } = usePosts();
  const [text, setText] = useState('');
  const [photo, setPhoto] = useState<string>();
  const [sending, setSending] = useState(false);
  const list = useRef<FlatList>(null);
  const openedAt = useRef(Date.now());

  useEffect(() => {
    if (!notice) return;
    const t = setTimeout(clearNotice, 5000);
    return () => clearTimeout(t);
  }, [notice, clearNotice]);

  if (!me) return null;
  const settings = me.settings;
  const totalReplies = posts.reduce((n, p) => n + p.replyCount, 0);

  const submit = async () => {
    if ((!text.trim() && !photo) || sending) return;
    setSending(true);
    if (await send(text.trim(), photo)) {
      setText('');
      setPhoto(undefined);
    }
    setSending(false);
  };

  return (
    <SafeAreaView style={styles.screen} edges={['top', 'bottom']}>
      <TopBar>
        <Avatar name={settings.displayName} src={settings.avatar} size={38} />
        <View style={styles.title}>
          <Text style={styles.name} numberOfLines={1}>
            {settings.displayName}
          </Text>
          <Text style={styles.subtitle} numberOfLines={1}>
            {settings.statusText ? `${settings.statusText} · ` : ''}
            {settings.fandom} · {compact(totalReplies)} replies
          </Text>
        </View>
        <Pressable onPress={() => router.push('/settings')} accessibilityLabel="Settings" hitSlop={10}>
          <Text style={styles.gear}>⚙︎</Text>
        </Pressable>
      </TopBar>

      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <FlatList
          ref={list}
          style={styles.flex}
          contentContainerStyle={styles.listContent}
          data={posts}
          keyExtractor={(p) => String(p.id)}
          onContentSizeChange={() => list.current?.scrollToEnd({ animated: true })}
          refreshing={false}
          onRefresh={reload}
          ListHeaderComponent={
            <>
              {!me.aiEnabled && (
                <Text style={styles.banner}>
                  AI fan replies are off because the server has no OpenRouter key. Quick reactions still work.
                </Text>
              )}
              {!!error && <Text style={styles.banner}>{error}</Text>}
            </>
          }
          ListEmptyComponent={
            loading ? (
              <ActivityIndicator style={styles.empty} color={colors.accent} />
            ) : (
              <View style={styles.empty}>
                <Text style={styles.emptyEmoji}>🫧</Text>
                <Text style={styles.emptyText}>Your diary is empty.</Text>
                <Text style={styles.emptyHint}>Write about your day or share a photo. Your fans are waiting!</Text>
              </View>
            )
          }
          renderItem={({ item, index }) => {
            const prev = posts[index - 1];
            const newDay = !prev || new Date(prev.time).toDateString() !== new Date(item.time).toDateString();
            return (
              <>
                {newDay && <Text style={styles.day}>{formatDay(item.time)}</Text>}
                <PostBubble
                  post={item}
                  settings={settings}
                  isNew={item.time > openedAt.current}
                  onOpen={() => router.push({ pathname: '/post/[id]', params: { id: item.id } })}
                  onRetry={() => retry(item.id)}
                />
              </>
            );
          }}
        />

        {!!notice && (
          <Pressable style={styles.toast} onPress={clearNotice}>
            <Text style={styles.toastText}>{notice}</Text>
          </Pressable>
        )}

        <View style={styles.composer}>
          {photo && (
            <View style={styles.preview}>
              <Image source={{ uri: photo }} style={styles.previewImage} contentFit="cover" />
              <Pressable style={styles.removePhoto} onPress={() => setPhoto(undefined)} accessibilityLabel="Remove photo">
                <Text style={styles.removePhotoText}>✕</Text>
              </Pressable>
            </View>
          )}
          <View style={styles.composerRow}>
            <Pressable
              style={styles.attach}
              accessibilityLabel="Add photo"
              onPress={async () => {
                const picked = await pickPhoto().catch(() => undefined);
                if (picked) setPhoto(picked);
              }}
            >
              <Text style={styles.attachText}>＋</Text>
            </Pressable>
            <TextInput
              style={styles.input}
              value={text}
              onChangeText={setText}
              placeholder="Write to your fans…"
              placeholderTextColor={colors.muted}
              multiline
              maxLength={2000}
            />
            <Pressable
              style={[styles.send, (sending || (!text.trim() && !photo)) && styles.sendDisabled]}
              onPress={submit}
              disabled={sending || (!text.trim() && !photo)}
              accessibilityLabel="Send"
            >
              <Text style={styles.sendText}>↑</Text>
            </Pressable>
          </View>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  flex: { flex: 1, backgroundColor: colors.bg },
  title: { flex: 1 },
  name: { fontSize: 16, fontWeight: '700', color: colors.text },
  subtitle: { fontSize: 12, color: colors.muted },
  gear: { fontSize: 22, color: colors.muted },
  listContent: { paddingVertical: 10, flexGrow: 1 },
  banner: {
    marginHorizontal: 14,
    marginBottom: 8,
    padding: 10,
    borderRadius: 12,
    backgroundColor: colors.accentSoft,
    color: colors.text,
    fontSize: 13,
  },
  day: { alignSelf: 'center', fontSize: 12, color: colors.muted, marginVertical: 10 },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 32, gap: 6 },
  emptyEmoji: { fontSize: 48 },
  emptyText: { fontSize: 16, fontWeight: '600', color: colors.text },
  emptyHint: { fontSize: 14, color: colors.muted, textAlign: 'center' },
  toast: { position: 'absolute', left: 14, right: 14, bottom: 80, backgroundColor: colors.text, borderRadius: 12, padding: 12 },
  toastText: { color: '#fff', fontSize: 14 },
  composer: {
    backgroundColor: colors.surface,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.line,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  preview: { marginBottom: 8, alignSelf: 'flex-start' },
  previewImage: { width: 72, height: 72, borderRadius: 12 },
  removePhoto: {
    position: 'absolute',
    top: -6,
    right: -6,
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: colors.text,
    alignItems: 'center',
    justifyContent: 'center',
  },
  removePhotoText: { color: '#fff', fontSize: 12 },
  composerRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 8 },
  attach: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.accentSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  attachText: { fontSize: 20, color: colors.accent },
  input: {
    flex: 1,
    maxHeight: 120,
    minHeight: 36,
    backgroundColor: colors.bg,
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingTop: 8,
    paddingBottom: 8,
    fontSize: 15,
    color: colors.text,
  },
  send: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendDisabled: { opacity: 0.4 },
  sendText: { color: '#fff', fontSize: 18, fontWeight: '700' },
});
