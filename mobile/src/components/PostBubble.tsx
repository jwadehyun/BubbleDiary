import { Image } from 'expo-image';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { api } from '@/lib/api';
import { formatTime, useCountUp } from '@/lib/format';
import { colors, shadow } from '@/lib/theme';
import type { Post, Settings } from '@/lib/types';
import { Avatar } from './Avatar';

interface Props {
  post: Post;
  settings: Settings;
  /** posted during this visit, so the reply count counts up from zero */
  isNew: boolean;
  onOpen: () => void;
  onRetry: () => void;
}

/** One diary entry in the feed, shown like a message from the artist on Bubble. */
export function PostBubble({ post, settings, isNew, onOpen, onRetry }: Props) {
  const count = useCountUp(post.replyCount, isNew);
  return (
    <View style={styles.post}>
      <Avatar name={settings.displayName} src={settings.avatar} size={36} />
      <View style={styles.body}>
        <Text style={styles.name}>{settings.displayName}</Text>
        <View style={styles.row}>
          <Pressable style={styles.bubble} onPress={onOpen} accessibilityRole="button">
            {post.imageUrl && (
              <Image source={api.imageSource(post.imageUrl)} style={styles.photo} contentFit="cover" />
            )}
            {!!post.text && <Text style={styles.text}>{post.text}</Text>}
          </Pressable>
          <Text style={styles.time}>{formatTime(post.time)}</Text>
        </View>
        <View style={styles.meta}>
          <Pressable style={styles.pill} onPress={onOpen} accessibilityLabel={`${post.replyCount} replies`}>
            <Text style={styles.pillText}>💬 {count.toLocaleString()}</Text>
            {post.status === 'pending' && <Text style={styles.typing}>fans typing…</Text>}
          </Pressable>
        </View>
        {post.status === 'error' && (
          <Text style={styles.error}>
            {post.error}{' '}
            <Text style={styles.link} onPress={onRetry}>
              Retry
            </Text>
          </Text>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  post: { flexDirection: 'row', gap: 10, paddingHorizontal: 14, paddingVertical: 8 },
  body: { flex: 1, alignItems: 'flex-start' },
  name: { fontSize: 13, fontWeight: '600', color: colors.text, marginBottom: 4 },
  row: { flexDirection: 'row', alignItems: 'flex-end', gap: 6, maxWidth: '100%' },
  bubble: {
    backgroundColor: colors.surface,
    borderRadius: 18,
    borderTopLeftRadius: 4,
    overflow: 'hidden',
    maxWidth: '85%',
    flexShrink: 1,
    ...shadow,
  },
  photo: { width: 240, height: 240, backgroundColor: colors.accentSoft },
  text: { fontSize: 15, lineHeight: 21, color: colors.text, paddingHorizontal: 13, paddingVertical: 9 },
  time: { fontSize: 11, color: colors.muted },
  meta: { flexDirection: 'row', marginTop: 6 },
  pill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: colors.accentSoft,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  pillText: { fontSize: 13, fontWeight: '600', color: colors.accent },
  typing: { fontSize: 12, color: colors.muted },
  error: { fontSize: 12, color: colors.danger, marginTop: 6 },
  link: { color: colors.accent, fontWeight: '600' },
});
