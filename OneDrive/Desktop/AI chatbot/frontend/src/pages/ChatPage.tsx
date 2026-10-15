import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ChatSession, ChatMessage, Citation, DocumentItem } from '../types';
import {
  Plus,
  Send,
  Bot,
  Copy,
  Check,
  Paperclip,
  RefreshCw,
  X,
  FileText,
  ThumbsUp,
  ThumbsDown,
  ChevronDown,
  ChevronRight,
  ChevronLeft,
  ExternalLink,
  Layers,
  ArrowRight,
  BookOpen,
  Sparkles,
  Search,
  SlidersHorizontal,
  Compass,
  FileCheck2,
  FileStack,
  CheckCheck,
  Trash2,
  History,
  Clock,
  MessageSquare,
  Pencil,
  ClipboardPaste,
  Square
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';

export const ChatPage: React.FC = () => {
  const queryClient = useQueryClient();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [inputText, setInputText] = useState('');
  const [optimisticMessages, setOptimisticMessages] = useState<ChatMessage[]>([]);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [likedMap, setLikedMap] = useState<Record<string, 'up' | 'down' | null>>({});
  const [uploadingFile, setUploadingFile] = useState(false);
  const [uploadedDocNotification, setUploadedDocNotification] = useState<string | null>(null);
  const [attachedDoc, setAttachedDoc] = useState<{ id: string; title: string; fileType: string } | null>(null);
  const [showDocPicker, setShowDocPicker] = useState(false);
  const [selectedCitation, setSelectedCitation] = useState<Citation | null>(null);
  const [rightTab, setRightTab] = useState<'sources' | 'related' | 'metadata'>('sources');

  // Editing state for user questions and assistant answers
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null);
  const [editContent, setEditContent] = useState<string>('');

  // Past Worked History Panel state (hidden by default per user request)
  const [showHistory, setShowHistory] = useState<boolean>(false);
  const [historySearch, setHistorySearch] = useState<string>('');

  // RAG Retrieval Parameters
  const [topK, setTopK] = useState(5);
  const [threshold, setThreshold] = useState(0.20);
  const [hybridSearch, setHybridSearch] = useState(true);

  // Queries
  const { data: sessions } = useQuery({
    queryKey: ['chatSessions'],
    queryFn: () => api.chat.listSessions(),
  });

  const { data: docData } = useQuery({
    queryKey: ['documentsListPreview'],
    queryFn: () => api.documents.list('', 0, 10),
  });

  const { data: concepts } = useQuery({
    queryKey: ['conceptsListPreview'],
    queryFn: () => api.knowledge.listConcepts(),
  });

  // Handle passed router state (e.g., from Dashboard or Documents page)
  useEffect(() => {
    if (location.state?.sessionId) {
      setActiveSessionId(location.state.sessionId);
    }
    if (location.state?.initialPrompt) {
      setInputText(location.state.initialPrompt);
    }
    if (location.state?.attachedDoc) {
      setAttachedDoc(location.state.attachedDoc);
      setUploadedDocNotification(`✅ Attached "${location.state.attachedDoc.title}". Questions will be answered strictly from this file.`);
    }
    if (location.search.includes('view=history') || location.state?.openHistory) {
      setShowHistory(true);
    }
  }, [location.state, location.search]);

  // Select first session if none selected
  useEffect(() => {
    if (!activeSessionId && sessions && sessions.length > 0) {
      setActiveSessionId(sessions[0].id);
    }
  }, [sessions, activeSessionId]);

  // Format date helper for history
  const formatSessionDate = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      const diffHours = Math.floor(diffMins / 60);
      const diffDays = Math.floor(diffHours / 24);

      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      if (diffHours < 24) return `${diffHours}h ago`;
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays}d ago`;
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } catch {
      return '';
    }
  };

  // Filtered sessions for history search
  const filteredSessions = React.useMemo(() => {
    if (!sessions) return [];
    if (!historySearch.trim()) return sessions;
    const term = historySearch.toLowerCase().trim();
    return sessions.filter((s) => s.title.toLowerCase().includes(term));
  }, [sessions, historySearch]);

  // Messages Query
  const { data: messages, isLoading: messagesLoading } = useQuery({
    queryKey: ['chatMessages', activeSessionId],
    queryFn: () => (activeSessionId ? api.chat.getMessages(activeSessionId) : Promise.resolve([])),
    enabled: !!activeSessionId,
  });

  // Merged messages: server messages plus any pending optimistic user question
  const displayedMessages: ChatMessage[] = React.useMemo(() => {
    const serverMsgs = messages || [];
    if (optimisticMessages.length === 0) return serverMsgs;
    const serverIds = new Set(serverMsgs.map((m) => m.id));
    const pending = optimisticMessages.filter((opt) => !serverIds.has(opt.id));
    return [...serverMsgs, ...pending];
  }, [messages, optimisticMessages]);

  // Automatically pick the latest assistant citation for preview
  useEffect(() => {
    if (displayedMessages.length > 0) {
      const lastAssistant = [...displayedMessages].reverse().find((m) => m.role === 'ASSISTANT' && m.citations && m.citations.length > 0);
      if (lastAssistant && lastAssistant.citations && lastAssistant.citations.length > 0) {
        setSelectedCitation(lastAssistant.citations[0]);
      }
    }
  }, [displayedMessages]);

  // Create Session Mutation
  const createSessionMutation = useMutation({
    mutationFn: (title?: string) => api.chat.createSession(title || 'New Conversation'),
    onSuccess: (newSession: ChatSession) => {
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      setActiveSessionId(newSession.id);
      setInputText('');
      setOptimisticMessages([]);
    },
  });

  // Delete Session Mutation
  const deleteSessionMutation = useMutation({
    mutationFn: (sessionId: string) => api.chat.deleteSession(sessionId),
    onSuccess: (_, deletedSessionId) => {
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      if (activeSessionId === deletedSessionId) {
        const remaining = sessions?.filter((s) => s.id !== deletedSessionId);
        if (remaining && remaining.length > 0) {
          setActiveSessionId(remaining[0].id);
        } else {
          createSessionMutation.mutate('New Conversation');
        }
      }
    },
  });

  // Send Message Mutation with AbortSignal support
  const sendMessageMutation = useMutation({
    mutationFn: ({
      sessionId,
      content,
      documentId,
      signal,
    }: {
      sessionId: string;
      content: string;
      documentId?: string;
      signal?: AbortSignal;
    }) => {
      return api.chat.sendMessage(sessionId, content, {
        documentId,
        topK,
        similarityThreshold: threshold,
        hybridSearch,
        signal,
      });
    },
    onSuccess: (_, variables) => {
      setOptimisticMessages([]);
      abortControllerRef.current = null;
      queryClient.invalidateQueries({ queryKey: ['chatMessages', variables.sessionId] });
      queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
    },
    onError: (err: any) => {
      abortControllerRef.current = null;
      if (err?.name === 'AbortError' || err?.message?.toLowerCase().includes('abort')) {
        return;
      }
      setOptimisticMessages([]);
    },
  });

  const handleStop = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    sendMessageMutation.reset();
  };

  // Update Message Mutation (for editing user questions or assistant answers)
  const updateMessageMutation = useMutation({
    mutationFn: ({ sessionId, messageId, content }: { sessionId: string; messageId: string; content: string }) => {
      return api.chat.updateMessage(sessionId, messageId, content);
    },
    onSuccess: (updatedMsg, variables) => {
      setEditingMessageId(null);
      setEditContent('');
      queryClient.setQueryData(['chatMessages', variables.sessionId], (old: ChatMessage[] | undefined) => {
        if (!old) return [updatedMsg];
        return old.map((m) => (m.id === variables.messageId ? { ...m, content: variables.content } : m));
      });
      queryClient.invalidateQueries({ queryKey: ['chatMessages', variables.sessionId] });
    },
  });

  const handleStartEdit = (message: ChatMessage) => {
    setEditingMessageId(message.id);
    setEditContent(message.content);
  };

  const handleCancelEdit = () => {
    setEditingMessageId(null);
    setEditContent('');
  };

  const handleSaveEdit = (message: ChatMessage) => {
    if (!activeSessionId || !editContent.trim()) return;
    updateMessageMutation.mutate({
      sessionId: activeSessionId,
      messageId: message.id,
      content: editContent.trim(),
    });
  };

  const handleSaveAndReask = (message: ChatMessage) => {
    const newText = editContent.trim();
    if (!newText) return;
    handleCancelEdit();
    if (activeSessionId) {
      updateMessageMutation.mutate({
        sessionId: activeSessionId,
        messageId: message.id,
        content: newText,
      });
    }
    handleSend(newText);
  };

  // Scroll to bottom on new messages or pending request
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [displayedMessages, uploadingFile, sendMessageMutation.isPending]);

  const handleSend = async (textToSend?: string) => {
    const text = textToSend || inputText;
    if (!text.trim() || sendMessageMutation.isPending) return;

    let targetSessionId = activeSessionId;

    if (!targetSessionId) {
      try {
        const newSession = await api.chat.createSession('New Conversation');
        targetSessionId = newSession.id;
        setActiveSessionId(newSession.id);
        queryClient.invalidateQueries({ queryKey: ['chatSessions'] });
      } catch {
        targetSessionId = `ses-${Date.now()}`;
      }
    }

    const trimmed = text.trim();
    // Optimistically show user question immediately on screen
    const optimisticMsg: ChatMessage = {
      id: `temp-${Date.now()}`,
      sessionId: targetSessionId,
      role: 'USER',
      content: trimmed,
      createdAt: new Date().toISOString(),
    };
    setOptimisticMessages((prev) => [...prev, optimisticMsg]);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    setInputText('');
    setUploadedDocNotification(null);
    sendMessageMutation.mutate({
      sessionId: targetSessionId,
      content: trimmed,
      documentId: attachedDoc?.id,
      signal: controller.signal,
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  // Direct Document Upload from inside Chat
  const handleChatFileUpload = async (file: File) => {
    try {
      setUploadingFile(true);
      const doc = await api.documents.upload(file);
      queryClient.invalidateQueries({ queryKey: ['documents'] });
      queryClient.invalidateQueries({ queryKey: ['documentsListPreview'] });
      queryClient.invalidateQueries({ queryKey: ['systemStatus'] });
      setAttachedDoc({ id: doc.id, title: doc.title, fileType: doc.fileType });
      setUploadedDocNotification(`✅ Attached "${doc.title}.${doc.fileType}". Questions will be answered strictly from this file.`);
      setInputText('');
    } catch (err: any) {
      setUploadedDocNotification(`Upload failed: ${err.message || 'Error processing document'}`);
    } finally {
      setUploadingFile(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // Quick Action / Starter Cards
  const quickActions = [
    {
      title: 'Full Document Content',
      subtitle: 'Give complete file content without shortening',
      prompt: 'Give that file full content. Provide all details and every section fully without shortening.',
      icon: FileText,
      iconColor: 'bg-indigo-50 text-indigo-600 border-indigo-100',
    },
    {
      title: 'Compare two documents',
      subtitle: 'Find differences and similarities',
      prompt: 'Compare the architecture and security specifications between the uploaded documents.',
      icon: FileStack,
      iconColor: 'bg-blue-50 text-blue-600 border-blue-100',
    },
    {
      title: 'Find a process or procedure',
      subtitle: 'Locate step-by-step instructions',
      prompt: 'What are the step-by-step procedures outlined for data compliance and verification?',
      icon: Compass,
      iconColor: 'bg-cyan-50 text-cyan-600 border-cyan-100',
    },
    {
      title: 'Explain a concept',
      subtitle: 'Get detailed explanation from your knowledge base',
      prompt: 'Explain the core OKF v0.2 concept definitions and retrieval architecture.',
      icon: BookOpen,
      iconColor: 'bg-purple-50 text-purple-600 border-purple-100',
    },
  ];

  const greetingName = user?.fullName ? user.fullName.split(' ')[0] : 'John';

  const userInitials = user?.fullName
    ? user.fullName
        .split(' ')
        .map((n) => n[0])
        .slice(0, 2)
        .join('')
        .toUpperCase()
    : 'JD';

  // Active citations list from messages or fallback documents
  const allActiveCitations: Citation[] = React.useMemo(() => {
    const list: Citation[] = [];
    messages?.forEach((m) => {
      m.citations?.forEach((c) => {
        if (!list.some((existing) => existing.documentId === c.documentId && existing.chunkIndex === c.chunkIndex)) {
          list.push(c);
        }
      });
    });
    if (list.length > 0) return list;

    // Default preview sources from docs if no messages yet
    return (
      docData?.items.slice(0, 3).map((d, idx) => ({
        documentId: d.id,
        documentTitle: d.originalFilename || `${d.title}.${d.fileType}`,
        pageNumber: idx + 2,
        sectionName: idx === 0 ? '2.4 Security Policies' : idx === 1 ? 'Access Control' : '3. Security Requirements',
        chunkIndex: idx,
        snippet: idx === 0
          ? 'The organization implements a role-based access control (RBAC) model to manage system access. All users must be authenticated and authorized before accessing sensitive resources.'
          : 'Security configurations mandate multi-factor authentication (MFA) and encrypted transmission channels across all services.',
        similarityScore: 0.88 - idx * 0.05,
      })) || []
    );
  }, [messages, docData]);

  // Concept tags
  const conceptTags = [
    { label: 'security', color: 'bg-purple-100 text-purple-700' },
    { label: 'access control', color: 'bg-sky-100 text-sky-800' },
    { label: 'authentication', color: 'bg-emerald-100 text-emerald-800' },
    { label: 'data protection', color: 'bg-cyan-100 text-cyan-800' },
    { label: 'incident response', color: 'bg-indigo-100 text-indigo-800' },
  ];

  return (
    <div className="flex gap-4 h-[calc(100vh-5.5rem)] select-none">
      {/* ============================================================== */}
      {/* 1. LEFT SIDEBAR: Past Worked History */}
      {/* ============================================================== */}
      {showHistory && (
        <aside className="w-72 sm:w-80 flex flex-col bg-white border border-slate-200/90 rounded-3xl p-3.5 shadow-xs shrink-0 select-none overflow-hidden transition-all duration-300">
          {/* Header */}
          <div className="flex items-center justify-between pb-3 border-b border-slate-100 px-1">
            <div className="flex items-center space-x-2 min-w-0">
              <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
                <History className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <h2 className="text-xs font-bold text-slate-900 tracking-tight flex items-center space-x-1.5">
                  <span className="truncate">Past Worked History</span>
                  <span className="px-1.5 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[10px] font-mono font-semibold">
                    {sessions?.length || 0}
                  </span>
                </h2>
                <p className="text-[10px] text-slate-400 truncate">Previous conversations</p>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setShowHistory(false)}
              className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors cursor-pointer"
              title="Collapse History"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
          </div>

          {/* New Conversation Button */}
          <div className="pt-2.5 pb-2">
            <button
              onClick={() => createSessionMutation.mutate('New Conversation')}
              disabled={createSessionMutation.isPending}
              className="w-full py-2 px-3 rounded-xl bg-[#2d5cf6] hover:bg-[#2551e8] active:scale-[0.98] text-white text-xs font-semibold flex items-center justify-center space-x-1.5 transition-all shadow-sm shadow-blue-500/25 cursor-pointer disabled:opacity-50"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>New Conversation</span>
            </button>
          </div>

          {/* Search Input Filter */}
          <div className="relative mb-2">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={historySearch}
              onChange={(e) => setHistorySearch(e.target.value)}
              placeholder="Search past history..."
              className="w-full pl-8 pr-7 py-1.5 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-slate-800 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-blue-400 font-medium transition-all"
            />
            {historySearch && (
              <button
                onClick={() => setHistorySearch('')}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-0.5"
              >
                <X className="w-3 h-3" />
              </button>
            )}
          </div>

          {/* Sessions List */}
          <div className="flex-1 overflow-y-auto space-y-1 pr-0.5 pt-1">
            {filteredSessions.length > 0 ? (
              filteredSessions.map((s) => {
                const isActive = s.id === activeSessionId;
                return (
                  <div
                    key={s.id}
                    onClick={() => {
                      setActiveSessionId(s.id);
                      setOptimisticMessages([]);
                    }}
                    className={`group relative p-2.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                      isActive
                        ? 'bg-gradient-to-r from-blue-50 to-indigo-50/60 border-blue-300 shadow-2xs text-blue-950 font-semibold'
                        : 'bg-white hover:bg-slate-50/90 border-transparent hover:border-slate-200 text-slate-700'
                    }`}
                  >
                    <div className="flex items-center space-x-2.5 min-w-0 flex-1">
                      <div
                        className={`w-2 h-2 rounded-full shrink-0 ${
                          isActive ? 'bg-blue-600 ring-2 ring-blue-300' : 'bg-slate-300 group-hover:bg-slate-400'
                        }`}
                      />
                      <div className="min-w-0 flex-1">
                        <p className={`text-xs truncate ${isActive ? 'font-bold text-blue-900' : 'font-medium text-slate-800'}`}>
                          {s.title}
                        </p>
                        <div className="flex items-center space-x-2 text-[10px] text-slate-400 mt-0.5 font-mono">
                          <span>{formatSessionDate(s.updatedAt || s.createdAt)}</span>
                          {s.messageCount !== undefined && (
                            <span className="px-1.5 py-0.2 rounded bg-slate-100 text-slate-500 font-medium">
                              {s.messageCount} {s.messageCount === 1 ? 'msg' : 'msgs'}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        if (window.confirm(`Delete "${s.title}" from history?`)) {
                          deleteSessionMutation.mutate(s.id);
                        }
                      }}
                      className="opacity-0 group-hover:opacity-100 p-1 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-all ml-1 shrink-0 cursor-pointer"
                      title="Delete chat session"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                );
              })
            ) : (
              <div className="py-12 text-center text-slate-400 text-xs px-2">
                <History className="w-6 h-6 mx-auto mb-2 text-slate-300" />
                <p className="font-medium text-slate-600">No past conversations</p>
                <p className="text-[11px] text-slate-400 mt-1">Start chatting or upload a file to record history.</p>
              </div>
            )}
          </div>

          {/* Footer note */}
          <div className="pt-2 border-t border-slate-100 text-[10px] text-slate-400 text-center font-medium">
            Protected & Grounded History
          </div>
        </aside>
      )}

      {/* ============================================================== */}
      {/* 2. CENTER WORKSPACE: History Bar, Greeting Banner, Chat Feed, and Input Bar */}
      {/* ============================================================== */}
      <div className="flex-1 flex flex-col justify-between min-w-0 bg-transparent">
        {/* Top Session & History Bar */}
        <div className="flex items-center justify-between bg-white border border-slate-200/80 rounded-2xl px-4 py-2 text-xs shadow-2xs mb-2">
          <div className="flex items-center space-x-3 min-w-0">
            <button
              type="button"
              onClick={() => setShowHistory(!showHistory)}
              className={`px-3 py-1.5 rounded-xl border flex items-center space-x-1.5 font-semibold transition-all cursor-pointer ${
                showHistory
                  ? 'bg-blue-50 border-blue-200 text-blue-700'
                  : 'bg-slate-50 hover:bg-slate-100 border-slate-200 text-slate-700'
              }`}
              title="Toggle Past Worked History"
            >
              <History className="w-3.5 h-3.5 text-blue-600" />
              <span>{showHistory ? 'Hide History' : 'Show History'}</span>
              <span className="w-4 h-4 rounded-full bg-blue-600 text-white text-[10px] flex items-center justify-center font-bold">
                {sessions?.length || 0}
              </span>
            </button>

            <div className="hidden sm:flex items-center space-x-1.5 text-slate-500 truncate min-w-0">
              <span className="text-[11px] text-slate-400">Current:</span>
              <span className="font-semibold text-slate-800 truncate">
                {sessions?.find((s) => s.id === activeSessionId)?.title || 'Active Session'}
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-2 shrink-0">
            {attachedDoc && (
              <div className="flex items-center space-x-1.5 bg-blue-50 text-blue-700 border border-blue-200 px-2.5 py-1 rounded-xl text-[11px] font-semibold">
                <FileText className="w-3 h-3 text-blue-600" />
                <span className="truncate max-w-[140px] sm:max-w-[200px]">{attachedDoc.title}</span>
              </div>
            )}
            <button
              onClick={() => createSessionMutation.mutate('New Conversation')}
              disabled={createSessionMutation.isPending}
              className="px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-black text-white text-[11px] font-semibold flex items-center space-x-1 transition-all cursor-pointer"
            >
              <Plus className="w-3 h-3" />
              <span>New Chat</span>
            </button>
          </div>
        </div>

        {/* Messages & Content Area */}
        <div className="flex-1 overflow-y-auto pr-1 space-y-5 pb-3">
          {/* 1. Hero Greeting Banner */}
          <div className="relative rounded-3xl bg-gradient-to-r from-[#eff4fe] via-[#f5f8ff] to-[#e8f0fe] border border-[#dbe6fd] p-6 sm:p-7 overflow-hidden shadow-xs">
            {/* Artistic Landscape/Mountain Aura Vector in Background */}
            <div className="absolute right-0 top-0 bottom-0 w-80 pointer-events-none opacity-85 hidden sm:block">
              <svg viewBox="0 0 320 180" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-full h-full object-cover">
                <path d="M120 180L190 70L240 130L290 50L350 180H120Z" fill="url(#mount-grad-1)" fillOpacity="0.45" />
                <path d="M80 180L160 90L220 150L260 85L320 180H80Z" fill="url(#mount-grad-2)" fillOpacity="0.65" />
                <path d="M160 90L190 130L220 90L250 180H120L160 90Z" fill="url(#mount-grad-3)" fillOpacity="0.4" />
                <defs>
                  <linearGradient id="mount-grad-1" x1="120" y1="50" x2="350" y2="180" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#818cf8" />
                    <stop offset="1" stopColor="#38bdf8" />
                  </linearGradient>
                  <linearGradient id="mount-grad-2" x1="80" y1="85" x2="320" y2="180" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#6366f1" />
                    <stop offset="1" stopColor="#c084fc" />
                  </linearGradient>
                  <linearGradient id="mount-grad-3" x1="120" y1="90" x2="250" y2="180" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#ffffff" stopOpacity="0.8" />
                    <stop offset="1" stopColor="#93c5fd" stopOpacity="0.2" />
                  </linearGradient>
                </defs>
              </svg>
            </div>

            {/* Header Content */}
            <div className="relative z-10 flex items-start justify-between">
              <div>
                <h1 className="text-2xl sm:text-[26px] font-extrabold text-slate-900 tracking-tight">
                  Good morning, {greetingName} 👋
                </h1>
                <p className="text-xs sm:text-sm text-slate-600 mt-1 font-medium">
                  Ask questions about your documents and get AI-powered answers with source citations.
                </p>
              </div>

              {/* + New Chat Button */}
              <button
                onClick={() => createSessionMutation.mutate('New Conversation')}
                disabled={createSessionMutation.isPending}
                className="px-4 py-2 rounded-xl bg-[#2d5cf6] hover:bg-[#2551e8] active:scale-95 text-white text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-sm shadow-blue-500/25 shrink-0 cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>New Chat</span>
              </button>
            </div>

            {/* 4 Action / Quick Prompt Cards */}
            <div className="relative z-10 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 mt-5">
              {quickActions.map((qa, idx) => {
                const Icon = qa.icon;
                return (
                  <button
                    key={idx}
                    onClick={() => handleSend(qa.prompt)}
                    className="p-3.5 rounded-2xl bg-white border border-slate-200/90 hover:border-blue-400 hover:shadow-sm text-left transition-all duration-200 group cursor-pointer"
                  >
                    <div className="flex items-center space-x-3 mb-2">
                      <div className={`w-8 h-8 rounded-xl flex items-center justify-center border shrink-0 ${qa.iconColor}`}>
                        <Icon className="w-4 h-4" />
                      </div>
                      <span className="text-xs font-bold text-slate-900 group-hover:text-blue-600 transition-colors truncate">
                        {qa.title}
                      </span>
                    </div>
                    <p className="text-[11px] text-slate-500 leading-relaxed font-normal">
                      {qa.subtitle}
                    </p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* 2. Messages Stream */}
          {messagesLoading && displayedMessages.length === 0 ? (
            <div className="py-16 text-center text-slate-400 text-xs">
              <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
              Loading conversation...
            </div>
          ) : displayedMessages && displayedMessages.length > 0 ? (
            displayedMessages.map((m: ChatMessage) => {
              const isUser = m.role === 'USER';
              return (
                <div key={m.id} className="space-y-4">
                  {isUser ? (
                    /* User Bubble with Edit & Copy */
                    <div className="flex items-start justify-end space-x-2.5 group">
                      {editingMessageId === m.id ? (
                        /* User Message Edit Form */
                        <div className="w-full max-w-xl bg-white border-2 border-blue-400 rounded-2xl p-4 shadow-md space-y-3">
                          <div className="flex items-center justify-between text-xs text-slate-700 font-semibold border-b border-slate-100 pb-2">
                            <span className="flex items-center space-x-1.5 text-blue-600">
                              <Pencil className="w-3.5 h-3.5" />
                              <span>Edit your question</span>
                            </span>
                            <button
                              type="button"
                              onClick={handleCancelEdit}
                              className="text-slate-400 hover:text-slate-600 p-1 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
                              title="Cancel editing"
                            >
                              <X className="w-4 h-4" />
                            </button>
                          </div>
                          <textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            rows={3}
                            autoFocus
                            className="w-full text-xs sm:text-sm text-slate-800 bg-slate-50 border border-slate-200 rounded-xl p-3 focus:outline-none focus:border-blue-500 focus:bg-white resize-none"
                            placeholder="Edit your question..."
                          />
                          <div className="flex items-center justify-end space-x-2 pt-1">
                            <button
                              type="button"
                              onClick={handleCancelEdit}
                              className="px-3 py-1.5 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 text-xs font-medium transition-colors cursor-pointer"
                            >
                              Cancel
                            </button>
                            <button
                              type="button"
                              onClick={() => handleSaveEdit(m)}
                              disabled={updateMessageMutation.isPending || !editContent.trim()}
                              className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold flex items-center space-x-1 transition-colors cursor-pointer disabled:opacity-50"
                              title="Save without re-asking"
                            >
                              <Check className="w-3.5 h-3.5 text-slate-600" />
                              <span>Save Only</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => handleSaveAndReask(m)}
                              disabled={updateMessageMutation.isPending || !editContent.trim()}
                              className="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold flex items-center space-x-1.5 shadow-sm transition-all cursor-pointer disabled:opacity-50"
                              title="Save and get a new AI response"
                            >
                              <Send className="w-3.5 h-3.5" />
                              <span>Save & Re-ask</span>
                            </button>
                          </div>
                        </div>
                      ) : (
                        /* Normal User Message Display */
                        <div className="flex flex-col items-end space-y-1 max-w-xl">
                          <div className="bg-[#eef4ff] text-slate-800 border border-blue-200/70 rounded-2xl rounded-tr-xs px-4 py-3 shadow-2xs text-xs sm:text-sm font-medium w-full">
                            <p className="leading-relaxed whitespace-pre-wrap">{m.content}</p>
                            <div className="text-[10px] text-blue-600 flex items-center justify-end space-x-1 mt-1 font-mono">
                              <span>{new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                              <CheckCheck className="w-3.5 h-3.5 text-blue-500" />
                            </div>
                          </div>

                          {/* Quick Actions (Copy & Edit) for User Question */}
                          <div className="flex items-center space-x-1.5 opacity-90 sm:opacity-0 sm:group-hover:opacity-100 transition-opacity duration-200 pr-1">
                            <button
                              type="button"
                              onClick={() => handleCopy(m.content, m.id)}
                              className="px-2 py-0.5 rounded-lg bg-white border border-slate-200/90 hover:bg-blue-50 hover:border-blue-300 text-slate-600 flex items-center space-x-1 text-[11px] font-medium transition-colors shadow-2xs cursor-pointer"
                              title="Copy question"
                            >
                              {copiedId === m.id ? (
                                <>
                                  <Check className="w-3 h-3 text-emerald-600" />
                                  <span className="text-emerald-700 font-semibold text-[10px]">Copied</span>
                                </>
                              ) : (
                                <>
                                  <Copy className="w-3 h-3 text-slate-400" />
                                  <span className="text-[10px]">Copy</span>
                                </>
                              )}
                            </button>

                            <button
                              type="button"
                              onClick={() => handleStartEdit(m)}
                              className="px-2 py-0.5 rounded-lg bg-white border border-slate-200/90 hover:bg-blue-50 hover:border-blue-300 text-slate-600 flex items-center space-x-1 text-[11px] font-medium transition-colors shadow-2xs cursor-pointer"
                              title="Edit question"
                            >
                              <Pencil className="w-3 h-3 text-slate-400" />
                              <span className="text-[10px]">Edit</span>
                            </button>
                          </div>
                        </div>
                      )}

                      <div className="w-8 h-8 rounded-full bg-[#2d5cf6] text-white flex items-center justify-center text-xs font-bold shrink-0 shadow-md shadow-blue-500/25">
                        {userInitials}
                      </div>
                    </div>
                  ) : (
                    /* Assistant Message Card with Edit & Copy */
                    <div className="flex items-start space-x-3">
                      <div className="w-8 h-8 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 p-[1.5px] shadow-md shadow-blue-500/25 shrink-0 mt-1">
                        <div className="w-full h-full bg-white rounded-[14px] flex items-center justify-center">
                          <Bot className="w-4 h-4 text-blue-600" />
                        </div>
                      </div>

                      <div className="flex-1 bg-white border border-slate-200/90 rounded-3xl p-6 shadow-xs space-y-4 max-w-3xl">
                        {editingMessageId === m.id ? (
                          /* Assistant Inline Edit Form */
                          <div className="space-y-3">
                            <div className="flex items-center justify-between text-xs text-slate-700 font-semibold border-b border-slate-100 pb-2">
                              <span className="flex items-center space-x-1.5 text-blue-600">
                                <Pencil className="w-3.5 h-3.5" />
                                <span>Edit assistant response</span>
                              </span>
                              <button
                                type="button"
                                onClick={handleCancelEdit}
                                className="text-slate-400 hover:text-slate-600 p-1 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
                                title="Cancel editing"
                              >
                                <X className="w-4 h-4" />
                              </button>
                            </div>
                            <textarea
                              value={editContent}
                              onChange={(e) => setEditContent(e.target.value)}
                              rows={8}
                              autoFocus
                              className="w-full text-xs sm:text-sm text-slate-800 bg-slate-50 border border-slate-200 rounded-xl p-3 focus:outline-none focus:border-blue-500 focus:bg-white resize-y font-mono"
                              placeholder="Edit response content..."
                            />
                            <div className="flex items-center justify-end space-x-2 pt-1">
                              <button
                                type="button"
                                onClick={handleCancelEdit}
                                className="px-3 py-1.5 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 text-xs font-medium transition-colors cursor-pointer"
                              >
                                Cancel
                              </button>
                              <button
                                type="button"
                                onClick={() => handleSaveEdit(m)}
                                disabled={updateMessageMutation.isPending || !editContent.trim()}
                                className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold flex items-center space-x-1.5 shadow-sm transition-all cursor-pointer disabled:opacity-50"
                              >
                                <Check className="w-3.5 h-3.5" />
                                <span>Save Changes</span>
                              </button>
                            </div>
                          </div>
                        ) : (
                          /* Markdown Text Response */
                          <div className="markdown-content text-xs sm:text-sm text-slate-800 leading-relaxed">
                            <ReactMarkdown>{m.content}</ReactMarkdown>
                          </div>
                        )}

                        {/* Action Bar Footer */}
                        <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs text-slate-500">
                          <div className="flex items-center space-x-2">
                            {/* Copy Option */}
                            <button
                              onClick={() => handleCopy(m.content, m.id)}
                              className="px-2.5 py-1 rounded-lg hover:bg-slate-100 text-slate-600 flex items-center space-x-1.5 text-[11px] font-medium transition-colors cursor-pointer"
                              title="Copy answer"
                            >
                              {copiedId === m.id ? (
                                <>
                                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                                  <span className="text-emerald-700 font-bold">Copied</span>
                                </>
                              ) : (
                                <>
                                  <Copy className="w-3.5 h-3.5 text-slate-400" />
                                  <span>Copy</span>
                                </>
                              )}
                            </button>

                            {/* Edit Option */}
                            <button
                              onClick={() => handleStartEdit(m)}
                              className="px-2.5 py-1 rounded-lg hover:bg-slate-100 text-slate-600 flex items-center space-x-1.5 text-[11px] font-medium transition-colors cursor-pointer"
                              title="Edit answer"
                            >
                              <Pencil className="w-3.5 h-3.5 text-slate-400" />
                              <span>Edit</span>
                            </button>

                            <button
                              onClick={() => setLikedMap((prev) => ({ ...prev, [m.id]: 'up' }))}
                              className={`p-1.5 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer ${
                                likedMap[m.id] === 'up' ? 'text-blue-600 font-bold' : 'text-slate-400'
                              }`}
                            >
                              <ThumbsUp className="w-3.5 h-3.5" />
                            </button>

                            <button
                              onClick={() => setLikedMap((prev) => ({ ...prev, [m.id]: 'down' }))}
                              className={`p-1.5 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer ${
                                likedMap[m.id] === 'down' ? 'text-rose-600 font-bold' : 'text-slate-400'
                              }`}
                            >
                              <ThumbsDown className="w-3.5 h-3.5" />
                            </button>

                            <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 text-[10px] font-medium">
                              Good answer
                            </span>
                          </div>

                          <span className="text-[10px] text-slate-400 font-mono">
                            {new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              );
            })
          ) : null}

          {/* Assistant Generating Indicator with Stop Option */}
          {sendMessageMutation.isPending && (
            <div className="flex items-center space-x-3 text-xs text-slate-600">
              <div className="w-8 h-8 rounded-2xl bg-blue-600 flex items-center justify-center shrink-0 animate-pulse text-white shadow-sm">
                <Bot className="w-4 h-4" />
              </div>
              <div className="p-3.5 sm:p-4 rounded-3xl bg-white border border-slate-200/90 rounded-tl-none flex items-center justify-between shadow-xs max-w-xl w-full">
                <div className="flex items-center space-x-3 min-w-0">
                  <div className="w-2.5 h-2.5 rounded-full bg-blue-600 animate-ping shrink-0" />
                  <span className="font-semibold text-slate-800 text-xs truncate">
                    Searching knowledge base and formulating grounded answer...
                  </span>
                </div>
                <button
                  type="button"
                  onClick={handleStop}
                  className="px-3 py-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-600 border border-rose-200 text-xs font-semibold flex items-center space-x-1.5 transition-all shadow-xs cursor-pointer ml-3 active:scale-95 shrink-0"
                  title="Stop searching and cancel answer"
                >
                  <Square className="w-3.5 h-3.5 fill-current" />
                  <span>Stop</span>
                </button>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* 3. Bottom Chat Input Area */}
        <div className="space-y-2 pt-2 relative">
          {/* Active Attached Document Scope Banner */}
          {attachedDoc && (
            <div className="flex items-center justify-between px-3 py-1.5 bg-blue-50 border border-blue-200 text-blue-900 rounded-xl text-xs font-medium shadow-xs">
              <div className="flex items-center space-x-2 min-w-0">
                <Paperclip className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                <span className="truncate">
                  Target File: <strong className="font-semibold text-blue-950">{attachedDoc.title}.{attachedDoc.fileType}</strong>
                </span>
                <span className="text-[10px] bg-blue-200 text-blue-800 font-semibold px-2 py-0.5 rounded-full shrink-0">
                  Answers scoped strictly to this file
                </span>
              </div>
              <button
                type="button"
                onClick={() => setAttachedDoc(null)}
                className="text-blue-500 hover:text-blue-800 p-0.5 hover:bg-blue-100 rounded transition-colors cursor-pointer ml-2"
                title="Detach file to allow cross-document search"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {uploadedDocNotification && !attachedDoc && (
            <div className="p-2.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between shadow-xs">
              <span className="font-medium truncate">{uploadedDocNotification}</span>
              <button
                onClick={() => setUploadedDocNotification(null)}
                className="text-emerald-700 hover:text-emerald-900 ml-2"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Document Picker Dropdown Popup */}
          {showDocPicker && (
            <div className="absolute bottom-16 left-2 z-50 w-72 bg-white rounded-2xl border border-slate-200 shadow-xl p-2 space-y-1 text-xs">
              <div className="flex items-center justify-between px-2 py-1 border-b border-slate-100 text-slate-500 font-semibold text-[11px]">
                <span>Select Target Document</span>
                <button
                  type="button"
                  onClick={() => setShowDocPicker(false)}
                  className="hover:text-slate-800 p-0.5"
                >
                  <X className="w-3 h-3" />
                </button>
              </div>

              {/* Upload new option */}
              <button
                type="button"
                onClick={() => {
                  setShowDocPicker(false);
                  fileInputRef.current?.click();
                }}
                className="w-full flex items-center space-x-2 px-2.5 py-2 rounded-xl text-left hover:bg-blue-50 text-blue-600 font-medium transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5 shrink-0" />
                <span>Upload new file from computer...</span>
              </button>

              {/* List existing uploaded docs */}
              {docData?.items && docData.items.length > 0 && (
                <div className="pt-1 border-t border-slate-100 max-h-48 overflow-y-auto space-y-0.5">
                  <div className="px-2 py-0.5 text-[10px] uppercase font-bold text-slate-400">
                    Your Uploaded Files:
                  </div>
                  {docData.items.map((docItem) => {
                    const isSelected = attachedDoc?.id === docItem.id;
                    return (
                      <button
                        key={docItem.id}
                        type="button"
                        onClick={() => {
                          setAttachedDoc({ id: docItem.id, title: docItem.title, fileType: docItem.fileType });
                          setShowDocPicker(false);
                          setUploadedDocNotification(null);
                        }}
                        className={`w-full flex items-center justify-between px-2.5 py-1.5 rounded-lg text-left transition-colors cursor-pointer ${
                          isSelected ? 'bg-blue-50 text-blue-700 font-semibold' : 'hover:bg-slate-50 text-slate-700'
                        }`}
                      >
                        <div className="flex items-center space-x-2 truncate">
                          <FileText className="w-3 h-3 text-slate-400 shrink-0" />
                          <span className="truncate">{docItem.title}.{docItem.fileType}</span>
                        </div>
                        {isSelected && <Check className="w-3 h-3 text-blue-600 shrink-0 ml-1" />}
                      </button>
                    );
                  })}
                </div>
              )}

              {/* Option to clear attachment */}
              {attachedDoc && (
                <div className="pt-1 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => {
                      setAttachedDoc(null);
                      setShowDocPicker(false);
                    }}
                    className="w-full flex items-center space-x-2 px-2.5 py-1.5 rounded-lg text-left text-slate-500 hover:bg-slate-50 hover:text-rose-600 transition-colors cursor-pointer"
                  >
                    <X className="w-3 h-3 text-slate-400 shrink-0" />
                    <span>Clear attachment (search all docs)</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Floating Pill Input Box */}
          <div className="relative flex items-center bg-white border border-slate-200/90 rounded-2xl px-3 py-2 shadow-sm focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500 transition-all">
            {/* Paperclip Button */}
            <button
              type="button"
              onClick={() => setShowDocPicker(!showDocPicker)}
              disabled={uploadingFile || sendMessageMutation.isPending}
              title="Attach or select a document to target Q&A"
              className={`p-2 rounded-xl transition-colors shrink-0 cursor-pointer disabled:opacity-50 mr-1 ${
                attachedDoc
                  ? 'bg-blue-100 text-blue-700 hover:bg-blue-200'
                  : 'text-slate-400 hover:text-blue-600 hover:bg-slate-100'
              }`}
            >
              {uploadingFile ? (
                <RefreshCw className="w-4 h-4 animate-spin text-blue-600" />
              ) : (
                <Paperclip className="w-4 h-4" />
              )}
            </button>
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => e.target.files?.[0] && handleChatFileUpload(e.target.files[0])}
              className="hidden"
              accept=".pdf,.ppt,.pptx,.docx,.txt,.md,.markdown,.csv"
            />

            {/* Input Textarea: auto-expanding, handles long copied paragraphs smoothly */}
            <textarea
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={handleKeyDown}
              rows={1}
              placeholder={attachedDoc ? `Ask a question about ${attachedDoc.title}...` : "Ask a question or paste text here..."}
              className="w-full text-xs sm:text-sm text-slate-800 placeholder-slate-400 bg-transparent focus:outline-none font-medium px-1 resize-none max-h-36 min-h-[26px] py-1 leading-relaxed"
            />

            {/* Quick Paste from Clipboard button */}
            <button
              type="button"
              onClick={async () => {
                try {
                  const clip = await navigator.clipboard.readText();
                  if (clip) {
                    setInputText((prev) => (prev ? prev + ' ' + clip : clip));
                  }
                } catch {
                  // Ignore if permission denied
                }
              }}
              title="Paste from clipboard"
              className="p-2 rounded-xl text-slate-400 hover:text-blue-600 hover:bg-slate-100 transition-colors shrink-0 cursor-pointer"
            >
              <ClipboardPaste className="w-4 h-4" />
            </button>

            {/* Send or Stop Button */}
            {sendMessageMutation.isPending ? (
              <button
                type="button"
                onClick={handleStop}
                title="Stop searching / cancel answer"
                className="w-9 h-9 rounded-xl bg-rose-600 hover:bg-rose-700 text-white flex items-center justify-center transition-all shadow-md shadow-rose-500/25 shrink-0 ml-1 active:scale-95 cursor-pointer animate-pulse"
              >
                <Square className="w-4 h-4 fill-current" />
              </button>
            ) : (
              <button
                type="button"
                onClick={() => handleSend()}
                disabled={!inputText.trim()}
                title="Send question"
                className="w-9 h-9 rounded-xl bg-[#2d5cf6] hover:bg-[#2551e8] text-white flex items-center justify-center transition-all shadow-md shadow-blue-500/25 shrink-0 ml-1 active:scale-95 disabled:opacity-40 cursor-pointer"
              >
                <Send className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Bottom Hotkeys */}
          <div className="flex items-center justify-between text-[11px] text-slate-400 px-2 font-medium">
            <span>Press Shift + Enter for new line</span>
            <span>Enter to send</span>
          </div>
        </div>
      </div>

      {/* Context panel is hidden per user request */}
      <div className="hidden">
        <div className="space-y-5">
          {/* 1. Header Navigation Tabs: Sources (3) | Related | Metadata */}
          <div className="flex items-center justify-between border-b border-slate-100 pb-3 text-xs">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => setRightTab('sources')}
                className={`font-bold pb-1 flex items-center space-x-1.5 transition-colors cursor-pointer ${
                  rightTab === 'sources'
                    ? 'text-blue-600 border-b-2 border-blue-600'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                <span>Sources</span>
                <span className="w-4 h-4 rounded-full bg-blue-100 text-blue-700 text-[10px] flex items-center justify-center font-bold">
                  {allActiveCitations.length}
                </span>
              </button>

              <button
                onClick={() => setRightTab('related')}
                className={`font-medium pb-1 transition-colors cursor-pointer ${
                  rightTab === 'related'
                    ? 'text-blue-600 border-b-2 border-blue-600 font-bold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                Related
              </button>

              <button
                onClick={() => setRightTab('metadata')}
                className={`font-medium pb-1 transition-colors cursor-pointer ${
                  rightTab === 'metadata'
                    ? 'text-blue-600 border-b-2 border-blue-600 font-bold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                Metadata
              </button>
            </div>

            <ChevronRight className="w-4 h-4 text-slate-400" />
          </div>

          {/* 2. Sources Accordion Section */}
          <div className="space-y-3">
            <div className="flex items-center justify-between text-xs font-bold text-slate-800">
              <span>Sources ({allActiveCitations.length})</span>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
            </div>

            <div className="space-y-2">
              {allActiveCitations.map((c, idx) => {
                const isPdf = c.documentTitle.toLowerCase().endsWith('.pdf');
                const isSelected = selectedCitation?.documentId === c.documentId;
                return (
                  <div
                    key={idx}
                    onClick={() => setSelectedCitation(c)}
                    className={`p-3 rounded-2xl border transition-all cursor-pointer flex items-center justify-between group ${
                      isSelected
                        ? 'bg-blue-50/60 border-blue-300 shadow-xs'
                        : 'bg-white border-slate-200/80 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center space-x-3 min-w-0">
                      <div
                        className={`w-7 h-7 rounded-lg flex items-center justify-center text-white shrink-0 ${
                          isPdf ? 'bg-rose-500' : 'bg-blue-600'
                        }`}
                      >
                        <FileText className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-xs font-bold text-slate-900 group-hover:text-blue-600 truncate">
                          {c.documentTitle}
                        </p>
                        <p className="text-[10px] text-slate-500 truncate mt-0.5">
                          Page {c.pageNumber || 1} • {c.sectionName || 'Security Policies'}
                        </p>
                      </div>
                    </div>

                    <span className="text-[10px] font-mono font-bold text-slate-500 uppercase px-1.5 py-0.5 rounded bg-slate-100 border border-slate-200 shrink-0">
                      {isPdf ? 'PDF' : 'DOCX'}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 3. Document Preview Card */}
          <div className="space-y-2.5 pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs font-bold text-slate-800">
              <div className="flex items-center space-x-1.5">
                <FileCheck2 className="w-3.5 h-3.5 text-blue-600" />
                <span>Document Preview</span>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
            </div>

            {selectedCitation ? (
              <div className="space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-2 truncate">
                    <div className="w-5 h-5 rounded bg-rose-500 text-white flex items-center justify-center shrink-0 text-[9px] font-bold">
                      <FileText className="w-3 h-3" />
                    </div>
                    <span className="font-bold text-slate-900 truncate text-[11px]">
                      {selectedCitation.documentTitle}
                    </span>
                  </div>
                  <div className="flex items-center space-x-1 text-[10px] text-slate-500 font-mono shrink-0">
                    <ChevronLeft className="w-3 h-3 text-slate-400 cursor-pointer" />
                    <span>{selectedCitation.pageNumber || 3} / 12</span>
                    <ChevronRight className="w-3 h-3 text-slate-400 cursor-pointer" />
                  </div>
                </div>

                <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/90 text-xs space-y-2">
                  <h4 className="font-bold text-slate-900 text-xs">
                    {selectedCitation.sectionName || '2.4 Security Policies'}
                  </h4>
                  <p className="text-[11px] text-slate-600 leading-relaxed font-normal italic">
                    "{selectedCitation.snippet}"
                  </p>
                  <div className="pt-1 space-y-1 text-[11px] text-slate-700">
                    <p className="font-bold text-[11px] text-slate-800">Key Policies:</p>
                    <p className="text-slate-600">• User roles and permissions</p>
                    <p className="text-slate-600">• Multi-factor authentication</p>
                    <p className="text-slate-600">• Periodic access reviews</p>
                  </div>
                </div>
              </div>
            ) : (
              <div className="p-4 rounded-2xl bg-slate-50 text-center text-xs text-slate-400">
                Click any source to preview its extracted text excerpt.
              </div>
            )}
          </div>

          {/* 4. Related Concepts Tags */}
          <div className="space-y-2.5 pt-2 border-t border-slate-100">
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-slate-800">Related Concepts</span>
              <Link
                to="/okf"
                className="text-[11px] font-bold text-blue-600 hover:text-blue-800 flex items-center space-x-1"
              >
                <span>View in Knowledge Explorer</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            </div>

            <div className="flex flex-wrap gap-1.5">
              {conceptTags.map((tag, idx) => (
                <span
                  key={idx}
                  className={`px-3 py-1 rounded-full text-[11px] font-semibold cursor-pointer hover:opacity-85 transition-opacity ${tag.color}`}
                >
                  {tag.label}
                </span>
              ))}
              <span className="px-2.5 py-1 rounded-full text-[11px] font-semibold bg-slate-100 text-slate-600 border border-slate-200">
                +2 more
              </span>
            </div>
          </div>
        </div>

        {/* Bottom Help */}
        <div className="pt-3 border-t border-slate-100 text-[10px] text-slate-400 text-center">
          NeuraKnow v1.0 • Grounded in OKF v0.2 Protocol
        </div>
      </div>
    </div>
  );
};
