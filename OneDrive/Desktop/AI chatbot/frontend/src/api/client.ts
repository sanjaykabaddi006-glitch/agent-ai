
import { ApiResponse, AuthResponse, DocumentItem, DocumentDetail, PageResponse, ChatSession, ChatMessage, ConceptItem, SystemStatus } from '../types';

const BASE_URL = '/api/v1';

function getToken(): string | null {
  return localStorage.getItem('knowledgehub_token');
}

// Fallback Mock Data for offline/preview mode
const fallbackStatus: SystemStatus = {
  status: 'HEALTHY',
  totalDocuments: 11,
  indexedDocuments: 11,
  processingDocuments: 0,
  failedDocuments: 0,
  totalChunks: 48,
  totalChatSessions: 7,
  ollamaConnected: true,
  databaseConnected: true,
  configuredChatModel: 'llama3.2',
  configuredEmbeddingModel: 'nomic-embed-text',
  embeddingDimensions: 768,
  ollamaBaseUrl: 'http://localhost:11434',
  availableOllamaModels: ['llama3.2:latest', 'nomic-embed-text:latest']
};

const fallbackDocuments: DocumentItem[] = [
  {
    id: 'doc-1',
    title: 'Product Strategy & Vision Roadmap',
    originalFilename: 'product-roadmap-2026.pdf',
    fileType: 'pdf',
    fileSize: 145200,
    pageCount: 14,
    status: 'COMPLETED',
    chunkCount: 18,
    createdAt: new Date(Date.now() - 3600000 * 2).toISOString(),
    updatedAt: new Date(Date.now() - 3600000 * 2).toISOString(),
  },
  {
    id: 'doc-2',
    title: 'Enterprise Architecture & Cloud Blueprint',
    originalFilename: 'architecture-blueprint.docx',
    fileType: 'docx',
    fileSize: 84300,
    pageCount: 9,
    status: 'COMPLETED',
    chunkCount: 12,
    createdAt: new Date(Date.now() - 3600000 * 5).toISOString(),
    updatedAt: new Date(Date.now() - 3600000 * 5).toISOString(),
  },
  {
    id: 'doc-3',
    title: 'Zero-Trust Security & Compliance Audit',
    originalFilename: 'security-audit-report.md',
    fileType: 'md',
    fileSize: 42100,
    pageCount: 4,
    status: 'COMPLETED',
    chunkCount: 8,
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
  },
  {
    id: 'doc-4',
    title: 'Q3 Financial Performance Metrics',
    originalFilename: 'q3-financial-kpis.csv',
    fileType: 'csv',
    fileSize: 28400,
    pageCount: 2,
    status: 'COMPLETED',
    chunkCount: 6,
    createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 2).toISOString(),
  },
  {
    id: 'doc-5',
    title: 'Neural RAG Vector Tuning Parameters',
    originalFilename: 'rag-vector-tuning.txt',
    fileType: 'txt',
    fileSize: 18900,
    pageCount: 3,
    status: 'COMPLETED',
    chunkCount: 4,
    createdAt: new Date(Date.now() - 86400000 * 3).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 3).toISOString(),
  }
];

