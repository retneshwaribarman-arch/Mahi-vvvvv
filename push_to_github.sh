#!/data/data/com.termux/files/usr/bin/bash
# Usage: bash push_to_github.sh https://github.com/USERNAME/MAHI.git
set -e
if [ -z "$1" ]; then echo "Repo URL dao: bash push_to_github.sh <url>"; exit 1; fi
pkg install -y git >/dev/null 2>&1 || true
git init -b main
git add .
git commit -m "MAHI Android app"
git remote remove origin 2>/dev/null || true
git remote add origin "$1"
git push -u origin main
echo "Push hoyeche. GitHub > Actions tab e giye APK download koro."
