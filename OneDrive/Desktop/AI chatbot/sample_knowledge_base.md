# KnowledgeHub AI Enterprise Architecture & Operations Guide

## Executive Summary
KnowledgeHub AI is a next-generation corporate intelligence engine designed for secure knowledge extraction, vector embedding, and retrieval-augmented generation (RAG). By utilizing Open Knowledge Format (OKF v0.2) Markdown as an intermediate representation, the platform preserves document hierarchy, semantic relationships, and source provenance.

## OKF v0.2 Specification Standards
The Open Knowledge Format v0.2 standardizes corporate data interchange through structured YAML frontmatter coupled with semantic Markdown. Every OKF document adheres to the following principles:
- **Header Provenance**: Contains unique identifiers (`document_id`, `bundle_id`), ISO-8601 timestamps, author metadata, and checksum verification.
- **Taxonomy Tagging**: Categorizes records across business domains (Engineering, Finance, Legal, Operations) and hierarchical topics.
- **Concept Decomposition**: Long enterprise narratives are parsed into atomic concepts, definitions, rules, and operational procedures.

## Security and Compliance Protocol
KnowledgeHub AI enforces defense-in-depth principles:
- **Authentication**: Stateless HMAC-SHA512 JSON Web Tokens with 24-hour expiration.
- **Prompt Injection Defense**: Guardrails filter out system override sequences, jailbreaks, and delimiter manipulation.
- **Tenant Isolation**: Strict role-based access control (RBAC) ensuring documents and chat sessions are strictly isolated per authenticated user identity.
- **Fallback Guarantee**: If local neural networks (Ollama) are unavailable or retrieved context has cosine similarity below 0.50, the model gracefully returns grounded fallback warnings without hallucination.

## Hardware and Model Configuration
The default on-premises deployment utilizes:
- **Chat LLM**: Meta Llama 3.2 (3B parameters) running through Ollama on port 11434.
- **Embedding Model**: Nomic Embed Text (768 dimensions) with 8192 token context window.
- **Vector Indexing**: PostgreSQL with pgvector extension or embedded H2 memory index with cosine similarity search.
- **Chunk Parameters**: 800 token chunk size with 150 token sliding overlap window.