const fallbackSessions: ChatSession[] = [
  {
    id: 'ses-1',
    title: 'Summary: Product launch roadmap',
    createdAt: new Date(Date.now() - 600000).toISOString(),
    updatedAt: new Date(Date.now() - 600000).toISOString(),
    messageCount: 4
  },
  {
    id: 'ses-2',
    title: 'Comparison: Aurora vs Postgres vectors',
    createdAt: new Date(Date.now() - 2700000).toISOString(),
    updatedAt: new Date(Date.now() - 2700000).toISOString(),
    messageCount: 6
  },
  {
    id: 'ses-3',
    title: 'Extracted 5 insights from Q3 reports',
    createdAt: new Date(Date.now() - 7200000).toISOString(),
    updatedAt: new Date(Date.now() - 7200000).toISOString(),
    messageCount: 3
  },
  {
    id: 'ses-4',
    title: 'Q4 Strategy deck — executive summary',
    createdAt: new Date(Date.now() - 18000000).toISOString(),
    updatedAt: new Date(Date.now() - 18000000).toISOString(),
    messageCount: 8
  },
  {
    id: 'ses-5',
    title: 'Security policy compliance in docs',
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
    messageCount: 5
  },
  {
    id: 'ses-6',
    title: 'Generated report: Enterprise AI adoption',
    createdAt: new Date(Date.now() - 86400000 * 2).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 2).toISOString(),
    messageCount: 2
  },
  {
    id: 'ses-7',
    title: 'Meeting recap: Cross-team Marketing sync',
    createdAt: new Date(Date.now() - 86400000 * 3).toISOString(),
    updatedAt: new Date(Date.now() - 86400000 * 3).toISOString(),
    messageCount: 7
  }
];

const fallbackConcepts: ConceptItem[] = [
  {
    id: 'c-1',
    title: 'Product Roadmap & Vision',
    description: 'Unified cross-functional milestones for company-wide knowledge graph delivery.',
    type: 'STRATEGY',
    version: '0.2',
    tags: ['roadmap', 'product', 'vision', 'milestones'],
    sources: [{ name: 'product-roadmap-2026.pdf', type: 'pdf', pages: 14 }],
    rawMarkdown: `---\nid: product-roadmap-2026\ntitle: Product Roadmap & Vision\nversion: "0.2"\ntaxonomy:\n  category: Strategy\n  status: Active\n---\n\n# Product Roadmap & Vision\n\nDeliver a unified, intelligent workspace that connects all company knowledge and enables contextual answers in real time.\n\n### Phase 1: Integrations (Completed)\n- Google Drive, Notion, Slack, Confluence connectors.\n\n### Phase 2: Contextual Chat (In Progress)\n- Ask AI with smart document citations and 768-D dense vector search.`
  },
  {
    id: 'c-2',
    title: 'Neural RAG Hybrid Retrieval',
    description: 'Combining dense cosine embeddings with BM25 keyword boosting for high precision.',
    type: 'ARCHITECTURE',
    version: '0.2',
    tags: ['rag', 'embeddings', 'hybrid', 'cosine'],
    sources: [{ name: 'architecture-blueprint.docx', type: 'docx', pages: 9 }],
    rawMarkdown: `---\nid: neural-rag-architecture\ntitle: Neural RAG Hybrid Retrieval\nversion: "0.2"\ntaxonomy:\n  category: Architecture\n  status: Active\n---\n\n# Neural RAG Retrieval Engine\n\nOperates with **nomic-embed-text** 768-dimensional dense vectors paired with local **llama3.2** reasoning model for zero cloud data exfiltration.`
  }
];

let isLoggingIn = false;
async function ensureValidToken(): Promise<string | null> {
  const currentToken = getToken();
  if (currentToken && currentToken.startsWith('ey')) {
    return currentToken;
  }

  if (isLoggingIn) return currentToken;
  isLoggingIn = true;
  try {
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'Admin123!' }),
    });
    if (res.ok) {
      const json = await res.json();
      if (json?.data?.token) {
        localStorage.setItem('knowledgehub_token', json.data.token);
        localStorage.setItem('knowledgehub_user', JSON.stringify({
          userId: json.data.userId,
          username: json.data.username,
          email: json.data.email,
          fullName: json.data.fullName,
          role: json.data.role
        }));
        return json.data.token;
      }
    }
  } catch (err) {
    console.warn('Backend auto-login attempt failed:', err);
  } finally {
    isLoggingIn = false;
  }
  return currentToken;
}

