function initials(name) {
  return String(name || "User")
    .trim()
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

export default function UserAvatar({ className = "avatar", onClick, profile, user }) {
  const displayName = profile?.displayName || user?.username || "User";
  const content = profile?.avatarData ? (
    <img alt="" src={profile.avatarData} />
  ) : (
    initials(displayName)
  );

  if (!onClick) {
    return <div className={className} title={displayName}>{content}</div>;
  }

  return (
    <button
      aria-label={`Open ${displayName}'s profile`}
      className={className}
      onClick={onClick}
      title={displayName}
      type="button"
    >
      {content}
    </button>
  );
}
