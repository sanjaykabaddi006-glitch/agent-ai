import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Search, Bell, X, FileText, BookOpen, MessageSquare, ArrowRight, Sparkles } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import { useNavigate } from 'react-router-dom';
import { DocumentItem, ConceptItem, PageResponse } from '../../types';

export const Navbar: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [searchTerm, setSearchTerm] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Queries for live search results
  const { data: docData } = useQuery<PageResponse<DocumentItem>>({
    queryKey: ['navbarSearchDocs', searchTerm],
    queryFn: () => api.documents.list(searchTerm.trim(), 0, 5),
    enabled: searchTerm.trim().length > 1,
  });

  const { data: concepts } = useQuery({
    queryKey: ['navbarSearchConcepts'],
    queryFn: () => api.knowledge.listConcepts(),
    staleTime: 60000,
  });

  // Filter concepts based on search term
  const matchedConcepts = React.useMemo(() => {
    if (!searchTerm.trim() || !concepts) return [];
    const q = searchTerm.toLowerCase();
    return concepts.filter(
      (c: ConceptItem) =>
        c.title.toLowerCase().includes(q) ||
        c.description?.toLowerCase().includes(q) ||
        c.tags?.some((t) => t.toLowerCase().includes(q))
    ).slice(0, 4);
  }, [searchTerm, concepts]);

  // Global Ctrl+K / Cmd+K keyboard shortcut
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        inputRef.current?.focus();
        setIsOpen(true);
      }
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Click outside to close search dropdown
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearchSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!searchTerm.trim()) return;
    setIsOpen(false);
    navigate('/chat', { state: { initialPrompt: searchTerm.trim() } });
  };

  const handleSelectDoc = (id: string) => {
    setIsOpen(false);
    setSearchTerm('');
    navigate(`/documents/${id}`);
  };

  const handleSelectConcept = (id: string) => {
    setIsOpen(false);
    setSearchTerm('');
    navigate('/okf', { state: { selectedConceptId: id } });
  };

  const handleAskAi = () => {
    if (!searchTerm.trim()) return;
    setIsOpen(false);
    const query = searchTerm.trim();
    setSearchTerm('');
    navigate('/chat', { state: { initialPrompt: query } });
  };

  const initials = user?.fullName
    ? user.fullName
        .split(' ')
        .map((n) => n[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
    : 'JD';

  const hasResults =
    searchTerm.trim().length > 1 &&
    ((docData?.items && docData.items.length > 0) || matchedConcepts.length > 0);

  return (
    <header className="h-16 bg-[#f8fafc] px-6 flex items-center justify-between sticky top-0 z-40 shrink-0 select-none">
      {/* 1. Global Interactive Search Bar */}
      <div ref={searchRef} className="flex-1 max-w-xl relative">
        <form onSubmit={handleSearchSubmit} className="relative flex items-center">
          <div className="relative flex items-center w-full bg-white border border-slate-200/90 focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-blue-500/20 rounded-full px-4 py-2 shadow-xs transition-all">
            <Search className="w-4 h-4 text-slate-400 mr-2.5 shrink-0" />
            <input
              ref={inputRef}
              type="text"
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setIsOpen(true);
              }}
              onFocus={() => setIsOpen(true)}
              placeholder="Search documents, concepts, or ask a question..."
              className="w-full text-xs sm:text-sm text-slate-800 placeholder-slate-400 bg-transparent focus:outline-none font-medium"
            />

            {searchTerm ? (
              <button
                type="button"
                onClick={() => {
                  setSearchTerm('');
                  inputRef.current?.focus();
                }}
                className="p-1 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors mr-1.5"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            ) : null}

            <div className="flex items-center space-x-1 px-2 py-0.5 rounded-lg bg-slate-100 border border-slate-200 text-[11px] font-mono font-medium text-slate-500 shrink-0">
              <span>⌘</span>
              <span>K</span>
            </div>
          </div>
        </form>

        {/* Live Search Results Dropdown */}
        {isOpen && searchTerm.trim().length > 0 && (
          <div className="absolute top-12 left-0 right-0 bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden z-50 p-2 space-y-2 max-h-96 overflow-y-auto animate-in fade-in slide-in-from-top-2 duration-150">
            {/* Ask AI in Chat Option */}
            <div
              onClick={handleAskAi}
              className="p-3 rounded-xl bg-blue-50/80 hover:bg-blue-100/80 border border-blue-200/80 text-xs flex items-center justify-between cursor-pointer transition-colors group"
            >
              <div className="flex items-center space-x-2.5 min-w-0">
                <div className="w-7 h-7 rounded-lg bg-[#2d5cf6] text-white flex items-center justify-center shrink-0 shadow-sm shadow-blue-500/20">
                  <Sparkles className="w-3.5 h-3.5" />
                </div>
                <div className="min-w-0">
                  <span className="font-bold text-blue-950 truncate block">
                    Ask AI about "{searchTerm}"
                  </span>
                  <span className="text-[10px] text-blue-700">
                    Query your documents with citations
                  </span>
                </div>
              </div>
              <ArrowRight className="w-4 h-4 text-blue-600 group-hover:translate-x-0.5 transition-transform shrink-0" />
            </div>

            {/* Matching Documents Section */}
            {docData?.items && docData.items.length > 0 && (
              <div className="space-y-1 pt-1 border-t border-slate-100">
                <p className="px-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Documents ({docData.items.length})
                </p>
                {docData.items.map((doc: DocumentItem) => (
                  <div
                    key={doc.id}
                    onClick={() => handleSelectDoc(doc.id)}
                    className="p-2.5 rounded-xl hover:bg-slate-50 flex items-center justify-between cursor-pointer transition-colors group"
                  >
                    <div className="flex items-center space-x-2.5 min-w-0">
                      <div className="w-6 h-6 rounded-md bg-blue-50 text-blue-600 border border-blue-200 flex items-center justify-center shrink-0">
                        <FileText className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-xs font-bold text-slate-800 group-hover:text-blue-600 truncate">
                          {doc.title}
                        </p>
                        <p className="text-[10px] text-slate-400 truncate">
                          {doc.originalFilename} • {doc.chunkCount} chunks
                        </p>
                      </div>
                    </div>
                    <span className="text-[10px] font-mono text-slate-400 uppercase font-bold px-1.5 py-0.5 bg-slate-100 rounded">
                      {doc.fileType}
                    </span>
                  </div>
                ))}
              </div>
            )}

            {/* Matching OKF Concepts Section */}
            {matchedConcepts.length > 0 && (
              <div className="space-y-1 pt-1 border-t border-slate-100">
                <p className="px-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
                  Concepts & Topics ({matchedConcepts.length})
                </p>
                {matchedConcepts.map((c: ConceptItem) => (
                  <div
                    key={c.id}
                    onClick={() => handleSelectConcept(c.id)}
                    className="p-2.5 rounded-xl hover:bg-slate-50 flex items-center justify-between cursor-pointer transition-colors group"
                  >
                    <div className="flex items-center space-x-2.5 min-w-0">
                      <div className="w-6 h-6 rounded-md bg-purple-50 text-purple-600 border border-purple-200 flex items-center justify-center shrink-0">
                        <BookOpen className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-xs font-bold text-slate-800 group-hover:text-purple-600 truncate">
                          {c.title}
                        </p>
                        <p className="text-[10px] text-slate-400 truncate">
                          {c.description || 'Open Knowledge Format v0.2'}
                        </p>
                      </div>
                    </div>
                    <span className="text-[10px] text-blue-600 font-bold">
                      View
                    </span>
                  </div>
                ))}
              </div>
            )}

            {/* No matches fallback */}
            {!hasResults && (
              <div className="p-4 text-center text-xs text-slate-500">
                No matching documents found. Press Enter to ask AI directly.
              </div>
            )}
          </div>
        )}
      </div>

      {/* 2. Right Side: Ollama text is hidden as requested; Notification Bell & User Avatar preserved */}
      <div className="flex items-center space-x-3 ml-4">
        {/* Bell Notifications */}
        <button
          title="Notifications"
          className="w-9 h-9 rounded-full bg-white border border-slate-200 hover:border-slate-300 text-slate-600 hover:text-slate-900 flex items-center justify-center transition-colors shadow-xs"
        >
          <Bell className="w-4 h-4" />
        </button>

        {/* User Avatar Circle */}
        <div
          title={user?.fullName || 'User Profile'}
          className="w-9 h-9 rounded-full bg-[#2d5cf6] text-white flex items-center justify-center text-xs font-bold shadow-md shadow-blue-500/25 shrink-0 cursor-pointer"
        >
          {initials}
        </div>
      </div>
    </header>
  );
};
