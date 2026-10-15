import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
  Settings,
  Cpu,
  Database,
  ExternalLink,
  Shield,
  Layers,
  Sliders,
  CheckCircle2,
  AlertTriangle,
  Folder,
  ShieldCheck,
  Lock,
  Activity,
  FileCode,
  Sparkles,
  Server
} from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { user } = useAuth();

  const { data: status, refetch, isFetching } = useQuery({
    queryKey: ['systemStatus'],
    queryFn: () => api.system.getStatus(),
  });

  return (
    <div className="space-y-6 max-w-5xl pb-10">
      <div>
        <div className="flex items-center space-x-2 mb-1">
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
            System Architecture
          </span>
          <span className="text-xs text-slate-500 font-medium">Enterprise Environment & Telemetry</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-blue-950">
          Administration & Settings
        </h1>
        <p className="text-xs sm:text-sm text-slate-600 max-w-xl">
          Inspect local Ollama AI endpoints, vector retrieval tuning, Open Knowledge Format storage directories, and security controls.
        </p>
      </div>

      {/* Ollama Configuration Card */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 space-y-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200">
          <div className="flex items-center space-x-3.5">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-600 shadow-xs">
              <Cpu className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-bold text-blue-950">Local Ollama Neural Engine</h2>
              <p className="text-xs text-slate-500 font-medium">On-premises private neural model inference</p>
            </div>
          </div>

          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs text-blue-950 font-bold flex items-center space-x-1.5 transition-colors self-start sm:self-auto border border-slate-200 disabled:opacity-50"
          >
            <Activity className="w-3.5 h-3.5 text-blue-600" />
            <span>{isFetching ? 'Pinging Ollama...' : 'Refresh Telemetry'}</span>
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 shadow-xs">
            <span className="text-slate-500 uppercase font-bold text-[10px]">Ollama Endpoint</span>
            <p className="font-mono text-blue-950 font-bold">{status?.ollamaBaseUrl || 'http://localhost:11434'}</p>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 shadow-xs">
            <span className="text-slate-500 uppercase font-bold text-[10px]">Neural Status</span>
            <div className="flex items-center space-x-2 pt-0.5">
              <span
                className={`w-2 h-2 rounded-full ${
                  status?.ollamaConnected ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'
                }`}
              />
              <span className="font-bold text-blue-950">
                {status?.ollamaConnected ? 'Active (Port 11434)' : 'Offline / Mock Fallback'}
              </span>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 shadow-xs">
            <span className="text-slate-500 uppercase font-bold text-[10px]">Reasoning Model</span>
            <p className="font-mono text-blue-700 font-bold">{status?.configuredChatModel || 'llama3.2'} (3B)</p>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 shadow-xs">
            <span className="text-slate-500 uppercase font-bold text-[10px]">Embedding Model</span>
            <p className="font-mono text-cyan-800 font-bold">
              {status?.configuredEmbeddingModel || 'nomic-embed-text'} ({status?.embeddingDimensions || 768}D)
            </p>
          </div>
        </div>

        {/* Installed models list from Ollama */}
        {status?.availableOllamaModels && status.availableOllamaModels.length > 0 && (
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
            <span className="text-[11px] font-bold text-blue-950 uppercase tracking-wider">
              Installed Models Detected on Ollama Host:
            </span>
            <div className="flex flex-wrap gap-2">
              {status.availableOllamaModels.map((m: string, idx: number) => (
                <span
                  key={idx}
                  className="px-3 py-1 rounded-xl bg-white text-blue-900 border border-blue-200 text-xs font-mono font-bold flex items-center space-x-1.5 shadow-xs"
                >
                  <Cpu className="w-3 h-3 text-blue-600" />
                  <span>{m}</span>
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* RAG & Ingestion Architecture Parameters */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 space-y-6 shadow-sm">
        <div className="flex items-center space-x-3.5 pb-4 border-b border-slate-200">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-200 flex items-center justify-center text-indigo-600 shadow-xs">
            <Sliders className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-base sm:text-lg font-bold text-blue-950">RAG Hyperparameters & Indexing</h2>
            <p className="text-xs text-slate-500 font-medium">Sliding window chunking and vector index configuration</p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 shadow-xs">
            <span className="text-slate-500 uppercase font-bold text-[10px]">Token Chunk Size</span>
            <p className="font-mono text-blue-950 font-bold text-sm">800 characters</p>
            <p className="text-[11px] text-slate-500">Optimized for semantic coherence</p>
          </div>
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 shadow-xs">
            <span className="text-slate-500 uppercase font-bold text-[10px]">Chunk Sliding Overlap</span>
            <p className="font-mono text-blue-950 font-bold text-sm">150 characters</p>
            <p className="text-[11px] text-slate-500">Preserves boundary context</p>
          </div>
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5 shadow-xs">
            <span className="text-slate-500 uppercase font-bold text-[10px]">Similarity Cutoff</span>
            <p className="font-mono text-emerald-700 font-bold text-sm">0.20 Cosine (Adaptive)</p>
            <p className="text-[11px] text-slate-500">Includes title & keyword boost</p>
          </div>
        </div>
      </div>

      {/* Security, Storage & Compliance */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 space-y-6 shadow-sm">
        <div className="flex items-center space-x-3.5 pb-4 border-b border-slate-200">
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center text-emerald-600 shadow-xs">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-base sm:text-lg font-bold text-blue-950">Security & Tenant Privacy Controls</h2>
            <p className="text-xs text-slate-500 font-medium">Role-based access, prompt shielding, and data isolation</p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2 shadow-xs">
            <div className="flex items-center space-x-2 text-blue-950 font-bold">
              <Lock className="w-4 h-4 text-blue-600" />
              <span>Stateless JWT Authentication</span>
            </div>
            <p className="text-slate-600 leading-relaxed text-[11px]">
              Tokens signed with HMAC-SHA512 with 24-hour expiration. Passwords hashed using BCrypt.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2 shadow-xs">
            <div className="flex items-center space-x-2 text-blue-950 font-bold">
              <Shield className="w-4 h-4 text-emerald-600" />
              <span>Anti-Injection Guardrails</span>
            </div>
            <p className="text-slate-600 leading-relaxed text-[11px]">
              Sanitizes retrieved text chunks against prompt override sequences and jailbreak patterns.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
