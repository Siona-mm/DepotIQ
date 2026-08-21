import { RefreshCw } from "lucide-react";

export default function RetryNotice({ message, onRetry }) {
  return (
    <div className="notice error retry-notice" role="alert">
      <span>{message}</span>
      <button className="secondary-button" onClick={onRetry} type="button">
        <RefreshCw aria-hidden="true" size={13} />
        Retry
      </button>
    </div>
  );
}
