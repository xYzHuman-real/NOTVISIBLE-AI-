# NOTVISIBLEAI Web

A static product site plus a browser chat workspace for NOTVISIBLEAI.

## Local use

Serve this directory with any static HTTP server:

```bash
python -m http.server 8080 --directory web
```

Open `/chat.html` for the full browser workspace.

The workspace supports local conversation history, chat switching, renaming, clearing history, API connection settings, and text-based file context. It sends OpenAI-style requests to the configured NOTVISIBLEAI API.

Do not put a production API key into source-controlled files. The browser workspace stores a development key in local browser storage for convenience; production deployments should use a secure server-side authentication flow.
