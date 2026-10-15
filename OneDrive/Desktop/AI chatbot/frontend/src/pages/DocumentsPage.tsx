import React, { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { DocumentItem } from '../types';
import {
  Upload,
  FileText,
  Search,
  RefreshCw,
  Trash2,
  ExternalLink,
  AlertCircle,
  CheckCircle2,
  Clock,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  BookOpen,
  X,
  FileCode,
  Layers,
  Database,
  Filter,
  Check,
  Sparkles,
  ArrowRight
} from 'lucide-react';

export const DocumentsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'COMPLETED' | 'PENDING' | 'FAILED'>('ALL');
  const [page, setPage] = useState(0);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  const { data: docData, isLoading } = useQuery({
    queryKey: ['documents', search, page],
    queryFn: () => api.documents.list(search, page, 10),
    refetchInterval: (query) => {
      const items = query.state.data?.items;
      const hasActive = items?.some((d: any) => d.status !== 'COMPLETED' && d.status !== 'FAILED');
      return hasActive ? 1500 : 5000;
    },
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => api.documents.upload(file),
    onSuccess: () => {
      setUploadError(null);
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      queryClient.invalidateQueries({ queryKey: ['systemStatus'] });
    },
    onError: (err: any) => {
      setUploadError(err.message || 'Upload failed');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.documents.delete(id),
    onSuccess: () => {
      setDeleteConfirmId(null);
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      queryClient.invalidateQueries({ queryKey: ['systemStatus'] });
    },
  });

  const retryMutation = useMutation({
    mutationFn: (id: string) => api.documents.retry(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });

  const handleFileDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const handleFileUpload = (file: File) => {
    const allowed = ['pdf', 'ppt', 'pptx', 'docx', 'txt', 'md', 'markdown', 'csv'];
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    if (!allowed.includes(ext)) {
      setUploadError(`Unsupported file format (.${ext}). Allowed formats: PDF, PPT, PPTX, DOCX, TXT, MD, CSV.`);
      return;
    }
    if (file.size > 25 * 1024 * 1024) {
      setUploadError('File exceeds maximum allowed size of 25MB.');
      return;
    }
    uploadMutation.mutate(file);
  };

  const filteredItems = docData?.items.filter((doc: DocumentItem) => {
    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'COMPLETED') return doc.status === 'COMPLETED';
    if (statusFilter === 'FAILED') return doc.status === 'FAILED';
    if (statusFilter === 'PENDING') return doc.status !== 'COMPLETED' && doc.status !== 'FAILED';
    return true;
  });

  const totalChunksCount = docData?.items.reduce((acc, d) => acc + (d.chunkCount || 0), 0) || 0;
  const totalSizeBytes = docData?.items.reduce((acc, d) => acc + (d.fileSize || 0), 0) || 0;
  const activeIngestionCount = docData?.items.filter((doc: DocumentItem) => doc.status !== 'COMPLETED' && doc.status !== 'FAILED').length || 0;

  return (
    <div className="space-y-6 pb-10">
      {/* Header and KPI strip */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
              Knowledge Repository
            </span>
            <span className="text-xs text-slate-500 font-medium">Open Knowledge Format v0.2 Pipeline</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-blue-950">
            Document Intelligence Library
          </h1>
          <p className="text-xs sm:text-sm text-slate-600 max-w-xl">
            Ingest enterprise business files, convert narratives into atomic OKF concepts, and embed with 768-D dense vectors for neural RAG.
          </p>
        </div>

        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={uploadMutation.isPending}
          className="px-5 py-3 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 active:scale-95 text-white text-sm font-semibold transition-all shadow-md shadow-blue-500/25 flex items-center justify-center space-x-2 shrink-0 disabled:opacity-50 border border-blue-500/30"
        >
          <Upload className="w-4 h-4" />
          <span>{uploadMutation.isPending ? 'Processing Ingestion...' : 'Upload Document'}</span>
        </button>
        <input
          type="file"
          ref={fileInputRef}
          onChange={(e) => e.target.files?.[0] && handleFileUpload(e.target.files[0])}
          className="hidden"
          accept=".pdf,.ppt,.pptx,.docx,.txt,.md,.markdown,.csv"
        />
      </div>

      {/* Mini Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm">
          <span className="text-xs text-slate-500 font-semibold">Total Ingested</span>
          <p className="text-xl font-extrabold text-blue-950 mt-1">{docData?.totalElements ?? 0}</p>
        </div>
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm">
          <span className="text-xs text-slate-500 font-semibold">Indexed Chunks</span>
          <p className="text-xl font-extrabold text-blue-700 mt-1 font-mono">{totalChunksCount}</p>
        </div>
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm">
          <span className="text-xs text-slate-500 font-semibold">Total Size</span>
          <p className="text-xl font-extrabold text-slate-700 mt-1 font-mono">{(totalSizeBytes / 1024).toFixed(1)} KB</p>
        </div>
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-sm">
          <span className="text-xs text-slate-500 font-semibold">Format Standard</span>
          <p className="text-xl font-extrabold text-blue-700 mt-1 font-mono">OKF v0.2</p>
        </div>
      </div>

      {/* Drag & Drop Zone */}
      <div
        onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleFileDrop}
        className={`p-8 rounded-3xl border-2 border-dashed transition-all text-center cursor-pointer relative overflow-hidden ${
          isDragging
            ? 'border-blue-500 bg-blue-50 shadow-md scale-[1.01]'
            : 'border-slate-200 bg-white hover:border-blue-400 hover:bg-blue-50/40 shadow-sm'
        }`}
        onClick={() => fileInputRef.current?.click()}
      >
        <div className="flex flex-col items-center justify-center space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-600 shadow-xs">
            <Upload className="w-7 h-7" />
          </div>
          <div>
            <p className="text-base font-bold text-blue-950">
              Drag and drop business documents here, or <span className="text-blue-600 underline underline-offset-2">browse files</span>
            </p>
            <p className="text-xs text-slate-500 mt-1">
              Supported Formats: <strong className="text-slate-700 font-semibold">PDF, PPT/PPTX, DOCX, TXT, Markdown, CSV</strong> (Up to 25MB per file)
            </p>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-2 pt-1 text-[11px] font-mono text-slate-600">
            <span className="px-2 py-0.5 rounded bg-slate-100 border border-slate-200 font-medium">PDF (PDFBox)</span>
            <span className="px-2 py-0.5 rounded bg-slate-100 border border-slate-200 font-medium">PPT / PPTX (Apache POI)</span>
            <span className="px-2 py-0.5 rounded bg-slate-100 border border-slate-200 font-medium">DOCX (Apache POI)</span>
            <span className="px-2 py-0.5 rounded bg-slate-100 border border-slate-200 font-medium">CSV (Commons CSV)</span>
            <span className="px-2 py-0.5 rounded bg-slate-100 border border-slate-200 font-medium">Markdown / Plaintext</span>
          </div>
        </div>
      </div>

      {activeIngestionCount > 0 && (
        <div className="p-4 rounded-2xl bg-blue-50 border border-blue-200 flex items-center justify-between text-blue-900 text-xs shadow-xs">
          <div className="flex items-center space-x-3">
            <RefreshCw className="w-4 h-4 animate-spin text-blue-600 shrink-0" />
            <div>
              <span className="font-bold text-blue-950">
                Fast Ingestion Pipeline Active ({activeIngestionCount} {activeIngestionCount === 1 ? 'document' : 'documents'} processing)
              </span>
              <p className="text-[11px] text-slate-600 mt-0.5">
                Real-time parsing, Open Knowledge Format conversion, and 768-D dense vector embedding.
              </p>
            </div>
          </div>
          <span className="text-[11px] font-mono text-blue-800 font-bold px-2.5 py-1 rounded-lg bg-white border border-blue-200 shrink-0 shadow-xs">
            Auto-refreshing (1.5s)
          </span>
        </div>
      )}

      {uploadError && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 flex items-start justify-between text-rose-800 text-xs shadow-xs">
          <div className="flex items-start space-x-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
            <span className="font-semibold">{uploadError}</span>
          </div>
          <button onClick={() => setUploadError(null)} className="text-rose-600 hover:text-rose-800">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Search and Filter Tabs */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            placeholder="Search documents by title or filename..."
            className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white border border-slate-200 text-xs sm:text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-xs"
          />
        </div>

        {/* Filter Tabs */}
        <div className="flex items-center space-x-1.5 p-1 rounded-2xl bg-white border border-slate-200 text-xs shadow-xs">
          {(['ALL', 'COMPLETED', 'PENDING', 'FAILED'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setStatusFilter(tab)}
              className={`px-3 py-1.5 rounded-xl font-bold transition-all ${
                statusFilter === tab
                  ? 'bg-blue-600 text-white shadow-xs'
                  : 'text-slate-600 hover:text-blue-700 hover:bg-blue-50'
              }`}
            >
              {tab === 'ALL' ? 'All Records' : tab}
            </button>
          ))}
        </div>
      </div>

      {/* Document Table */}
      <div className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-[11px] uppercase tracking-wider text-blue-950 font-bold">
              <tr>
                <th className="py-4 px-5">Document Title & Filename</th>
                <th className="py-4 px-4">Format</th>
                <th className="py-4 px-4">File Size</th>
                <th className="py-4 px-4">Vectors</th>
                <th className="py-4 px-4">Pipeline Status</th>
                <th className="py-4 px-4">Ingested Date</th>
                <th className="py-4 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-16 text-center text-slate-500 text-xs">
                    <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
                    Loading enterprise documents...
                  </td>
                </tr>
              ) : filteredItems && filteredItems.length > 0 ? (
                filteredItems.map((doc: DocumentItem) => (
                  <tr key={doc.id} className="hover:bg-blue-50/40 transition-colors group">
                    <td className="py-3.5 px-5">
                      <div className="flex items-center space-x-3">
                        <div className="w-9 h-9 rounded-xl bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-700 font-mono text-xs uppercase font-bold shrink-0">
                          {doc.fileType}
                        </div>
                        <div className="min-w-0">
                          <Link
                            to={`/documents/${doc.id}`}
                            className="font-bold text-blue-950 hover:text-blue-700 transition-colors block max-w-sm truncate"
                          >
                            {doc.title}
                          </Link>
                          <span className="text-xs text-slate-500 block truncate max-w-sm font-mono mt-0.5">
                            {doc.originalFilename}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="font-mono text-xs uppercase px-2 py-0.5 rounded-md bg-blue-50 text-blue-800 border border-blue-200 font-semibold">
                        {doc.fileType}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-xs text-slate-700 font-mono font-medium">
                      {(doc.fileSize / 1024).toFixed(1)} KB
                    </td>
                    <td className="py-3.5 px-4 text-xs text-blue-700 font-mono font-bold">
                      {doc.chunkCount} chunks
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
                    <td className="py-3.5 px-4 text-xs text-slate-500 font-medium">
                      {new Date(doc.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-3.5 px-5 text-right">
                      <div className="flex items-center justify-end space-x-1.5">
                        <Link
                          to={`/documents/${doc.id}`}
                          title="Inspect Details & OKF Markdown"
                          className="p-2 rounded-xl text-slate-400 hover:text-blue-700 hover:bg-blue-50 transition-colors"
                        >
                          <BookOpen className="w-4 h-4" />
                        </Link>

                        <Link
                          to="/chat"
                          state={{
                            initialPrompt: `Tell me about the key concepts in ${doc.title}`,
                            attachedDoc: { id: doc.id, title: doc.title, fileType: doc.fileType }
                          }}
                          title="Ask AI questions about this document"
                          className="p-2 rounded-xl text-slate-400 hover:text-blue-700 hover:bg-blue-50 transition-colors"
                        >
                          <Sparkles className="w-4 h-4" />
                        </Link>

                        {doc.status === 'FAILED' && (
                          <button
                            onClick={() => retryMutation.mutate(doc.id)}
                            title="Retry ingestion pipeline"
                            className="p-2 rounded-xl text-amber-600 hover:bg-amber-50 transition-colors"
                          >
                            <RefreshCw className="w-4 h-4" />
                          </button>
                        )}

                        <button
                          onClick={() => setDeleteConfirmId(doc.id)}
                          title="Delete document"
                          className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="py-16 text-center text-slate-500 text-xs">
                    No documents found matching the filter criteria.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {docData && docData.totalPages > 1 && (
          <div className="p-4 border-t border-slate-200 bg-slate-50/50 flex items-center justify-between text-xs text-slate-600">
            <span>
              Page {docData.page + 1} of {docData.totalPages} ({docData.totalElements} records)
            </span>
            <div className="flex items-center space-x-2">
              <button
                disabled={!docData.hasPrevious}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="p-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 disabled:opacity-40 transition-colors shadow-xs"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                disabled={!docData.hasNext}
                onClick={() => setPage((p) => p + 1)}
                className="p-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-100 disabled:opacity-40 transition-colors shadow-xs"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      {deleteConfirmId && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white p-6 sm:p-8 rounded-3xl max-w-md w-full space-y-4 border border-rose-200 shadow-xl">
            <div className="flex items-center space-x-3 text-rose-600">
              <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-200 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <h3 className="text-base font-bold text-slate-900">Confirm Permanent Deletion</h3>
                <p className="text-xs text-slate-500 font-medium">Irreversible Action</p>
              </div>
            </div>
            <p className="text-xs text-slate-600 leading-relaxed">
              Are you sure you want to delete this document? This will remove the source file, delete the generated OKF v0.2 Markdown concept representation, and purge all vector embeddings from the index.
            </p>
            <div className="flex items-center justify-end space-x-2.5 pt-2">
              <button
                onClick={() => setDeleteConfirmId(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs text-slate-700 font-semibold transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => deleteMutation.mutate(deleteConfirmId)}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-xs text-white font-semibold shadow-md shadow-rose-600/20 transition-all active:scale-95"
              >
                Delete Permanently
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
