#!/bin/sh
set -eu

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || {
echo "Not inside a git repository"
exit 1
}

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# Use versioned hooks tracked in the repository.
git config core.hooksPath .githooks

# Ensure hooks are executable
find .githooks -type f -exec chmod +x {} +

echo "Git hooks configured successfully."
echo "Active hooks path: $(git config --get core.hooksPath)"
