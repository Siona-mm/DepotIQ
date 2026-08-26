import { LogOut, UserRound, X } from "lucide-react";
import { useEffect, useId, useRef, useState } from "react";

function initials(name) {
  return String(name || "User")
    .trim()
    .split(/\s+/)
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

export default function UserAvatar({ className = "avatar", onClick, onSignOut, profile, user }) {
  const [open, setOpen] = useState(false);
  const [confirmingSignOut, setConfirmingSignOut] = useState(false);
  const menuRef = useRef(null);
  const menuId = useId();
  const displayName = profile?.displayName || user?.username || "User";
  const role = user?.roles?.[0]?.replace("ROLE_", "") || "USER";
  const content = profile?.avatarData ? (
    <img alt="" src={profile.avatarData} />
  ) : (
    initials(displayName)
  );

  useEffect(() => {
    const closeFromOutside = (event) => {
      if (!menuRef.current?.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener("pointerdown", closeFromOutside);
    return () => document.removeEventListener("pointerdown", closeFromOutside);
  }, []);

  useEffect(() => {
    const closeFromEscape = (event) => {
      if (event.key !== "Escape") return;
      setOpen(false);
      setConfirmingSignOut(false);
    };

    document.addEventListener("keydown", closeFromEscape);
    return () => document.removeEventListener("keydown", closeFromEscape);
  }, []);

  if (!onClick && !onSignOut) {
    return <div className={className} title={displayName}>{content}</div>;
  }

  return (
    <div className="profile-control" ref={menuRef}>
      <span className="profile-name">{displayName}</span>
      <button
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        aria-haspopup="menu"
        aria-label={`Open ${displayName}'s account menu`}
        className={className}
        onClick={() => setOpen((current) => !current)}
        title={displayName}
        type="button"
      >
        {content}
      </button>

      {open && (
        <div className="account-popover" id={menuId} role="menu">
          <div className="account-identity">
            <strong>{displayName}</strong>
            <span>{role}</span>
          </div>
          <button
            disabled={!onClick}
            onClick={() => {
              setOpen(false);
              onClick?.();
            }}
            role="menuitem"
            type="button"
          >
            <UserRound aria-hidden="true" size={15} />
            Profile details
          </button>
          <button
            className="account-signout"
            disabled={!onSignOut}
            onClick={() => {
              setOpen(false);
              setConfirmingSignOut(true);
            }}
            role="menuitem"
            type="button"
          >
            <LogOut aria-hidden="true" size={15} />
            Sign out
          </button>
        </div>
      )}

      {confirmingSignOut && (
        <div className="modal-backdrop" onClick={(event) => {
          if (event.target === event.currentTarget) setConfirmingSignOut(false);
        }}>
          <section aria-labelledby="sign-out-title" aria-modal="true" className="override-dialog confirmation-dialog signout-dialog" role="dialog">
            <header>
              <div>
                <span>Account action</span>
                <h2 id="sign-out-title">Sign out?</h2>
              </div>
              <button aria-label="Close sign out confirmation" className="icon-button" onClick={() => setConfirmingSignOut(false)} type="button">
                <X aria-hidden="true" size={17} />
              </button>
            </header>
            <p>Are you sure you want to sign out of DepotIQ?</p>
            <footer>
              <button className="secondary-button" onClick={() => setConfirmingSignOut(false)} type="button">Cancel</button>
              <button className="reject-confirm-button" onClick={onSignOut} type="button">
                <LogOut aria-hidden="true" size={14} />
                Sign out
              </button>
            </footer>
          </section>
        </div>
      )}
    </div>
  );
}
