import React, { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import { ProcessingJob } from '../types';
import {
  ArrowLeft,
  FileText,
  Clock,
  Layers,
  RefreshCw,
  Trash2,
  CheckCircle2,
  AlertTriangle,
  Code,
  BookOpen,
  Sparkles,
  Download,
  Copy,
  Check,
  Cpu,
  Database,
  ArrowRight
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';

export const DocumentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'overview' | 'okf' | 'jobs'>('overview');
  const [copiedRaw, setCopiedRaw] = useState(false);

  const { data: doc, isLoading } = useQuery({
    queryKey: ['document', id],
    queryFn: () => (id ? api.documents.get(id) : Promise.reject('No ID')),
    enabled: !!id,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status !== 'COMPLETED' && status !== 'FAILED' ? 1200 : 5000;
    },
  });

  const { data: okfData } = useQuery({
    queryKey: ['documentOkf', id],
    queryFn: () => (id ? api.documents.getOkf(id) : Promise.reject('No ID')),
    enabled: !!id && doc?.status === 'COMPLETED',
  });

  const retryMutation = useMutation({
    mutationFn: (docId: string) => api.documents.retry(docId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['document', id] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (docId: string) => api.documents.delete(docId),
    onSuccess: () => {
      navigate('/documents');
    },
  });

  const handleCopyRaw = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedRaw(true);
    setTimeout(() => setCopiedRaw(false), 2000);
  };

  if (isLoading || !doc) {
    return (
      <div className="py-20 text-center text-slate-500 text-xs">
        <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
        Loading document intelligence...
      </div>
    );
  }

  const isCompleted = doc.status === 'COMPLETED';
  const stageOrder = ['PENDING', 'EXTRACTING', 'CONVERTING_OKF', 'CHUNKING', 'EMBEDDING', 'COMPLETED'];
  const currentIndex = stageOrder.indexOf(doc.status);

  const pipelineStages = [
    {
      name: 'Document Extraction',
      desc: `Parsed via ${doc.fileType.toUpperCase()} Engine`,
      done: isCompleted || currentIndex > 1,
      active: doc.status === 'EXTRACTING'
    },
    {
      name: 'OKF v0.2 Generation',
      desc: 'SnakeYAML Frontmatter & Concepts',
      done: isCompleted || currentIndex > 2,
      active: doc.status === 'CONVERTING_OKF'
    },
    {
      name: 'Text Chunking',
      desc: doc.chunkCount > 0 ? `${doc.chunkCount} Contextual Chunks` : 'Contextual splitting',
      done: isCompleted || currentIndex > 3,
      active: doc.status === 'CHUNKING'
    },
    {
      name: 'Dense Embeddings',
      desc: doc.chunkCount > 0 ? `${doc.chunkCount} Vectors Indexed` : '768-D Vectors Indexed',
      done: isCompleted,
      active: doc.status === 'EMBEDDING'
    },
  ];

  return (
    <div className="space-y-6 pb-10">
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <Link
          to="/documents"
          className="inline-flex items-center space-x-2 text-xs font-bold text-slate-500 hover:text-blue-700 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Document Library</span>
        </Link>

        <div className="flex items-center space-x-2">
          <Link
            to="/chat"
            state={{
              initialPrompt: `What are the key concepts in ${doc.title}?`,
              attachedDoc: { id: doc.id, title: doc.title, fileType: doc.fileType }
            }}
            className="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold flex items-center space-x-1.5 shadow-sm transition-all active:scale-95"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Chat with this Document</span>
          </Link>

          {doc.status === 'FAILED' && (
            <button
              onClick={() => id && retryMutation.mutate(id)}
              disabled={retryMutation.isPending}
              className="px-3.5 py-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-700 border border-amber-200 text-xs font-semibold flex items-center space-x-1.5 transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Retry Pipeline</span>
            </button>
          )}

          <button
            onClick={() => {
              if (window.confirm('Are you sure you want to permanently delete this document and its vector embeddings?')) {
                id && deleteMutation.mutate(id);
              }
            }}
            className="px-3.5 py-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 text-xs font-semibold flex items-center space-x-1.5 transition-colors"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Delete</span>
          </button>
        </div>
      </div>

      {/* Main Header Card */}
      <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 space-y-6 shadow-sm">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center space-x-2.5">
              <span className="font-mono text-xs uppercase px-2.5 py-0.5 rounded-md bg-blue-50 text-blue-800 border border-blue-200 font-bold">
                {doc.fileType}
              </span>
              <span
                className={`text-[11px] font-bold px-2.5 py-1 rounded-full uppercase inline-flex items-center space-x-1 ${
                  doc.status === 'COMPLETED'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : doc.status === 'FAILED'
                    ? 'bg-rose-50 text-rose-700 border border-rose-200'
                    : 'bg-amber-50 text-amber-700 border border-amber-200 animate-pulse'
                }`}
              >
                {doc.status === 'COMPLETED' && <Check className="w-3 h-3 mr-0.5" />}
                <span>{doc.status}</span>
              </span>
              <span className="text-[11px] font-mono text-slate-600 bg-slate-50 px-2 py-0.5 rounded-md border border-slate-200 font-medium">
                OKF v0.2 Validated
              </span>
            </div>

            <h1 className="text-xl sm:text-2xl lg:text-3xl font-extrabold text-blue-950 tracking-tight">
              {doc.title}
            </h1>
            <p className="text-xs text-slate-500 font-mono">
              Source Filename: {doc.originalFilename}
            </p>
          </div>

          <div className="grid grid-cols-3 gap-3 p-3.5 rounded-2xl bg-slate-50 border border-slate-200 shrink-0 text-center">
            <div className="px-3">
              <span className="block text-slate-500 text-[10px] uppercase font-bold">File Size</span>
              <span className="font-mono text-slate-800 font-bold text-sm">{(doc.fileSize / 1024).toFixed(1)} KB</span>
            </div>
            <div className="px-3 border-x border-slate-200">
              <span className="block text-slate-500 text-[10px] uppercase font-bold">Pages</span>
              <span className="font-mono text-slate-800 font-bold text-sm">{doc.pageCount}</span>
            </div>
            <div className="px-3">
              <span className="block text-slate-500 text-[10px] uppercase font-bold">Indexed Vectors</span>
              <span className="font-mono text-blue-700 font-bold text-sm">{doc.chunkCount}</span>
            </div>
          </div>
        </div>

        {/* Pipeline Stepper */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-4 border-t border-slate-200">
          {pipelineStages.map((stage, sIdx) => (
            <div
              key={sIdx}
              className={`p-3.5 rounded-2xl border transition-all ${
                stage.done
                  ? 'bg-emerald-50/60 border-emerald-200 text-emerald-900'
                  : stage.active
                  ? 'bg-blue-50 border-blue-400 shadow-xs'
                  : 'bg-slate-50 border-slate-200'
              }`}
            >
              <div className="flex items-center space-x-2">
                <div
                  className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold ${
                    stage.done
                      ? 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                      : stage.active
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-200 text-slate-600'
                  }`}
                >
                  {stage.done ? (
                    <Check className="w-3 h-3 text-emerald-700" />
                  ) : stage.active ? (
                    <RefreshCw className="w-3 h-3 text-white animate-spin" />
                  ) : (
                    sIdx + 1
                  )}
                </div>
                <span className={`text-xs font-bold ${stage.done ? 'text-emerald-950' : stage.active ? 'text-blue-950' : 'text-slate-600'}`}>
                  {stage.name}
                </span>
              </div>
              <p className="text-[11px] text-slate-500 mt-1 pl-7 font-mono">{stage.desc}</p>
            </div>
          ))}
        </div>

        {doc.errorMessage && (
          <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 flex items-start space-x-2.5 text-rose-800 text-xs shadow-xs">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
            <div>
              <span className="font-bold block">Ingestion Error Log:</span>
              <span className="font-mono mt-0.5 block">{doc.errorMessage}</span>
            </div>
          </div>
        )}

        {/* Tab navigation */}
        <div className="flex items-center space-x-2 border-t border-slate-200 pt-4">
          <button
            onClick={() => setActiveTab('overview')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center space-x-2 ${
              activeTab === 'overview'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-blue-700 hover:bg-blue-50'
            }`}
          >
            <FileText className="w-3.5 h-3.5" />
            <span>Overview & Metadata</span>
          </button>
          <button
            onClick={() => setActiveTab('okf')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center space-x-2 ${
              activeTab === 'okf'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-blue-700 hover:bg-blue-50'
            }`}
          >
            <BookOpen className="w-3.5 h-3.5" />
            <span>OKF v0.2 Markdown Inspector</span>
          </button>
        </div>
      </div>

      {/* Tab Contents */}
      {activeTab === 'overview' && (
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 space-y-6 shadow-sm">
          <h2 className="text-sm font-bold uppercase tracking-wider text-blue-950">
            Document Identity & Persistence
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5">
              <span className="text-slate-500 uppercase font-bold text-[10px]">Document UUID</span>
              <p className="font-mono text-slate-800 break-all font-medium">{doc.id}</p>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5">
              <span className="text-slate-500 uppercase font-bold text-[10px]">OKF Bundle Location</span>
              <p className="font-mono text-slate-800 break-all font-medium">{doc.okfPath || 'Generated upon ingestion'}</p>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5">
              <span className="text-slate-500 uppercase font-bold text-[10px]">Creation Timestamp</span>
              <p className="font-mono text-slate-800 font-medium">{new Date(doc.createdAt).toISOString()}</p>
            </div>
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-1.5">
              <span className="text-slate-500 uppercase font-bold text-[10px]">Vector Density</span>
              <p className="font-mono text-emerald-700 font-bold">{doc.chunkCount} Chunks indexed in Vector Store</p>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'okf' && (
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 space-y-6 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <BookOpen className="w-5 h-5 text-blue-600" />
              <h2 className="text-sm font-bold text-blue-950 uppercase tracking-wider">
                OKF v0.2 Markdown & Frontmatter
              </h2>
            </div>
            {okfData?.rawContent && (
              <button
                onClick={() => handleCopyRaw(okfData.rawContent)}
                className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-blue-950 text-xs font-bold flex items-center space-x-1.5 transition-colors border border-slate-200"
              >
                {copiedRaw ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                    <span className="text-emerald-700">Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5 text-slate-500" />
                    <span>Copy Raw OKF</span>
                  </>
                )}
              </button>
            )}
          </div>

          {okfData ? (
            <div className="space-y-6">
              {/* Frontmatter YAML Card */}
              {okfData.rawContent && (
                <div className="space-y-2">
                  <span className="text-xs text-blue-900 font-semibold font-mono">YAML Frontmatter Header:</span>
                  <pre className="p-4 rounded-2xl bg-slate-50 border border-slate-200 font-mono text-xs text-slate-800 overflow-x-auto leading-relaxed shadow-xs">
                    {okfData.rawContent.split('---')[1] ? `---${okfData.rawContent.split('---')[1]}---` : '---'}
                  </pre>
                </div>
              )}

              {/* Formatted Markdown */}
              <div className="space-y-2">
                <span className="text-xs text-blue-900 font-semibold font-mono">Formatted Concept Markdown:</span>
                <div className="p-6 rounded-2xl bg-slate-50 border border-slate-200 markdown-content shadow-xs">
                  <ReactMarkdown>
                    {okfData.markdownBody || okfData.rawContent}
                  </ReactMarkdown>
                </div>
              </div>
            </div>
          ) : (
            <div className="py-12 text-center text-slate-500 text-xs">
              OKF representation is not ready or failed to generate.
            </div>
          )}
        </div>
      )}

      {activeTab === 'jobs' && (
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 space-y-4 shadow-sm">
          <h2 className="text-sm font-bold uppercase tracking-wider text-blue-950">
            Pipeline Execution Audit Trail
          </h2>
          <div className="space-y-2.5">
            {doc.jobs && doc.jobs.length > 0 ? (
              doc.jobs.map((job: ProcessingJob) => (
                <div
                  key={job.id}
                  className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between text-xs"
                >
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="font-mono text-blue-950 font-bold">{job.stage}</span>
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          job.status === 'COMPLETED'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : job.status === 'FAILED'
                            ? 'bg-rose-50 text-rose-700 border border-rose-200'
                            : 'bg-amber-50 text-amber-700 border border-amber-200'
                        }`}
                      >
                        {job.status}
                      </span>
                    </div>
                    {job.errorMessage && (
                      <p className="text-rose-600 font-mono text-[11px] font-medium">{job.errorMessage}</p>
                    )}
                  </div>
                  <div className="text-right text-[11px] text-slate-500 font-mono">
                    <div>Started: {new Date(job.startedAt).toLocaleTimeString()}</div>
                    {job.completedAt && (
                      <div>Completed: {new Date(job.completedAt).toLocaleTimeString()}</div>
                    )}
                  </div>
                </div>
              ))
            ) : (
              <div className="py-8 text-center text-slate-500 text-xs">
                No job execution records available.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