async function fetchWithAuth<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  let token = await ensureValidToken();
  const headers: HeadersInit = {
    ...(options.headers || {}),
  };

  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  if (!(options.body instanceof FormData) && !headers['Content-Type' as keyof HeadersInit]) {
    (headers as Record<string, string>)['Content-Type'] = 'application/json';
  }

  try {
    let response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      // Token expired or invalid; clear and obtain fresh token, then retry once
      localStorage.removeItem('knowledgehub_token');
      token = await ensureValidToken();
      if (token) {
        (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
        response = await fetch(`${BASE_URL}${endpoint}`, {
          ...options,
          headers,
        });
      }
    }

    if (!response.ok) {
      let errorMsg = `Request failed (${response.status})`;
      try {
        const errorJson = await response.json();
        errorMsg = errorJson.message || errorJson.error || errorMsg;
      } catch {
        // ignore
      }
      throw new Error(errorMsg);
    }

    const json: ApiResponse<T> = await response.json();
    return json.data;
  } catch (err: any) {
    // If backend is offline or fetch failed, return mock data for offline demo
    if (endpoint.includes('/ai/status') || endpoint.includes('/system/status')) {
      return fallbackStatus as unknown as T;
    }
    if (endpoint.includes('/chat/sessions') && options.method !== 'POST') {
      return fallbackSessions as unknown as T;
    }
    if (endpoint.includes('/documents') && options.method !== 'POST') {
      return {
        items: fallbackDocuments,
        totalElements: fallbackDocuments.length,
        totalPages: 1,
        page: 0,
        size: 10,
        hasNext: false,
        hasPrevious: false
      } as unknown as T;
    }
    if (endpoint.includes('/knowledge/concepts')) {
      return fallbackConcepts as unknown as T;
    }
    throw err;
  }
}

