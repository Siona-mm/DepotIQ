import { AlertTriangle, RefreshCw } from "lucide-react";
import { Component } from "react";

export default class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  handleRetry = () => {
    this.setState({ hasError: false });
    globalThis.location.reload();
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <main className="app-recovery" role="alert">
        <section>
          <AlertTriangle aria-hidden="true" size={28} />
          <span>Something needs attention</span>
          <h1>We could not display this page.</h1>
          <p>Your data has not been changed. Reload the application and try again.</p>
          <button className="primary-button" onClick={this.handleRetry} type="button">
            <RefreshCw aria-hidden="true" size={16} />
            Try again
          </button>
        </section>
      </main>
    );
  }
}
