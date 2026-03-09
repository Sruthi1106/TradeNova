import { Link } from 'react-router-dom'

const LandingPage = () => {
  return (
    <div className="min-h-screen bg-black text-white selection:bg-accent/30 selection:text-white overflow-x-hidden relative">
      <div className="fixed inset-0 pointer-events-none">
        <div
          className="absolute top-0 left-1/4 w-96 h-96 bg-accent/20 rounded-full blur-[120px] animate-pulse"
          style={{ animationDuration: '4s' }}
        />
        <div
          className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-[120px] animate-pulse"
          style={{ animationDuration: '6s', animationDelay: '1s' }}
        />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-accent/5 rounded-full blur-[150px]" />
      </div>

      <nav className="fixed w-full z-50 top-0 left-0 border-b border-accent/10 bg-black/80 backdrop-blur-xl">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2 group">
            <span className="font-bold text-2xl tracking-tight text-accent transition-all duration-300 group-hover:scale-105">
              TradeNova
            </span>
          </div>

          <div className="flex items-center gap-4">
            <Link to="/login">
              <button className="bg-accent/10 hover:bg-accent hover:text-black text-sm px-6 py-2 rounded-full border border-accent/30 hover:border-accent transition-all duration-300 font-medium hover:shadow-lg hover:shadow-accent/20">
                Log in
              </button>
            </Link>
            <Link to="/register">
              <button className="bg-accent/10 hover:bg-accent hover:text-black text-sm px-6 py-2 rounded-full border border-accent/30 hover:border-accent transition-all duration-300 font-medium hover:shadow-lg hover:shadow-accent/20">
                Sign up
              </button>
            </Link>
          </div>
        </div>
      </nav>

      <main className="relative">
        <section className="relative min-h-screen flex flex-col justify-center items-center px-6 overflow-hidden pt-16">
          <div className="max-w-4xl mx-auto text-center relative z-10 animate-in fade-in slide-in-from-bottom-4 duration-1000">
            <h1 className="text-5xl md:text-7xl font-bold tracking-tighter mb-6 bg-gradient-to-b from-white via-white to-accent bg-clip-text text-transparent animate-in fade-in slide-in-from-bottom-6 duration-1000 delay-100">
              Precision Trading. <br />
              Limitless Potential.
            </h1>

            <p className="text-lg md:text-xl text-white/50 mb-12 leading-relaxed max-w-2xl mx-auto font-light animate-in fade-in slide-in-from-bottom-8 duration-1000 delay-200">
              Execute trades with microsecond latency. <br />
              Institutional-grade liquidity on a fully decentralized protocol.
            </p>

            <div className="flex flex-col sm:flex-row items-center justify-center gap-4 animate-in fade-in slide-in-from-bottom-10 duration-1000 delay-300">
              <Link to="/register">
                <button className="px-8 py-3 bg-accent text-black text-sm font-semibold rounded-full hover:bg-accent/90 transition-all duration-300 hover:scale-105 hover:shadow-xl hover:shadow-accent/30">
                  Start Trading
                </button>
              </Link>
              <Link to="/login">
                <button className="px-8 py-3 bg-transparent border-2 border-accent/30 text-accent text-sm font-semibold rounded-full hover:bg-accent/10 hover:border-accent transition-all duration-300 backdrop-blur-sm">
                  View Markets
                </button>
              </Link>
            </div>
          </div>

          <div
            className="absolute top-1/4 left-10 w-2 h-2 bg-accent rounded-full animate-pulse"
            style={{ animationDuration: '3s' }}
          />
          <div
            className="absolute top-1/3 right-20 w-3 h-3 bg-blue-400 rounded-full animate-pulse"
            style={{ animationDuration: '4s', animationDelay: '1s' }}
          />
          <div
            className="absolute bottom-1/3 left-1/4 w-2 h-2 bg-accent/60 rounded-full animate-pulse"
            style={{ animationDuration: '5s', animationDelay: '2s' }}
          />
        </section>

        <footer className="py-12 border-t border-accent/10 text-center bg-black">
          <p className="text-white/40 text-sm">
            &copy; 2026 <span className="text-accent">TradeNova</span>. All rights reserved.
          </p>
        </footer>
      </main>
    </div>
  )
}

export default LandingPage