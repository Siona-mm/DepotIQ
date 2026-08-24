import NotificationCenter from "./NotificationCenter.jsx";
import UserAvatar from "./UserAvatar.jsx";

export default function HeaderAccountControls({
  onNavigate,
  profile,
  user,
}) {
  const displayName = profile?.displayName || user?.username || "User";

  return (
    <div
      className="header-account-controls"
      style={{
        alignItems: "center",
        display: "flex",
        gap: "12px",
        justifyContent: "flex-end",
      }}
    >
      <NotificationCenter
        onNavigate={onNavigate}
        profile={profile}
        user={user}
      />
      <span className="header-account-name">{displayName}</span>
      <UserAvatar
        onClick={() => onNavigate("Profile")}
        profile={profile}
        user={user}
      />
    </div>
  );
}
