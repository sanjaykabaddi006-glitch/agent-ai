export interface User {
  userId: string;
  username: string;
  email: string;
  fullName: string;
  role: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: string;
  username: string;
  email: string;
  fullName: string;
  role: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface DocumentItem {
  id: string;
  title: string;
  originalFilename: string;
  fileType: string;
  fileSize: number;
  status: 'PENDING' | 'EXTRACTING' | 'CONVERTING_OKF' | 'CHUNKING' | 'EMBEDDING' | 'COMPLETED' | 'FAILED';
  errorMessage?: string;
  chunkCount: number;
  pageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProcessingJob {
  id: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  stage: string;
  errorMessage?: string;
  startedAt: string;
  completedAt?: string;
}

export interface DocumentDetail extends DocumentItem {
  okfPath?: string;
  jobs: ProcessingJob[];
}

export interface Citation {
  documentId: string;
  documentTitle: string;
  pageNumber?: number;
  sectionName?: string;
  chunkIndex: number;
  snippet: string;
  similarityScore: number;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  citations?: Citation[];
  createdAt: string;
}

export interface ChatSession {
  id: string;
  title: string;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ConceptItem {
  id: string;
  type: string;
  title: string;
  description: string;
  tags: string[];
  sources: Array<{ name?: string; type?: string; pages?: number; size_bytes?: number }>;
  rawMarkdown: string;
  version?: string;
}

export interface SystemStatus {
  status: string;
  databaseConnected: boolean;
  ollamaConnected: boolean;
  ollamaBaseUrl: string;
  configuredChatModel: string;
  configuredEmbeddingModel: string;
  embeddingDimensions: number;
  availableOllamaModels: string[];
  totalDocuments: number;
  indexedDocuments: number;
  processingDocuments: number;
  failedDocuments: number;
  totalChunks: number;
  totalChatSessions: number;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
