# NOTVISIBLEAI JavaScript SDK

Minimal ESM client for the NOTVISIBLEAI API.

```js
import { NotVisibleAI } from "./notvisibleai.mjs";

const client = new NotVisibleAI("YOUR_KEY", "http://localhost:8000");
console.log(await client.models());
```
