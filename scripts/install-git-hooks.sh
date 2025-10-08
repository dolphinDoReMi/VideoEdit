#!/usr/bin/env bash
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
mkdir -p "${ROOT}/.git/hooks"

# commit-msg: Conventional Commits + scope
cat > "${ROOT}/.git/hooks/commit-msg" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
MSG_FILE="$1"
SCOPE='(app|core\/infra|core\/media|core\/ml|feature\/whisper|feature\/clip|feature\/retrieval|feature\/ui|tools|docs|ci)'
if ! grep -E -q "^(feat|fix|perf|refactor|docs|test|chore|ci|build|revert)\(${SCOPE}\)!?: .+" "$MSG_FILE"; then
  echo "❌ Commit message must follow Conventional Commits with approved scope."
  echo "   e.g., feat(feature/retrieval): add cosine topK unit test"
  exit 1
fi
EOF
chmod +x "${ROOT}/.git/hooks/commit-msg"

# pre-push: policy + quick gradle checks
cat > "${ROOT}/.git/hooks/pre-push" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
tools/push_guard.sh
EOF
chmod +x "${ROOT}/.git/hooks/pre-push"

echo "✓ Git hooks installed"
