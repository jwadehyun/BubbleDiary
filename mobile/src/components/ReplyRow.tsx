import { Pressable, StyleSheet, Text, View } from 'react-native';
import { timeAgo } from '@/lib/format';
import { colors } from '@/lib/theme';
import type { Reply } from '@/lib/types';
import { Avatar } from './Avatar';

export function ReplyRow({ reply, liked, onLike }: { reply: Reply; liked: boolean; onLike: () => void }) {
  const likes = reply.likes + (liked ? 1 : 0);
  return (
    <View style={styles.reply}>
      <Avatar name={reply.fan} size={34} />
      <View style={styles.body}>
        <View style={styles.head}>
          <Text style={styles.fan} numberOfLines={1}>
            {reply.fan}
          </Text>
          <Text style={styles.ago}>{timeAgo(reply.time)}</Text>
        </View>
        <Text style={styles.text}>{reply.text}</Text>
      </View>
      <Pressable style={styles.like} onPress={onLike} accessibilityLabel={liked ? 'Unlike' : 'Like'} hitSlop={8}>
        <Text style={[styles.heart, liked && styles.liked]}>{liked ? '♥' : '♡'}</Text>
        {likes > 0 && <Text style={styles.count}>{likes}</Text>}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  reply: {
    flexDirection: 'row',
    gap: 10,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.line,
    backgroundColor: colors.surface,
  },
  body: { flex: 1 },
  head: { flexDirection: 'row', alignItems: 'baseline', gap: 6 },
  fan: { fontSize: 13, fontWeight: '600', color: colors.text, flexShrink: 1 },
  ago: { fontSize: 11, color: colors.muted },
  text: { fontSize: 14, lineHeight: 20, color: colors.text, marginTop: 2 },
  like: { alignItems: 'center', minWidth: 28 },
  heart: { fontSize: 18, color: colors.muted },
  liked: { color: colors.accent2 },
  count: { fontSize: 11, color: colors.muted },
});
