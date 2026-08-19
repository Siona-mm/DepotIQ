import { LockKeyhole, PackageOpen } from "lucide-react";
import { useState } from "react";

export default function LoginView({ onSignIn }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await onSignIn(username, password);
    } catch (requestError) {
      setError(requestError.message || "Sign in failed.");
    } finally {
      setSubmitting(false);
    }
  };

  return <main className="login-page">
    <section className="login-card" aria-labelledby="login-title">
      <div className="login-brand"><PackageOpen size={28} /><span>DepotIQ</span></div>
      <div className="login-heading"><LockKeyhole size={19} /><h1 id="login-title">Sign in</h1><p>Use your assigned DepotIQ account to continue.</p></div>
      {error && <div className="notice error" role="alert">{error}</div>}
      <form onSubmit={submit}>
        <label>Username<input autoComplete="username" onChange={(event) => setUsername(event.target.value)} required value={username} /></label>
        <label>Password<input autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} /></label>
        <button className="save-button" disabled={submitting} type="submit">{submitting ? "Signing in..." : "Sign in"}</button>
      </form>
    </section>
  </main>;
}
