/** Pastel avatar color derived from the username so it's stable across renders. */
export function fanColor(name: string) {
  let h = 0;
  for (const c of name) h = (h * 31 + c.charCodeAt(0)) % 360;
  return `hsl(${h}, 70%, 82%)`;
}
