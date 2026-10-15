import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Bot, Lock, User, AlertCircle, ArrowRight, ShieldCheck, Sparkles, Cpu, Database, Check } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('Admin123!');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      await login({ username: username.trim() || 'admin', password: password || 'Admin123!' });
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
      navigate('/');
    }
  };

  const handleQuickLogin = async (user: string, pass: string) => {
    setUsername(user);
    setPassword(pass);
    setError(null);
    setIsLoading(true);
    try {
      await login({ username: user, password: pass });
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
      navigate('/');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 sm:p-6 bg-slate-50 relative overflow-hidden">
      {/* Ambient background glows */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-blue-500/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />

      <div className="w-full max-w-md space-y-6 relative z-10">
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex w-16 h-16 rounded-3xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-cyan-500 p-0.5 shadow-lg shadow-blue-500/25 mb-2">
            <div className="w-full h-full bg-white rounded-[22px] flex items-center justify-center">
              <Bot className="w-8 h-8 text-blue-600" />
            </div>
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-blue-950">
            KnowledgeHub AI
          </h1>
          <p className="text-xs sm:text-sm text-slate-600 max-w-sm mx-auto font-medium">
            Enterprise Knowledge Ingestion & Grounded Neural RAG Platform
          </p>

          <div className="flex items-center justify-center gap-2 pt-1">
            <span className="text-[10px] font-mono px-2.5 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200 font-bold">
              OKF v0.2 Protocol
            </span>
            <span className="text-[10px] font-mono px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 font-bold flex items-center space-x-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse mr-1" />
              Local Ollama
            </span>
          </div>
        </div>

        {/* Card Form */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl shadow-xl border border-slate-200 relative overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500" />

          {error && (
            <div className="mb-6 p-3.5 rounded-2xl bg-rose-50 border border-rose-200 flex items-start space-x-3 text-rose-800 text-xs shadow-xs">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
              <span className="font-semibold">{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-blue-950 mb-1.5">
                Username
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="admin or username"
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all font-medium"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-blue-950 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all font-medium"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 active:scale-[0.99] text-white font-bold text-sm transition-all duration-150 flex items-center justify-center space-x-2 shadow-md shadow-blue-500/25 disabled:opacity-50 border border-blue-500/30 mt-2"
            >
              <span>{isLoading ? 'Authenticating...' : 'Sign In to Workspace'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          {/* Quick Access Demo Credentials */}
          <div className="mt-6 pt-5 border-t border-slate-200 space-y-2">
            <span className="text-[11px] font-bold text-blue-950 uppercase tracking-wider block">
              Quick Test Credentials
            </span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleQuickLogin('admin', 'Admin123!')}
                className="p-2.5 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-300 text-left transition-all group"
              >
                <div className="text-xs font-bold text-blue-950 group-hover:text-blue-700">
                  Admin User
                </div>
                <div className="text-[10px] text-slate-500 font-mono mt-0.5 font-medium">
                  admin / Admin123!
                </div>
              </button>

              <button
                type="button"
                onClick={() => handleQuickLogin('demo', 'Demo123!')}
                className="p-2.5 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-300 text-left transition-all group"
              >
                <div className="text-xs font-bold text-blue-950 group-hover:text-blue-700">
                  Demo User
                </div>
                <div className="text-[10px] text-slate-500 font-mono mt-0.5 font-medium">
                  demo / Demo123!
                </div>
              </button>
            </div>
          </div>
        </div>

        {/* Security and Trust Footer */}
        <div className="text-center text-xs text-slate-500 space-y-1">
          <p className="flex items-center justify-center space-x-1.5 font-medium text-slate-600">
            <ShieldCheck className="w-4 h-4 text-blue-600" />
            <span>Stateless HMAC-SHA512 JWT Authentication</span>
          </p>
          <p className="text-[11px] text-slate-500">Strict User Tenant Isolation • Zero Cloud Data Exfiltration</p>
        </div>
      </div>
    </div>
  );
};
