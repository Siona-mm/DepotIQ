import NotificationCenter from "./NotificationCenter.jsx";
import UserAvatar from "./UserAvatar.jsx";

export default function HeaderAccountControls({ onNavigate, onSignOut, profile, user }) {
  return (
    <div className="header-account-controls">
      <NotificationCenter onNavigate={onNavigate} user={user} />
      <UserAvatar onClick={() => onNavigate("Profile")} onSignOut={onSignOut} profile={profile} user={user} />
    </div>
  );
}
