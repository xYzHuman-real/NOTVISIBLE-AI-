export class NotVisibleAI {
  constructor(apiKey, baseUrl = "http://localhost:8000") {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  async models() {
    return this.#request("/v1/models", { method: "GET" });
  }

  async chat(messages, options = {}) {
    return this.#request("/v1/chat/completions", {
      method: "POST",
      body: JSON.stringify({
        model: options.model ?? "nv-0.2",
        messages,
        temperature: options.temperature ?? 0.7,
        max_tokens: options.max_tokens ?? 256,
        stream: false,
      }),
    });
  }

  async #request(path, init) {
    const response = await fetch(this.baseUrl + path, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${this.apiKey}`,
        ...(init.headers ?? {}),
      },
    });
    if (!response.ok) throw new Error(`NOTVISIBLEAI API HTTP ${response.status}: ${await response.text()}`);
    return response.json();
  }
}
