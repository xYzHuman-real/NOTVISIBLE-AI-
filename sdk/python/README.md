# NOTVISIBLEAI Python SDK

Minimal client for the NOTVISIBLEAI API.

```python
from notvisibleai import NotVisibleAI

client = NotVisibleAI("YOUR_KEY", "http://localhost:8000")
print(client.models())
```

The SDK is intentionally small until the public API contract stabilizes.
