import { fanColor } from '../lib/fans';

export function Avatar({ name, src, size = 36 }: { name: string; src?: string | null; size?: number }) {
  const style = { width: size, height: size, fontSize: size * 0.42 };
  // no-referrer: Google profile photos refuse some cross-site referrers.
  if (src) return <img className="avatar" src={src} alt="" style={style} referrerPolicy="no-referrer" />;
  return (
    <div className="avatar" style={{ ...style, background: fanColor(name) }}>
      {[...name][0]?.toUpperCase() ?? '?'}
    </div>
  );
}
