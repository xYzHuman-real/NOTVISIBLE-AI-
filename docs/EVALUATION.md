# Evaluation Protocol

## Core dimensions
1. Instruction following
2. Factuality
3. Relevance
4. Clarity
5. Uncertainty handling
6. Safety behavior

## Procedure
- Keep the test set isolated from training.
- Use deterministic decoding for regression comparisons.
- Record prompts and outputs.
- Review failures individually.
- Add confirmed failure patterns to a future training/regression set.
- Never convert a tiny internal test into a broad benchmark claim.

## Release rule
A version is not considered successful merely because training loss decreases. A release requires held-out evaluation and documented failure analysis.
