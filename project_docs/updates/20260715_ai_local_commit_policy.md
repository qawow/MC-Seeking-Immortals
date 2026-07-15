# AI local Git commit policy

## Change class

- Documentation and repository workflow only.
- No shipped code, resources, data packs, build logic or gameplay behavior changed.
- `mod_version` remains `0.1.501`.
- `ModNetwork.PROTOCOL_VERSION` remains `18`.

## Policy

- Codex and Claude Code must create a local Git commit after every completed update batch.
- Every non-empty update must have a Chinese commit subject and Chinese commit body.
- The AI must inspect and stage only task-related files, then report the local commit hash and subject.
- Local `git commit` is the normal endpoint.
- The AI must not run `git push`, create a GitHub pull request, or perform another remote publishing operation unless the user explicitly requests it in that turn.

## Verification

- `AGENTS.md` and `CLAUDE.md` contain the same workflow rules apart from their tool-specific header.
- This is docs-only maintenance, so no version bump or Gradle build is required.
- Backup: `.bak/20260715_181054_ai_local_commit_policy/`.
