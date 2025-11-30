#!/bin/bash

# CONFIGURATION
EXCLUDE_DIR="src/main/java/net/publicworks/app/backend"

echo "-----------------------------------------"
echo "  SYNCING PRIVATE → PUBLIC (excluding $EXCLUDE_DIR)"
echo "-----------------------------------------"

# Ensure we are in a git repo
git rev-parse --git-dir > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "❌ Not a git repository. Run this from your project root."
  exit 1
fi

# Ensure both branches exist
git show-ref --verify --quiet refs/heads/private || { echo "❌ private branch missing"; exit 1; }
git show-ref --verify --quiet refs/heads/public  || { echo "❌ public branch missing"; exit 1; }

# Update private branch from origin
echo "✔ Updating private branch..."
git checkout private || exit 1
git pull origin private || exit 1

# Update public branch
echo "✔ Updating public branch..."
git checkout public || exit 1
git pull origin public || exit 1

# Apply private changes -> public except backend
echo "✔ Applying private → public diff (excluding $EXCLUDE_DIR)..."
git diff public..private -- . ":!$EXCLUDE_DIR" | git apply

# Stage + commit changes if any
if [[ -n "$(git status --porcelain)" ]]; then
    git add .
    git commit -m "Sync from private (excluding backend)"
    echo "✔ Changes committed"
else
    echo "✔ No changes to sync"
fi

# Push to origin/public
echo "✔ Pushing public branch to origin..."
git push origin public

# Push to demo repo
echo "✔ Pushing public branch to GitHub demo repo..."
git push public public:main

echo "-----------------------------------------"
echo "  ✅ SYNC COMPLETE"
echo "-----------------------------------------"

#chmod +x sync_public.sh
# ./sync_public.sh
