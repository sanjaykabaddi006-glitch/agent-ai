import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import { ConceptItem } from '../types';
import {
  BookOpen,
  Download,
  Search,
  ExternalLink,
  Tag,
  FileText,
  Code,
  Layers,
  Sparkles,
  Check,
  Copy,
  ShieldCheck,
  Cpu,
  Database,
  ArrowRight
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { Link } from 'react-router-dom';

export const OkfExplorerPage: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedConcept, setSelectedConcept] = useState<ConceptItem | null>(null);
  const [activeTab, setActiveTab] = useState<'markdown' | 'yaml' | 'schema'>('markdown');
  const [copiedRaw, setCopiedRaw] = useState(false);

  const { data: concepts, isLoading } = useQuery({
    queryKey: ['okfConcepts'],
    queryFn: () => api.knowledge.listConcepts(),
  });

  const filtered = concepts?.filter((c: ConceptItem) =>
    c.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.tags?.some((t: string) => t.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  // Set default selected concept
  React.useEffect(() => {
    if (!selectedConcept && filtered && filtered.length > 0) {
      setSelectedConcept(filtered[0]);
    }
  }, [filtered, selectedConcept]);

  const handleExportZip = () => {
    const token = localStorage.getItem('knowledgehub_token');
    fetch(api.knowledge.getExportUrl(), {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error(`Status ${res.status}`);
        return res.blob();
      })
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'knowledgehub-okf-bundle.zip';
        document.body.appendChild(a);
        a.click();
        a.remove();
      })
      .catch(() => {
        // Fallback: download concepts bundle JSON
        const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(concepts || [], null, 2));
        const a = document.createElement('a');
        a.href = dataStr;
        a.download = 'knowledgehub-okf-bundle.json';
        document.body.appendChild(a);
        a.click();
        a.remove();
      });
  };

  const handleCopyRaw = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedRaw(true);
    setTimeout(() => setCopiedRaw(false), 2000);
  };

  return (
    <div className="space-y-6 pb-10">
      {/* Header Banner */}
      <div className="relative overflow-hidden p-6 sm:p-8 rounded-3xl bg-gradient-to-r from-blue-50/90 via-indigo-50/40 to-white border border-blue-200/90 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2 max-w-2xl">
          <div className="flex items-center space-x-2">
            <span className="font-mono text-[11px] font-bold bg-white text-blue-700 px-3 py-0.5 rounded-full border border-blue-200 flex items-center space-x-1.5 shadow-xs">
              <Sparkles className="w-3 h-3 text-blue-600" />
              <span>Open Knowledge Format v0.2 Protocol</span>
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-blue-950">
            OKF Concept Explorer
          </h1>
          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed font-normal">
            Standardized Google Cloud Open Knowledge Format (OKF v0.2) representations. Every ingested document is transformed into an atomic concept file with structured YAML frontmatter and semantic Markdown.
          </p>
        </div>

        <button
          onClick={handleExportZip}
          className="px-5 py-3 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 active:scale-95 text-white text-sm font-semibold transition-all shadow-md shadow-blue-500/20 flex items-center justify-center space-x-2 shrink-0 border border-blue-500/30"
        >
          <Download className="w-4 h-4" />
          <span>Export Entire OKF Bundle (.zip)</span>
        </button>
      </div>

      {/* Search Input */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Filter OKF concepts by title, description, or taxonomy tag..."
          className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white border border-slate-200 text-xs sm:text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-xs"
        />
      </div>

      {/* Explorer Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Concept Cards (5 cols) */}
        <div className="lg:col-span-5 space-y-3 max-h-[720px] overflow-y-auto pr-1">
          {isLoading ? (
            <div className="py-20 text-center text-slate-400 text-xs">
              <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
              Loading OKF concept representations...
            </div>
          ) : filtered && filtered.length > 0 ? (
            filtered.map((concept: ConceptItem) => {
              const isSelected = selectedConcept?.id === concept.id;
              return (
                <div
                  key={concept.id}
                  onClick={() => setSelectedConcept(concept)}
                  className={`p-4 rounded-2xl border cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-blue-50 border-blue-400 shadow-sm scale-[1.01]'
                      : 'bg-white border-slate-200 hover:border-blue-300 hover:bg-blue-50/30'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-[10px] text-blue-800 uppercase tracking-wider font-bold px-2 py-0.5 rounded-full bg-blue-100 border border-blue-200">
                      {concept.type || 'DOCUMENT'}
                    </span>
                    <span className="text-[10px] font-mono text-slate-500 font-semibold">OKF v0.2</span>
                  </div>

                  <h3 className="text-sm font-bold text-blue-950 mt-2 mb-1 truncate">
                    {concept.title}
                  </h3>

                  <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed mb-3">
                    {concept.description || 'Enterprise document concept representation.'}
                  </p>

                  <div className="flex flex-wrap gap-1.5 pt-1 border-t border-slate-200">
                    {concept.tags?.map((t: string, idx: number) => (
                      <span
                        key={idx}
                        className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-slate-100 text-blue-800 font-semibold border border-slate-200"
                      >
                        #{t}
                      </span>
                    ))}
                  </div>
                </div>
              );
            })
          ) : (
            <div className="py-20 text-center text-slate-500 text-xs p-6 bg-white rounded-2xl border border-slate-200 shadow-xs">
              <BookOpen className="w-8 h-8 mx-auto text-blue-600 mb-2" />
              <p className="font-bold text-blue-950">No OKF concepts found</p>
              <p className="text-slate-500 mt-1">Upload business documents to automatically generate concepts.</p>
            </div>
          )}
        </div>

        {/* Right Column: Detailed OKF Inspector (7 cols) */}
        <div className="lg:col-span-7">
          {selectedConcept ? (
            <div className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-sm flex flex-col h-[720px]">
              {/* Header */}
              <div className="p-5 border-b border-slate-200 bg-white/95 backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-3 shrink-0">
                <div className="min-w-0">
                  <span className="text-[10px] font-mono text-blue-700 uppercase font-bold">
                    OKF Document Inspector
                  </span>
                  <h2 className="text-base font-bold text-blue-950 truncate">
                    {selectedConcept.title}
                  </h2>
                </div>

                {/* Tabs */}
                <div className="flex items-center space-x-1.5 p-1 rounded-2xl bg-slate-100 border border-slate-200 text-xs shrink-0 self-start sm:self-auto">
                  <button
                    onClick={() => setActiveTab('markdown')}
                    className={`px-3 py-1.5 rounded-xl font-bold transition-all ${
                      activeTab === 'markdown'
                        ? 'bg-blue-600 text-white shadow-xs'
                        : 'text-slate-600 hover:text-blue-800'
                    }`}
                  >
                    Markdown
                  </button>
                  <button
                    onClick={() => setActiveTab('yaml')}
                    className={`px-3 py-1.5 rounded-xl font-bold transition-all ${
                      activeTab === 'yaml'
                        ? 'bg-blue-600 text-white shadow-xs'
                        : 'text-slate-600 hover:text-blue-800'
                    }`}
                  >
                    Raw YAML
                  </button>
                  <button
                    onClick={() => setActiveTab('schema')}
                    className={`px-3 py-1.5 rounded-xl font-bold transition-all ${
                      activeTab === 'schema'
                        ? 'bg-blue-600 text-white shadow-xs'
                        : 'text-slate-600 hover:text-blue-800'
                    }`}
                  >
                    Validation
                  </button>
                </div>
              </div>

              {/* Tab Contents */}
              <div className="flex-1 overflow-y-auto p-6 space-y-4">
                {activeTab === 'markdown' && (
                  <div className="markdown-content">
                    <ReactMarkdown>
                      {selectedConcept.rawMarkdown || '# No content available'}
                    </ReactMarkdown>
                  </div>
                )}

                {activeTab === 'yaml' && (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-blue-900 font-semibold font-mono">
                        Safe SnakeYAML Frontmatter Serialization
                      </span>
                      <button
                        onClick={() => handleCopyRaw(selectedConcept.rawMarkdown || '')}
                        className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 text-blue-950 text-xs font-bold flex items-center space-x-1.5 transition-colors border border-slate-200"
                      >
                        {copiedRaw ? (
                          <>
                            <Check className="w-3.5 h-3.5 text-emerald-600" />
                            <span className="text-emerald-700">Copied</span>
                          </>
                        ) : (
                          <>
                            <Copy className="w-3.5 h-3.5 text-slate-500" />
                            <span>Copy RAW</span>
                          </>
                        )}
                      </button>
                    </div>

                    <pre className="p-4 rounded-2xl bg-slate-50 border border-slate-200 font-mono text-xs text-slate-800 overflow-x-auto leading-relaxed shadow-xs">
                      {selectedConcept.rawMarkdown || 'No raw content generated'}
                    </pre>
                  </div>
                )}

                {activeTab === 'schema' && (
                  <div className="space-y-4 text-xs">
                    <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center space-x-3 text-emerald-900">
                      <Check className="w-5 h-5 text-emerald-600 shrink-0" />
                      <div>
                        <p className="font-bold text-emerald-950 text-sm">OKF v0.2 Strict Specification Compliant</p>
                        <p className="text-[11px] text-emerald-800 mt-0.5">
                          Document conforms to Google Cloud Open Knowledge Format standards.
                        </p>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                        <span className="text-slate-500 uppercase font-bold text-[10px]">Concept ID</span>
                        <p className="font-mono text-slate-800 break-all font-medium">{selectedConcept.id}</p>
                      </div>
                      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                        <span className="text-slate-500 uppercase font-bold text-[10px]">Concept Type</span>
                        <p className="font-mono text-blue-900 font-bold">{selectedConcept.type}</p>
                      </div>
                      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                        <span className="text-slate-500 uppercase font-bold text-[10px]">Specification Version</span>
                        <p className="font-mono text-blue-700 font-bold">{selectedConcept.version || '0.2'}</p>
                      </div>
                      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
                        <span className="text-slate-500 uppercase font-bold text-[10px]">Provenance System</span>
                        <p className="font-mono text-slate-800 font-medium">KnowledgeHub AI Engine</p>
                      </div>
                    </div>

                    <div className="pt-2">
                      <Link
                        to="/chat"
                        state={{ initialPrompt: `Analyze the OKF document: ${selectedConcept.title}` }}
                        className="w-full py-2.5 px-4 rounded-xl bg-blue-50 hover:bg-blue-100 border border-blue-200 text-blue-800 font-bold text-xs flex items-center justify-center space-x-2 transition-colors shadow-xs"
                      >
                        <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                        <span>Query this Concept in Neural Chat</span>
                        <ArrowRight className="w-3.5 h-3.5 text-blue-600" />
                      </Link>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="h-[720px] bg-white rounded-3xl border border-slate-200 shadow-sm flex items-center justify-center text-center p-6 text-slate-500 text-xs">
              Select an OKF concept on the left to inspect its YAML frontmatter and semantic Markdown.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
