"use client";

// Used Only If The Root Layout Itself Crashes, So It Must Render Its Own <html>
export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <html lang="en">
      <body style={{ background: "#04050c", color: "#e6e9ff", fontFamily: "system-ui, sans-serif", margin: 0 }}>
        <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", flexDirection: "column", gap: 16 }}>
          <h1 style={{ fontSize: 28, margin: 0 }}>Nexus hit a critical error</h1>
          <p style={{ color: "#8b90b3", margin: 0 }}>Please reload the page.</p>
          <button
            onClick={reset}
            style={{ padding: "10px 22px", borderRadius: 12, border: 0, background: "#22d3ee", color: "#04050c", fontWeight: 700, cursor: "pointer" }}
          >
            Reload
          </button>
        </div>
      </body>
    </html>
  );
}
