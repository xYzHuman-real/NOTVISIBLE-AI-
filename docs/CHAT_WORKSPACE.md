# Chat Workspace

The web layer now includes a browser-based conversation workspace at `web/chat.html`.

## Included

- Multiple local conversations with recent-history switching.
- Local persistence using browser storage.
- Conversation rename and clear-history controls.
- Configurable API base URL and bearer key for development.
- Text-file context attachments for TXT, Markdown, JSON, CSV, Python, JavaScript, TypeScript, HTML, and CSS.
- Attachment content is truncated to 20,000 characters before it is sent to the API.
- OpenAI-style `/v1/chat/completions` requests using the current `nv-0.2` model ID.

## Boundary

This is a browser-local workspace, not a hosted account system. Chat history is not synchronized across devices and the API key is stored in local browser storage for convenience. A production account service should replace these mechanisms with server-side identity, encrypted secret handling, persistent storage, retention controls, and audit logging.

Binary documents, images, streaming responses, web search, and provider-backed multimodal inference are not claimed as implemented by this workspace.
