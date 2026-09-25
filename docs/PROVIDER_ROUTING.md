# Provider routing

The API can now use either the local NV-0.2 inference runner or an external OpenAI-compatible chat provider.

## Local mode

Set:

`NV_INFERENCE_URL=http://inference:8100`

and leave `NV_PROVIDER_URL` empty.

## Remote provider mode

Set:

`NV_PROVIDER_URL=https://your-provider.example`
`NV_PROVIDER_KEY=your-secret`
`NV_PROVIDER_MODEL=provider-model-id`

The gateway forwards the OpenAI-style chat request to `/v1/chat/completions`. The provider key stays on the API server and is never stored by the browser workspace.

## Production note

Use a secret manager for provider credentials, TLS for public traffic, request logging with appropriate privacy controls, and provider-specific terms/privacy review. Do not commit provider keys to Git.
