import { Camera, Save, UserRound } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { loadProfile, updateProfile } from "../api/depotiqApi.js";
import AppSidebar from "../components/AppSidebar.jsx";

const EMPTY_PROFILE = { displayName: "", email: "", jobTitle: "", avatarData: "" };

function initials(name, username) {
  return (name || username || "U").split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase();
}

export default function ProfileView({ collapsed, onAction, onCollapse, onNavigate, onSignOut, user }) {
  const [profile, setProfile] = useState(EMPTY_PROFILE);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const fileInput = useRef(null);

  useEffect(() => {
    loadProfile().then(setProfile).catch((requestError) => setError(requestError.message)).finally(() => setLoading(false));
  }, []);

  const updateField = (key, value) => setProfile((current) => ({ ...current, [key]: value }));
  const choosePhoto = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) { setError("Choose an image file for your profile photo."); return; }
    if (file.size > 1_000_000) { setError("Use an image smaller than 1 MB."); return; }
    const reader = new FileReader();
    reader.onload = () => updateField("avatarData", String(reader.result));
    reader.readAsDataURL(file);
  };
  const save = async (event) => {
    event.preventDefault(); setSaving(true); setMessage(""); setError("");
    try { setProfile(await updateProfile(profile)); setMessage("Profile saved."); }
    catch (requestError) { setError(requestError.message); }
    finally { setSaving(false); }
  };

  return <div className={collapsed ? "app-shell sidebar-collapsed" : "app-shell"}>
    <AppSidebar activePage="Profile" collapsed={collapsed} onAction={onAction} onCollapse={onCollapse} onNavigate={onNavigate} onSignOut={onSignOut} user={user} />
    <main className="dashboard profile-page">
      <header className="topbar"><h1>Profile</h1><div /><div className="avatar">{initials(profile.displayName, user.username)}</div></header>
      <div className="page-heading"><div><span>Account settings</span><h2>Your DepotIQ profile</h2></div></div>
      {(message || error) && <div className={error ? "notice error" : "notice"} role="status">{error || message}</div>}
      <section className="profile-layout">
        <aside className="profile-summary"><div className="profile-photo">{profile.avatarData ? <img alt="Your profile" src={profile.avatarData} /> : <span>{initials(profile.displayName, user.username)}</span>}<button aria-label="Choose profile photo" className="photo-button" onClick={() => fileInput.current?.click()} type="button"><Camera size={15} /></button></div><strong>{profile.displayName || user.username}</strong><span>{profile.jobTitle || "DepotIQ team member"}</span><button className="secondary-button" onClick={() => fileInput.current?.click()} type="button">Change photo</button><input accept="image/png,image/jpeg,image/webp" className="sr-only" onChange={choosePhoto} ref={fileInput} type="file" /></aside>
        <form className="profile-form" onSubmit={save}><header><UserRound size={18} /><div><h2>Profile details</h2><p>These details are visible across your local DepotIQ workspace.</p></div></header><div className="profile-fields"><label>Display name<input disabled={loading} onChange={(event) => updateField("displayName", event.target.value)} required value={profile.displayName ?? ""} /></label><label>Email address<input disabled={loading} onChange={(event) => updateField("email", event.target.value)} type="email" value={profile.email ?? ""} /></label><label>Job title<input disabled={loading} onChange={(event) => updateField("jobTitle", event.target.value)} value={profile.jobTitle ?? ""} /></label><label>Username<input disabled value={user.username} /></label></div><footer><button className="primary-button" disabled={loading || saving} type="submit"><Save size={16} />{saving ? "Saving..." : "Save profile"}</button></footer></form>
      </section>
    </main>
  </div>;
}
