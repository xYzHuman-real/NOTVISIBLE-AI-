# NOTVISIBLEAI Web

A static product site and API demo for NOTVISIBLEAI.

## Local use

Serve this directory with any static HTTP server:

```bash
python -m http.server 8080 --directory web
```

The page runs in demo mode until an API URL is supplied. Set `window.API_BASE` before the page loads, or store `nv_api_base` and optionally `nv_api_key` in localStorage.

Do not put a production API key into source-controlled files. Use a server-side proxy or secure runtime configuration for public deployments.
