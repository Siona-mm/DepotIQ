import {
  Camera,
  KeyRound,
  Save,
  Settings,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { useRef, useState } from "react";
import { updateCredentials, updateProfile } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";
import HeaderAccountControls from "../components/HeaderAccountControls.jsx";

const EMPTY_PROFILE = {
  displayName: "",
  email: "",
  jobTitle: "",
  avatarData: "",
};

function initials(name, username) {
  return (name || username || "U")
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

export default function ProfileView({
  collapsed,
  onAction,
  onCollapse,
  onNavigate,
  onProfileUpdated,
  onSignOut,
  onUserUpdated,
  permissions,
  profile: initialProfile,
  user,
}) {
  const [profile, setProfile] = useState(initialProfile ?? EMPTY_PROFILE);
  const [credentials, setCredentials] = useState({
    username: user.username,
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const loading = !initialProfile;
  const [saving, setSaving] = useState(false);
  const [savingCredentials, setSavingCredentials] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const fileInput = useRef(null);

  const updateField = (key, value) =>
    setProfile((current) => ({ ...current, [key]: value }));
  const updateCredentialField = (key, value) =>
    setCredentials((current) => ({ ...current, [key]: value }));

  const choosePhoto = (event) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    if (!file.type.startsWith("image/")) {
      setError("Choose an image file for your profile photo.");
      return;
    }
    if (file.size > 1_000_000) {
      setError("Use an image smaller than 1 MB.");
      return;
    }

    const reader = new globalThis.FileReader();
    reader.onload = () => updateField("avatarData", String(reader.result));
    reader.readAsDataURL(file);
  };

  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage("");
    setError("");

    try {
      const saved = await updateProfile(profile);
      setProfile(saved);
      onProfileUpdated(saved);
      setMessage("Profile saved.");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  };

  const saveCredentials = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (
      credentials.newPassword &&
      credentials.newPassword !== credentials.confirmPassword
    ) {
      setError("New password and confirmation do not match.");
      return;
    }

    setSavingCredentials(true);
    try {
      const updatedUser = await updateCredentials({
        username: credentials.username.trim(),
        currentPassword: credentials.currentPassword,
        newPassword: credentials.newPassword,
      });
      onUserUpdated(updatedUser);
      const updatedProfile = { ...profile, username: updatedUser.username };
      setProfile(updatedProfile);
      onProfileUpdated(updatedProfile);
      setCredentials((current) => ({
        ...current,
        username: updatedUser.username,
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      }));
      setMessage("Login credentials updated.");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSavingCredentials(false);
    }
  };

  return (
    <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
      <AppSidebar
        activePage="Profile"
        collapsed={collapsed}
        onAction={onAction}
        onCollapse={onCollapse}
        onNavigate={onNavigate}
        onSignOut={onSignOut}
        permissions={permissions}
        profile={profile}
        user={user}
      />
      <main className="dashboard profile-page">
        <header className="topbar">
          <h1>Profile</h1>
          <div />
          <HeaderAccountControls onNavigate={onNavigate} onSignOut={onSignOut} profile={profile} user={user} />
        </header>

        <div className="page-heading">
          <div>
            <span>Account settings</span>
            <h2>Your DepotIQ profile</h2>
          </div>
        </div>

        {(message || error) && (
          <div className={error ? "notice error" : "notice"} role="status">
            {error || message}
          </div>
        )}

        <section className="profile-layout">
          <aside className="profile-summary">
            <div className="profile-photo">
              {profile.avatarData ? (
                <img alt="Your profile" src={profile.avatarData} />
              ) : (
                <span>{initials(profile.displayName, user.username)}</span>
              )}
              <button
                aria-label="Choose profile photo"
                className="photo-button"
                onClick={() => fileInput.current?.click()}
                type="button"
              >
                <Camera aria-hidden="true" size={15} />
              </button>
            </div>
            <strong>{profile.displayName || user.username}</strong>
            <span>{profile.jobTitle || "DepotIQ team member"}</span>
            <button
              className="secondary-button profile-photo-action"
              onClick={() => fileInput.current?.click()}
              type="button"
            >
              Change photo
            </button>
            <input
              accept="image/png,image/jpeg,image/webp"
              className="sr-only"
              onChange={choosePhoto}
              ref={fileInput}
              type="file"
            />
          </aside>

          <div className="profile-content">
            <form className="profile-form" onSubmit={save}>
              <header>
                <UserRound aria-hidden="true" size={18} />
                <div>
                  <h2>Profile details</h2>
                  <p>These details are visible across your DepotIQ workspace.</p>
                </div>
              </header>
              <div className="profile-fields">
                <label>
                  Display name
                  <input
                    disabled={loading}
                    onChange={(event) =>
                      updateField("displayName", event.target.value)
                    }
                    required
                    value={profile.displayName ?? ""}
                  />
                </label>
                <label>
                  Email address
                  <input
                    disabled={loading}
                    onChange={(event) => updateField("email", event.target.value)}
                    type="email"
                    value={profile.email ?? ""}
                  />
                </label>
                <label>
                  Job title
                  <input
                    disabled={loading}
                    onChange={(event) =>
                      updateField("jobTitle", event.target.value)
                    }
                    value={profile.jobTitle ?? ""}
                  />
                </label>
              </div>
              <footer>
                <button
                  className="primary-button"
                  disabled={loading || saving}
                  type="submit"
                >
                  <Save aria-hidden="true" size={16} />
                  {saving ? "Saving..." : "Save profile"}
                </button>
              </footer>
            </form>

            <form
              className="profile-form credentials-form"
              onSubmit={saveCredentials}
            >
              <header>
                <KeyRound aria-hidden="true" size={18} />
                <div>
                  <h2>Login credentials</h2>
                  <p>Change your username or set a new account password.</p>
                </div>
              </header>
              <div className="profile-fields credentials-fields">
                <label>
                  Username
                  <input
                    autoComplete="username"
                    minLength={3}
                    onChange={(event) =>
                      updateCredentialField("username", event.target.value)
                    }
                    pattern="[A-Za-z0-9._-]+"
                    required
                    value={credentials.username}
                  />
                </label>
                <label>
                  Current password
                  <input
                    autoComplete="current-password"
                    maxLength={72}
                    onChange={(event) =>
                      updateCredentialField("currentPassword", event.target.value)
                    }
                    required
                    type="password"
                    value={credentials.currentPassword}
                  />
                </label>
                <label>
                  New password
                  <input
                    autoComplete="new-password"
                    maxLength={72}
                    minLength={8}
                    onChange={(event) =>
                      updateCredentialField("newPassword", event.target.value)
                    }
                    placeholder="Leave blank to keep current password"
                    type="password"
                    value={credentials.newPassword}
                  />
                </label>
                <label>
                  Confirm new password
                  <input
                    autoComplete="new-password"
                    disabled={!credentials.newPassword}
                    maxLength={72}
                    minLength={8}
                    onChange={(event) =>
                      updateCredentialField("confirmPassword", event.target.value)
                    }
                    required={Boolean(credentials.newPassword)}
                    type="password"
                    value={credentials.confirmPassword}
                  />
                </label>
              </div>
              <footer>
                <button
                  className="primary-button"
                  disabled={savingCredentials}
                  type="submit"
                >
                  <KeyRound aria-hidden="true" size={15} />
                  {savingCredentials ? "Updating..." : "Update login"}
                </button>
              </footer>
            </form>

            <section className="profile-account-card">
              <header>
                <ShieldCheck aria-hidden="true" size={18} />
                <div>
                  <h2>Account and workspace</h2>
                  <p>Manage your access and operating preferences.</p>
                </div>
              </header>
              <div className="profile-account-row">
                <span>Access level</span>
                <strong>
                  {user.roles?.[0]?.replace("ROLE_", "")?.toLowerCase() ||
                    "user"}
                </strong>
              </div>
              <div className="profile-account-actions">
                <button
                  className="secondary-button"
                  onClick={() => onNavigate("Settings")}
                  type="button"
                >
                  <Settings aria-hidden="true" size={15} />
                  Workspace settings
                </button>
              </div>
            </section>
          </div>
        </section>
      </main>
    </div>
  );
}
