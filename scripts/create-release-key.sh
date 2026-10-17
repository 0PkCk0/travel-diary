#!/usr/bin/env bash
# Creates the key that signs every release of the app and explains which GitHub secrets to add.
#
# Keep the key file and its password safe (a password manager is a good place). Android only
# installs an update signed with the same key: if the key is lost, nobody can update the app
# anymore and everyone has to uninstall it (losing their diary unless they exported it).
#
# Usage: scripts/create-release-key.sh [path/to/travel-diary-release.jks]
set -euo pipefail

keystore="${1:-$HOME/travel-diary-release.jks}"
alias="travel-diary"

if [ -e "$keystore" ]; then
  echo "$keystore already exists, not overwriting it." >&2
  exit 1
fi
if ! command -v keytool >/dev/null; then
  echo "keytool not found: install a JDK (17 or newer)." >&2
  exit 1
fi

read -rsp "Choose a password for the key (at least 6 characters): " password
echo
read -rsp "Type it again: " confirmation
echo
if [ "$password" != "$confirmation" ]; then
  echo "The passwords differ." >&2
  exit 1
fi

TRAVEL_DIARY_KEY_PASSWORD="$password" keytool -genkeypair \
  -keystore "$keystore" -storetype PKCS12 -alias "$alias" \
  -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Travel Diary" \
  -storepass:env TRAVEL_DIARY_KEY_PASSWORD -keypass:env TRAVEL_DIARY_KEY_PASSWORD

cat <<EOF

Created $keystore

Now add four repository secrets on GitHub (Settings > Secrets and variables > Actions):
  KEYSTORE_BASE64    the output of: base64 -w0 "$keystore"
  KEYSTORE_PASSWORD  the password you just chose
  KEY_ALIAS          $alias
  KEY_PASSWORD       the password you just chose

Or with the GitHub CLI, from the repository folder:
  base64 -w0 "$keystore" | gh secret set KEYSTORE_BASE64
  gh secret set KEYSTORE_PASSWORD
  gh secret set KEY_ALIAS --body "$alias"
  gh secret set KEY_PASSWORD

Back up $keystore and its password now.
EOF
