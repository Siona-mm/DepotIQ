import { LogOut } from "lucide-react";
import { useState } from "react";

function initials(username) {
  return (username ?? "User")
    .split(/[._\s-]+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

export default function ProfileMenu({ onSignOut, user }) {
  const [open, setOpen] = useState(false);
  const role = user.roles?.[0]?.replace("ROLE_", "") ?? "User";

  return (
    <div className="profile-menu">
      <button
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label={`Open profile menu for ${user.username}`}
        className="profile-trigger"
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        <span className="avatar">{initials(user.username)}</span>
      </button>
      {open && (
        <div className="profile-popover" role="menu">
          <div className="profile-identity">
            <strong>{user.username}</strong>
            <span>{role}</span>
          </div>
          <button onClick={onSignOut} role="menuitem" type="button">
            <LogOut aria-hidden="true" size={15} />
            Sign out
          </button>
        </div>
      )}
    </div>
  );
}
