import { Image } from 'expo-image';
import { router, useLocalSearchParams } from 'expo-router';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Avatar } from '@/components/Avatar';
import { ReplyRow } from '@/components/ReplyRow';
import { TopBar } from '@/components/TopBar';
import { api } from '@/lib/api';
import { confirm } from '@/lib/confirm';
import { formatDay, formatTime } from '@/lib/format';
import { usePosts } from '@/lib/posts';
import { useSession } from '@/lib/session';
import { colors } from '@/lib/theme';
import type { Reply } from '@/lib/types';

type Sort = 'top' | 'latest';

/** Every reply to one post, like a Bubble message's comment section. */
export default function Replies() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { me } = useSession();
  const { posts, retry, remove } = usePosts();
  const post = posts.find((p) => p.id === Number(id));
  const [sort, setSort] = useState<Sort>('top');
  const [loaded, setLoaded] = useState<Reply[] | null>(null);
  const [loadError, setLoadError] = useState('');
  const [liked, setLiked] = useState<Set<number>>(new Set());

  // Refetch as the count grows, so AI replies appear once they've been written.
  useEffect(() => {
    if (!post) return;
    let live = true;
    api
      .replies(post.id)
      .then((r) => live && (setLoaded(r), setLoadError('')))
      .catch((e) => live && setLoadError(e.message));
    return () => {
      live = false;
    };
  }, [post?.id, post?.replyCount]);

  const replies = useMemo(() => {
    const list = [...(loaded ?? [])];
    if (sort === 'latest') list.sort((a, b) => b.time - a.time);
    // "Top" puts the written replies first so the interesting ones surface.
    else list.sort((a, b) => b.likes + (b.ai ? 1000 : 0) - (a.likes + (a.ai ? 1000 : 0)));
    return list;
  }, [loaded, sort]);

  if (!me || !post) return null;
  const settings = me.settings;

  const deletePost = async () => {
    if (await confirm('Delete this entry?', 'The post and all its replies will be gone.', 'Delete')) {
      await remove(post.id);
      router.back();
    }
  };

  const toggleLike = (replyId: number) =>
    setLiked((s) => {
      const next = new Set(s);
      if (next.has(replyId)) next.delete(replyId);
      else next.add(replyId);
      return next;
    });

  return (
    <SafeAreaView style={styles.screen} edges={['top']}>
      <TopBar>
        <Pressable onPress={() => router.back()} accessibilityLabel="Back" hitSlop={10}>
          <Text style={styles.back}>‹</Text>
        </Pressable>
        <View style={styles.title}>
          <Text style={styles.name}>Fan replies</Text>
          <Text style={styles.subtitle}>{post.replyCount.toLocaleString()} replies</Text>
        </View>
        <Pressable onPress={deletePost} accessibilityLabel="Delete post" hitSlop={10}>
          <Text style={styles.trash}>🗑</Text>
        </Pressable>
      </TopBar>

      <FlatList
        style={styles.list}
        data={replies}
        keyExtractor={(r) => String(r.id)}
        initialNumToRender={20}
        ListHeaderComponent={
          <>
            <View style={styles.original}>
              <View style={styles.originalHead}>
                <Avatar name={settings.displayName} src={settings.avatar} size={40} />
                <View>
                  <Text style={styles.name}>{settings.displayName}</Text>
                  <Text style={styles.subtitle}>
                    {formatDay(post.time)} · {formatTime(post.time)}
                  </Text>
                </View>
              </View>
              {post.imageUrl && (
                <Image source={api.imageSource(post.imageUrl)} style={styles.photo} contentFit="cover" />
              )}
              {!!post.text && <Text style={styles.originalText}>{post.text}</Text>}
            </View>

            <View style={styles.tabs}>
              {(['top', 'latest'] as Sort[]).map((s) => (
                <Pressable key={s} style={[styles.tab, sort === s && styles.tabActive]} onPress={() => setSort(s)}>
                  <Text style={[styles.tabText, sort === s && styles.tabTextActive]}>{s === 'top' ? 'Top' : 'Latest'}</Text>
                </Pressable>
              ))}
            </View>

            {post.status === 'pending' && <Text style={styles.status}>✨ Fans are typing…</Text>}
            {!loaded && !loadError && <ActivityIndicator style={styles.status} color={colors.accent} />}
            {!!loadError && <Text style={[styles.status, styles.error]}>{loadError}</Text>}
            {post.status === 'error' && (
              <Text style={[styles.status, styles.error]}>
                {post.error}{' '}
                <Text style={styles.link} onPress={() => retry(post.id)}>
                  Retry
                </Text>
              </Text>
            )}
          </>
        }
        renderItem={({ item }) => <ReplyRow reply={item} liked={liked.has(item.id)} onLike={() => toggleLike(item.id)} />}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  list: { flex: 1, backgroundColor: colors.bg },
  back: { fontSize: 30, lineHeight: 30, color: colors.text, paddingRight: 4 },
  title: { flex: 1 },
  name: { fontSize: 15, fontWeight: '700', color: colors.text },
  subtitle: { fontSize: 12, color: colors.muted },
  trash: { fontSize: 18 },
  original: { backgroundColor: colors.surface, padding: 16, gap: 12 },
  originalHead: { flexDirection: 'row', gap: 10, alignItems: 'center' },
  photo: { width: '100%', aspectRatio: 1, borderRadius: 14, backgroundColor: colors.accentSoft },
  originalText: { fontSize: 16, lineHeight: 23, color: colors.text },
  tabs: { flexDirection: 'row', gap: 8, paddingHorizontal: 16, paddingVertical: 10 },
  tab: { paddingHorizontal: 14, paddingVertical: 6, borderRadius: 999, backgroundColor: colors.surface },
  tabActive: { backgroundColor: colors.accent },
  tabText: { fontSize: 13, fontWeight: '600', color: colors.muted },
  tabTextActive: { color: '#fff' },
  status: { textAlign: 'center', fontSize: 13, color: colors.muted, paddingVertical: 10 },
  error: { color: colors.danger },
  link: { color: colors.accent, fontWeight: '600' },
});
