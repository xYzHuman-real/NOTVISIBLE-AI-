# NOTVISIBLEAI — Product & Company Operating Spec

## Product
NOTVISIBLEAI is a developer-first AI model and inference platform. The current flagship research release is NV-0.2.

## Product surfaces
1. **Models** — versioned model/adapters with reproducible training and evaluation.
2. **Inference** — self-hosted runner exposing OpenAI-style chat completions.
3. **API** — authenticated gateway with rate limiting and model discovery.
4. **SDKs** — Python and JavaScript clients.
5. **Web** — public product site with an API-backed demo.
6. **Research** — datasets, configs, evaluation reports and reproducible workflows.

## Production boundary
The repository contains the complete software path needed to run the platform, but a genuinely public production service still requires external hosting, a domain, secret management, monitoring and an operational owner. Those resources cannot be provisioned from the repository alone.

## Security baseline
- Never commit API keys, model credentials or private tokens.
- Put secrets in the deployment platform's secret manager.
- Keep the inference runner private behind the API gateway.
- Use HTTPS for any public endpoint.
- Rotate credentials after accidental exposure.
- Treat model outputs as untrusted application data.
- Add persistent usage accounting and abuse controls before commercial launch.

## Commercial readiness
Before accepting paid production traffic, add billing, customer identity verification, terms/privacy pages appropriate to the jurisdiction, data-retention controls, support processes, monitoring/alerting and a tested incident-response procedure.