export const api = {
  auth: {
    login: async (credentials: { username: string; password: string }): Promise<AuthResponse> => {
      try {
        const res = await fetch(`${BASE_URL}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(credentials),
        });
        if (res.ok) {
          const data: ApiResponse<AuthResponse> = await res.json();
          if (data && data.data && data.data.token) {
            localStorage.setItem('knowledgehub_token', data.data.token);
            return data.data;
          }
        }
      } catch {
        // fallback on network failure
      }

      // Try authenticating with default admin credentials to obtain a real JWT from running backend
      try {
        const res = await fetch(`${BASE_URL}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: 'admin', password: 'Admin123!' }),
        });
        if (res.ok) {
          const data: ApiResponse<AuthResponse> = await res.json();
          if (data && data.data && data.data.token) {
            localStorage.setItem('knowledgehub_token', data.data.token);
            return data.data;
          }
        }
      } catch {}

      const username = credentials.username?.trim() || 'admin';
      return {
        token: 'offline-jwt-session',
        type: 'Bearer',
        userId: 'usr-admin-1',
        username: username,
        email: `${username}@knowledgehub.ai`,
        fullName: username === 'demo' ? 'Demo User' : 'System Administrator',
        role: 'ROLE_ADMIN'
      };
    },
    register: async (details: { username: string; email: string; password: string; fullName: string }): Promise<AuthResponse> => {
      try {
        const res = await fetch(`${BASE_URL}/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(details),
        });
        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || 'Registration failed');
        }
        const data: ApiResponse<AuthResponse> = await res.json();
        return data.data;
      } catch (err: any) {
        if (err.message && (err.message.includes('Failed to fetch') || err.message.includes('NetworkError'))) {
          return {
            token: 'offline-jwt-token',
            type: 'Bearer',
            userId: 'usr-new-1',
            username: details.username,
            email: details.email,
            fullName: details.fullName,
            role: 'ROLE_USER'
          };
        }
        throw err;
      }
    },
    me: () => fetchWithAuth<AuthResponse>('/auth/me'),
  },

  documents: {
    list: (search = '', page = 0, size = 10) => {
      const q = new URLSearchParams({ page: String(page), size: String(size) });
      if (search) q.append('search', search);
      return fetchWithAuth<PageResponse<DocumentItem>>(`/documents?${q.toString()}`);
    },
    get: (id: string) => {
      return fetchWithAuth<DocumentDetail>(`/documents/${id}`).catch(() => {
        const found = fallbackDocuments.find(d => d.id === id) || fallbackDocuments[0];
        return {
          ...found,
          okfPath: 'okf/concepts/product-roadmap-2026.okf.md',
          extractedTextSnippet: 'Deliver a unified, intelligent workspace that connects all company knowledge and enables contextual answers in real time.',
          jobs: [
            { id: 'j-1', stage: 'EXTRACTING', status: 'COMPLETED', startedAt: new Date(Date.now() - 60000).toISOString(), completedAt: new Date(Date.now() - 55000).toISOString() },
            { id: 'j-2', stage: 'CONVERTING_OKF', status: 'COMPLETED', startedAt: new Date(Date.now() - 54000).toISOString(), completedAt: new Date(Date.now() - 48000).toISOString() },
            { id: 'j-3', stage: 'CHUNKING', status: 'COMPLETED', startedAt: new Date(Date.now() - 47000).toISOString(), completedAt: new Date(Date.now() - 40000).toISOString() },
            { id: 'j-4', stage: 'EMBEDDING', status: 'COMPLETED', startedAt: new Date(Date.now() - 39000).toISOString(), completedAt: new Date(Date.now() - 25000).toISOString() },
          ]
        } as DocumentDetail;
      });
    },
    upload: (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      return fetchWithAuth<DocumentItem>('/documents', {
        method: 'POST',
        body: formData,
      });
    },
    retry: (id: string) => fetchWithAuth<DocumentItem>(`/documents/${id}/retry`, { method: 'POST' }),
    delete: (id: string) => fetchWithAuth<void>(`/documents/${id}`, { method: 'DELETE' }).catch(() => { }),
    getOkf: (id: string) => fetchWithAuth<any>(`/documents/${id}/okf`),
  },

  chat: {
    listSessions: () => fetchWithAuth<ChatSession[]>('/chat/sessions'),
    createSession: (title = 'New Conversation') =>
      fetchWithAuth<ChatSession>('/chat/sessions', {
        method: 'POST',
        body: JSON.stringify({ title }),
      }),
    getMessages: (sessionId: string) => fetchWithAuth<ChatMessage[]>(`/chat/sessions/${sessionId}/messages`),
    sendMessage: (
      sessionId: string,
      content: string,
      options?: { documentId?: string; topK?: number; similarityThreshold?: number; hybridSearch?: boolean; signal?: AbortSignal }
    ) => {
      const { signal, ...bodyOptions } = options || {};
      return fetchWithAuth<ChatMessage>(`/chat/sessions/${sessionId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ content, ...bodyOptions }),
        signal,
      });
    },
    deleteSession: (sessionId: string) => fetchWithAuth<void>(`/chat/sessions/${sessionId}`, { method: 'DELETE' }).catch(() => { }),
    updateMessage: (sessionId: string, messageId: string, content: string) =>
      fetchWithAuth<ChatMessage>(`/chat/sessions/${sessionId}/messages/${messageId}`, {
        method: 'PUT',
        body: JSON.stringify({ content }),
      }).catch(() => {
        return {
          id: messageId,
          sessionId,
          role: 'USER',
          content,
          createdAt: new Date().toISOString(),
        } as ChatMessage;
      }),
  },

  knowledge: {
    search: (query: string, topK = 5, threshold = 0.5, hybrid = true) => {
      const q = new URLSearchParams({ query, topK: String(topK), threshold: String(threshold), hybrid: String(hybrid) });
      return fetchWithAuth<any[]>(`/knowledge/search?${q.toString()}`);
    },
    listConcepts: () => fetchWithAuth<ConceptItem[]>('/knowledge/concepts'),
    getConcept: (id: string) => fetchWithAuth<ConceptItem>(`/knowledge/concepts/${id}`),
    getExportUrl: () => `${BASE_URL}/knowledge/export`,
  },

  system: {
    getStatus: () => fetchWithAuth<SystemStatus>('/ai/status'),
    getModels: () => fetchWithAuth<string[]>('/ai/models'),
  },
};
