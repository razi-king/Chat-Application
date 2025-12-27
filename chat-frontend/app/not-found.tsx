import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-black to-gray-900 p-4">
      <main className="max-w-4xl w-full space-y-10">
        {/* Hero Section: 404 Error with Tech Flair */}
        <section className="text-center space-y-6">
          <h1 className="text-8xl md:text-9xl font-black tracking-tighter">
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-500">
              404
            </span>
          </h1>
          <p className="text-4xl md:text-5xl font-bold text-gray-100">
            Missing Resource
          </p>
          <div className="max-w-2xl mx-auto">
            <p className="text-xl text-gray-400 leading-relaxed">
              The requested endpoint <code className="px-2 py-1 mx-1 bg-gray-800 rounded text-cyan-300">could not be resolved</code>.
              This path does not exist or has been deprecated.
            </p>
            <p className="mt-8 text-lg text-gray-500 italic">
              Oops! This Page Is Currently Under Development By The Full Stack Developer Razi.
            </p>
          </div>
        </section>

        {/* Suggested Actions: Mimics a Developer's Dashboard */}
        <section className="bg-gray-800/50 backdrop-blur-sm border border-gray-700 rounded-2xl p-8">
          <h2 className="text-2xl font-semibold text-gray-200 mb-6 text-center">
            Recommended Actions
          </h2>
          <div className="grid md:grid-cols-3 gap-6">
            <Link
              href="/"
              className="group p-6 bg-gradient-to-br from-gray-800 to-gray-900 border border-gray-700 rounded-xl hover:border-cyan-500/50 transition-all duration-300 text-center"
            >
              <div className="text-cyan-400 text-3xl mb-3">⌂</div>
              <h3 className="font-bold text-gray-100 mb-2">Go Home</h3>
              <p className="text-sm text-gray-400">Return to the application root</p>
            </Link>
            <div className="group p-6 bg-gradient-to-br from-gray-800 to-gray-900 border border-gray-700 rounded-xl text-center">
              <div className="text-gray-400 text-3xl mb-3">⌨️</div>
              <h3 className="font-bold text-gray-100 mb-2">Explore Docs</h3>
              <p className="text-sm text-gray-400">Review API or project documentation</p>
            </div>
            <a
              href="mailto:contact@example.com"
              className="group p-6 bg-gradient-to-br from-gray-800 to-gray-900 border border-gray-700 rounded-xl hover:border-blue-500/50 transition-all duration-300 text-center"
            >
              <div className="text-blue-400 text-3xl mb-3">✉️</div>
              <h3 className="font-bold text-gray-100 mb-2">Report Issue</h3>
              <p className="text-sm text-gray-400">File a ticket or send feedback</p>
            </a>
          </div>
        </section>

        {/* Status & Debug Info (Optional) */}
        <section className="text-center">
          <div className="inline-flex items-center px-4 py-2 rounded-full bg-gray-800 border border-gray-700">
            <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse mr-2"></div>
            <span className="text-sm font-mono text-gray-300">
              Server Status: <span className="text-green-400">Operational</span>
            </span>
          </div>
          <p className="mt-6 text-gray-500 text-sm">
            <span className="font-mono">Error Code: 404_NOT_FOUND</span>
            <span className="mx-3">•</span>
            <span>Timestamp: {new Date().toISOString().split('T')[0]}</span>
          </p>
        </section>
      </main>
    </div>
  );
}