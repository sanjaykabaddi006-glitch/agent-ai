import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { DocumentItem, ChatSession } from '../types';
import {
  FileText,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Bot,
  MessageSquare,
  ArrowRight,
  Upload,
  BookOpen,
  Cpu,
  Layers,
  Sparkles,
  Database,
  ShieldCheck,
  Zap,
  Activity,
  Check,
  ExternalLink,
  RefreshCw,
  History,
  HardDrive
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { data: status, isLoading: statusLoading } = useQuery({
    queryKey: ['systemStatus'],
    queryFn: () => api.system.getStatus(),
    refetchInterval: 10000,
  });

  const { data: docPage, isLoading: docsLoading } = useQuery({
    queryKey: ['recentDocuments'],
    queryFn: () => api.documents.list('', 0, 6),
  });

  const { data: sessions, isLoading: sessionsLoading } = useQuery({
    queryKey: ['recentSessions'],
    queryFn: () => api.chat.listSessions(),
  });

  // Professional blue KPI stats
  const stats = [
    {
      title: 'Knowledge Documents',
      value: status?.totalDocuments ?? 0,
      subtext: `${status?.indexedDocuments ?? 0} indexed & searchable`,
      icon: FileText,
      color: 'text-blue-600',
      bgColor: 'bg-blue-50',
      borderColor: 'border-blue-200',
      badge: 'OKF v0.2',
      badgeStyle: 'bg-blue-50 text-blue-700 border-blue-200',
    },
    {
      title: 'Vector Chunks',
      value: status?.totalChunks ?? 0,
      subtext: '768-D Cosine Embeddings',
      icon: Layers,
      color: 'text-cyan-700',
      bgColor: 'bg-cyan-50',
      borderColor: 'border-cyan-200',
      badge: 'nomic-embed',
      badgeStyle: 'bg-cyan-50 text-cyan-800 border-cyan-200',
    },
    {
      title: 'Neural Chat Sessions',
      value: status?.totalChatSessions ?? (sessions?.length ?? 0),
      subtext: 'Isolated per user tenant',
      icon: MessageSquare,
      color: 'text-emerald-700',
      bgColor: 'bg-emerald-50',
      borderColor: 'border-emerald-200',
      badge: 'Stateless',
      badgeStyle: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    },
    {
      title: 'Ollama Engine',
      value: status?.ollamaConnected ? 'Online' : 'Offline',
      subtext: status?.configuredChatModel ? `${status.configuredChatModel} (3B)` : 'llama3.2',
      icon: Cpu,
      color: status?.ollamaConnected ? 'text-indigo-600' : 'text-amber-600',
      bgColor: status?.ollamaConnected ? 'bg-indigo-50' : 'bg-amber-50',
      borderColor: status?.ollamaConnected ? 'border-indigo-200' : 'border-amber-200',
      badge: ':11434',
      badgeStyle: status?.ollamaConnected
        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
        : 'bg-amber-50 text-amber-700 border-amber-200',
    },
  ];

  return (
    <div className="space-y-8 pb-10">
      {/* 1. Hero Welcome Banner: Professional White Card with Blue Accents */}
      <div className="relative overflow-hidden p-6 sm:p-8 rounded-3xl bg-gradient-to-r from-blue-50/90 via-indigo-50/40 to-white border border-blue-200/90 shadow-sm">
        {/* Subtle decorative glow */}
        <div className="absolute top-0 right-0 -mt-12 -mr-12 w-96 h-96 bg-blue-400/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-1/3 -mb-16 w-80 h-80 bg-indigo-400/5 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-3 max-w-3xl">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-3 py-1 rounded-full text-xs font-bold bg-white text-blue-700 border border-blue-200 shadow-xs flex items-center space-x-1.5">
                <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                <span>Enterprise Knowledge Architecture</span>
              </span>

              <span className="px-3 py-1 rounded-full text-xs font-mono font-semibold bg-white text-emerald-700 border border-emerald-200 shadow-xs flex items-center space-x-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                <span>Zero Data Leakage • Local Ollama RAG</span>
              </span>
            </div>

            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-blue-950 leading-tight">
              AI Knowledge Intelligence Workspace
            </h1>

            <p className="text-sm sm:text-base text-slate-600 leading-relaxed font-normal">
              Automated ingestion of business documents (PDF, DOCX, TXT, MD, CSV) into standardized{' '}
              <span className="font-semibold text-blue-900">Open Knowledge Framework (OKF v0.2)</span>{' '}
              format with local 768-D vector semantic search.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row lg:flex-col xl:flex-row gap-3 shrink-0">
            <Link
              to="/documents"
              className="inline-flex items-center justify-center px-5 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm shadow-md shadow-blue-500/20 transition-all hover:shadow-lg hover:shadow-blue-500/30"
            >
              <Upload className="w-4 h-4 mr-2" />
              Ingest Document
            </Link>
            <Link
              to="/chat"
              className="inline-flex items-center justify-center px-5 py-3 rounded-xl bg-white hover:bg-slate-50 text-slate-800 border border-slate-300 font-medium text-sm shadow-xs transition-all hover:border-slate-400"
            >
              <MessageSquare className="w-4 h-4 mr-2 text-blue-600" />
              Launch Neural Chat
            </Link>
          </div>
        </div>
      </div>

      {/* 2. Key Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {stats.map((stat, idx) => {
          const Icon = stat.icon;
          return (
            <div
              key={idx}
              className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs hover:border-blue-300 hover:shadow-md transition-all group"
            >
              <div className="flex items-start justify-between">
                <div className={`p-3 rounded-xl ${stat.bgColor} border ${stat.borderColor}`}>
                  <Icon className={`w-5 h-5 ${stat.color}`} />
                </div>
                <span
                  className={`text-[11px] font-mono font-bold px-2.5 py-0.5 rounded-full border ${stat.badgeStyle}`}
                >
                  {stat.badge}
                </span>
              </div>
              <div className="mt-4 space-y-1">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-600">
                  {stat.title}
                </span>
                <p className="text-2xl sm:text-3xl font-extrabold text-blue-950 tracking-tight">
                  {stat.value}
                </p>
                <p className="text-xs text-slate-600 font-medium">
                  {stat.subtext}
                </p>
              </div>
            </div>
          );
        })}
      </div>

      {/* 3. Main Operational Sections Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left 8 cols: Recent Documents & Chat Activity */}
        <div className="lg:col-span-8 space-y-8">
          {/* Recent Documents Table Card */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-blue-950 flex items-center space-x-2">
                  <FileText className="w-4 h-4 text-blue-600" />
                  <span>Recent Ingested Documents</span>
                </h2>
                <p className="text-xs text-slate-600 mt-0.5">
                  Live state of the OKF parsing and vectorization pipeline
                </p>
              </div>
              <Link
                to="/documents"
                className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center space-x-1"
              >
                <span>View all ({status?.totalDocuments ?? 0})</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                    <th className="py-3 px-5">Document</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Chunks</th>
                    <th className="py-3 px-4">Date</th>
                    <th className="py-3 px-5 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {docsLoading ? (
                    <tr>
                      <td colSpan={5} className="py-8 text-center text-slate-600 text-xs">
                        <RefreshCw className="w-5 h-5 animate-spin mx-auto text-blue-600 mb-2" />
                        Loading recent documents...
                      </td>
                    </tr>
                  ) : !docPage?.items || docPage.items.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="py-10 text-center text-slate-600 text-xs">
                        <FileText className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                        <p className="font-semibold text-slate-700">No documents ingested yet</p>
                        <p className="text-slate-600 mt-1">Upload PDF, DOCX, TXT or Markdown files to begin.</p>
                        <Link
                          to="/documents"
                          className="mt-3 inline-flex items-center text-xs font-semibold text-blue-600 hover:underline"
                        >
                          <Upload className="w-3.5 h-3.5 mr-1" /> Ingest your first document
                        </Link>
                      </td>
                    </tr>
                  ) : (
                    docPage.items.map((doc: DocumentItem) => (
                      <tr key={doc.id} className="hover:bg-slate-50/80 transition-colors">
                        <td className="py-3.5 px-5">
                          <div className="flex items-center space-x-3">
                            <div className="p-2 rounded-lg bg-blue-50 text-blue-600 border border-blue-100 shrink-0">
                              <FileText className="w-4 h-4" />
                            </div>
                            <div className="min-w-0">
                              <Link
                                to={`/documents/${doc.id}`}
                                className="font-semibold text-blue-950 hover:text-blue-600 truncate block text-xs sm:text-sm"
                              >
                                {doc.originalFilename || doc.title}
                              </Link>
                              <span className="text-[11px] text-slate-600 font-mono">
                                {(doc.fileSize / 1024).toFixed(1)} KB • {doc.fileType?.toUpperCase()}
                              </span>
                            </div>
                          </div>
                        </td>
                        <td className="py-3.5 px-4">
                          <span
                            className={`text-[11px] font-bold px-2.5 py-1 rounded-full uppercase inline-flex items-center space-x-1 ${
                              doc.status === 'COMPLETED'
                                ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                                : doc.status === 'FAILED'
                                ? 'bg-rose-50 text-rose-700 border border-rose-200'
                                : doc.status === 'EMBEDDING'
                                ? 'bg-blue-50 text-blue-700 border border-blue-200'
                                : doc.status === 'CHUNKING'
                                ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                                : doc.status === 'CONVERTING_OKF'
                                ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                                : doc.status === 'EXTRACTING'
                                ? 'bg-cyan-50 text-cyan-800 border border-cyan-200'
                                : 'bg-amber-50 text-amber-700 border border-amber-200'
                            }`}
                          >
                            {doc.status === 'COMPLETED' ? (
                              <Check className="w-3 h-3 mr-0.5 text-emerald-700" />
                            ) : doc.status === 'FAILED' ? (
                              <AlertTriangle className="w-3 h-3 mr-0.5 text-rose-700" />
                            ) : doc.status === 'PENDING' ? (
                              <Clock className="w-3 h-3 mr-0.5 animate-pulse text-amber-700" />
                            ) : (
                              <RefreshCw className="w-3 h-3 mr-0.5 animate-spin" />
                            )}
                            <span>{doc.status}</span>
                          </span>
                        </td>
                        <td className="py-3.5 px-4 text-xs font-mono text-slate-700 font-semibold">
                          {doc.chunkCount ?? 0}
                        </td>
                        <td className="py-3.5 px-4 text-xs text-slate-600 font-medium">
                          {new Date(doc.createdAt).toLocaleDateString()}
                        </td>
                        <td className="py-3.5 px-5 text-right">
                          <Link
                            to={`/documents/${doc.id}`}
                            className="p-1.5 rounded-lg text-slate-600 hover:text-blue-600 hover:bg-blue-50 inline-flex transition-colors"
                            title="Inspect document & OKF representation"
                          >
                            <ArrowRight className="w-4 h-4" />
                          </Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Recent Neural Chat Conversations */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-blue-950 flex items-center space-x-2">
                  <MessageSquare className="w-4 h-4 text-blue-600" />
                  <span>Recent Neural Chat Sessions</span>
                </h2>
                <p className="text-xs text-slate-600 mt-0.5">
                  RAG-grounded sessions referencing ingested documents
                </p>
              </div>
              <Link
                to="/chat"
                className="inline-flex items-center px-3 py-1.5 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 text-xs font-semibold transition-colors"
              >
                <span>New Session</span>
                <ArrowRight className="w-3.5 h-3.5 ml-1" />
              </Link>
            </div>

            <div className="divide-y divide-slate-100">
              {sessionsLoading ? (
                <div className="p-6 text-center text-slate-600 text-xs">
                  <RefreshCw className="w-4 h-4 animate-spin mx-auto text-blue-600 mb-2" />
                  Loading chat history...
                </div>
              ) : !sessions || sessions.length === 0 ? (
                <div className="p-8 text-center text-slate-600 text-xs">
                  <Bot className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                  <p className="font-semibold text-slate-700">No chat sessions yet</p>
                  <p className="text-slate-600 mt-1">Start a conversation with grounded document citations.</p>
                  <Link
                    to="/chat"
                    className="mt-3 inline-flex items-center text-xs font-semibold text-blue-600 hover:underline"
                  >
                    Open Neural Chat <ArrowRight className="w-3.5 h-3.5 ml-1" />
                  </Link>
                </div>
              ) : (
                sessions.slice(0, 5).map((session: ChatSession) => (
                  <Link
                    key={session.id}
                    to={`/chat?session=${session.id}`}
                    className="p-4 hover:bg-slate-50/80 transition-colors flex items-center justify-between block group"
                  >
                    <div className="flex items-center space-x-3 min-w-0">
                      <div className="p-2 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-100 shrink-0 group-hover:bg-blue-50 group-hover:text-blue-600 group-hover:border-blue-200 transition-colors">
                        <MessageSquare className="w-4 h-4" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="font-semibold text-xs sm:text-sm text-blue-950 group-hover:text-blue-600 truncate">
                          {session.title || 'Untitled Session'}
                        </h3>
                        <p className="text-[11px] text-slate-600 font-mono">
                          {session.messageCount ?? 0} messages • Created {new Date(session.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    <ArrowRight className="w-4 h-4 text-slate-400 group-hover:text-blue-600 group-hover:translate-x-0.5 transition-all shrink-0" />
                  </Link>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Right 4 cols: Architecture & Telemetry Card */}
        <div className="lg:col-span-4 space-y-6">
          {/* System Telemetry Card */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs p-5 space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center space-x-2">
                <Activity className="w-4 h-4 text-blue-600" />
                <h3 className="text-sm font-bold text-blue-950 uppercase tracking-wider">
                  Engine Telemetry
                </h3>
              </div>
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold font-mono bg-emerald-50 text-emerald-700 border border-emerald-200">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1 animate-pulse" />
                Live
              </span>
            </div>

            <div className="space-y-3.5 text-xs">
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                <div className="flex items-center space-x-2 text-slate-600">
                  <Cpu className="w-4 h-4 text-indigo-600" />
                  <span className="font-medium">Ollama Core</span>
                </div>
                <span className={`font-mono font-bold ${status?.ollamaConnected ? 'text-emerald-700' : 'text-amber-600'}`}>
                  {status?.ollamaConnected ? 'Active (:11434)' : 'Offline'}
                </span>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                <div className="flex items-center space-x-2 text-slate-600">
                  <Bot className="w-4 h-4 text-blue-600" />
                  <span className="font-medium">Chat Model</span>
                </div>
                <span className="font-mono font-bold text-blue-950">
                  {status?.configuredChatModel || 'llama3.2'}
                </span>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                <div className="flex items-center space-x-2 text-slate-600">
                  <Layers className="w-4 h-4 text-cyan-600" />
                  <span className="font-medium">Embeddings</span>
                </div>
                <span className="font-mono font-bold text-cyan-800">
                  nomic-embed (768-D)
                </span>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                <div className="flex items-center space-x-2 text-slate-600">
                  <Database className="w-4 h-4 text-emerald-600" />
                  <span className="font-medium">Vector Store</span>
                </div>
                <span className="font-mono font-bold text-emerald-700">
                  Cosine In-Memory
                </span>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                <div className="flex items-center space-x-2 text-slate-600">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span className="font-medium">OKF Protocol</span>
                </div>
                <span className="font-mono font-bold text-blue-700">
                  v0.2 Standard
                </span>
              </div>
            </div>
          </div>

          {/* Quick Shortcuts */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs p-5 space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-600">
              Quick Navigation
            </h3>
            <div className="grid grid-cols-2 gap-2.5">
              <Link
                to="/documents"
                className="p-3 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 transition-colors group flex flex-col items-start space-y-2"
              >
                <Upload className="w-4 h-4 text-blue-600 group-hover:scale-110 transition-transform" />
                <span className="text-xs font-semibold text-slate-700 group-hover:text-blue-900">Ingest Files</span>
              </Link>
              <Link
                to="/chat"
                className="p-3 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 transition-colors group flex flex-col items-start space-y-2"
              >
                <MessageSquare className="w-4 h-4 text-emerald-600 group-hover:scale-110 transition-transform" />
                <span className="text-xs font-semibold text-slate-700 group-hover:text-blue-900">Ask Neural AI</span>
              </Link>
              <Link
                to="/okf"
                className="p-3 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 transition-colors group flex flex-col items-start space-y-2"
              >
                <BookOpen className="w-4 h-4 text-purple-600 group-hover:scale-110 transition-transform" />
                <span className="text-xs font-semibold text-slate-700 group-hover:text-blue-900">OKF Explorer</span>
              </Link>
              <Link
                to="/chat"
                className="p-3 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 hover:border-blue-200 transition-colors group flex flex-col items-start space-y-2"
              >
                <History className="w-4 h-4 text-amber-600 group-hover:scale-110 transition-transform" />
                <span className="text-xs font-semibold text-slate-700 group-hover:text-blue-900">Chat History</span>
              </Link>
            </div>
          </div>

          {/* OKF Architecture Info Card */}
          <div className="bg-gradient-to-br from-blue-900 to-indigo-950 rounded-2xl p-5 text-white shadow-md relative overflow-hidden space-y-3">
            <div className="absolute top-0 right-0 -mt-6 -mr-6 w-28 h-28 bg-blue-500/20 rounded-full blur-2xl pointer-events-none" />
            <div className="flex items-center space-x-2">
              <span className="px-2 py-0.5 rounded-md bg-blue-500/30 text-blue-200 text-[10px] font-mono font-bold uppercase tracking-wider border border-blue-400/30">
                OKF v0.2
              </span>
              <span className="text-xs font-bold text-blue-100">Open Knowledge Standard</span>
            </div>
            <p className="text-xs text-blue-100/90 leading-relaxed">
              Every document is parsed into markdown sections, indexed into 768-dimensional semantic vectors, and verified with anti-prompt-injection boundaries.
            </p>
            <div className="pt-1">
              <Link
                to="/okf"
                className="inline-flex items-center text-xs font-semibold text-blue-200 hover:text-white group"
              >
                <span>Inspect OKF Specifications</span>
                <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-1 transition-transform" />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
