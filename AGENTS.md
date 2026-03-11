# Repository Instructions

## Task Workflow

For each implementation task, follow this sequence unless the user explicitly asks for a different flow:

1. Create a new git branch before making task-specific changes.
2. Use a branch name that reflects the task, for example `task/01-project-setup` or `fix/http-client-timeout`.
3. Implement the task on that branch.
4. Run the relevant validation commands after implementation.
5. Confirm in the final response which tests or checks were run and whether they passed.
6. Commit only after the relevant tests/checks have run.
7. Use a detailed commit message that clearly describes the user-facing and technical changes.
8. Push the branch to `origin`.
9. Create a pull request using the GitHub CLI (`gh pr create`).

## Git and GitHub Rules

- Use the GitHub CLI for GitHub interactions, especially pull request creation and related PR operations.
- If `gh` is unavailable, misconfigured, or unauthenticated, stop at that point and tell the user exactly what is blocked.
- Do not merge pull requests unless the user explicitly asks for it.
- Do not commit or push broken code intentionally. If validation fails, report the failure and stop before commit unless the user instructs otherwise.

## Commit Message Guidance

Use detailed commit messages. Prefer this structure:

```text
<type>: <short summary>

- <change 1>
- <change 2>
- <change 3>
```

Example:

```text
feat: complete task 01 project bootstrap

- update Maven coordinates and Spring Boot dependencies
- add environment-specific application configuration
- add bootstrap tests and health check configuration
```

## Validation Expectations

- Run the narrowest meaningful test set first when iterating.
- Before commit, run the relevant project-level validation for the completed task.
- In the final response, state exactly which commands were run and whether they passed.

## Current Environment Note

As of 2026-03-11, `gh` is installed in this environment but not authenticated. Before push/PR steps can succeed through the GitHub CLI, run:

```bash
gh auth login
```
