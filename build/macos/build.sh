#!/usr/bin/env bash
set -euo pipefail

log() {
  printf '\n==> %s\n' "$1"
}

die() {
  printf 'Error: %s\n' "$1" >&2
  exit 1
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROJECT_FILE="${PROJECT_ROOT}/PoorCraftUltra.csproj"
PUBLISH_DIR="${PROJECT_ROOT}/publish/macos"
APP_BUNDLE="${SCRIPT_DIR}/PoorCraftUltra.app"
OUTPUT_DIR="${SCRIPT_DIR}/output"
TEMP_DMG="${SCRIPT_DIR}/temp-dmg"
ICON_FILE="${PROJECT_ROOT}/build/icons/poorcraftultra.icns"
PLIST_TEMPLATE="${SCRIPT_DIR}/Info.plist.template"

command -v dotnet >/dev/null 2>&1 || die "dotnet CLI not found. Install .NET 8 SDK."
command -v hdiutil >/dev/null 2>&1 || die "hdiutil is required (macOS only)."

if [[ ! -f "${PROJECT_FILE}" ]]; then
  die "Unable to locate project file at ${PROJECT_FILE}"
fi

VERSION=$(grep -oE '<Version>[^<]+' "${PROJECT_FILE}" | head -n1 | sed 's/<Version>//')
[[ -n "${VERSION:-}" ]] || die "Unable to extract <Version> from PoorCraftUltra.csproj"

log "Cleaning previous outputs"
rm -rf "${PUBLISH_DIR}" "${APP_BUNDLE}" "${OUTPUT_DIR}" "${TEMP_DMG}"
mkdir -p "${PUBLISH_DIR}" "${OUTPUT_DIR}" "${TEMP_DMG}"

log "Publishing self-contained osx-x64 build"
dotnet publish "${PROJECT_FILE}" \
  -c Release \
  -r osx-x64 \
  --self-contained true \
  -p:PublishSingleFile=false \
  -p:PublishTrimmed=false \
  -o "${PUBLISH_DIR}"

chmod +x "${PUBLISH_DIR}/PoorCraftUltra"

log "Copying documentation"
[[ -f "${PROJECT_ROOT}/README.md" ]] && cp "${PROJECT_ROOT}/README.md" "${PUBLISH_DIR}/"
[[ -f "${PROJECT_ROOT}/LICENSE" ]] && cp "${PROJECT_ROOT}/LICENSE" "${PUBLISH_DIR}/"
mkdir -p "${PUBLISH_DIR}/logs"

log "Creating app bundle"
mkdir -p \
  "${APP_BUNDLE}/Contents/MacOS" \
  "${APP_BUNDLE}/Contents/Resources"

cp -R "${PUBLISH_DIR}/"* "${APP_BUNDLE}/Contents/MacOS/"

if [[ ! -f "${ICON_FILE}" ]]; then
  die "Icon file not found at ${ICON_FILE}"
fi
cp "${ICON_FILE}" "${APP_BUNDLE}/Contents/Resources/poorcraftultra.icns"

if [[ ! -f "${PLIST_TEMPLATE}" ]]; then
  die "Info.plist template missing at ${PLIST_TEMPLATE}"
fi
sed "s/{VERSION}/${VERSION}/g" "${PLIST_TEMPLATE}" > "${APP_BUNDLE}/Contents/Info.plist"
chmod +x "${APP_BUNDLE}/Contents/MacOS/PoorCraftUltra"

log "Creating portable zip"
PORTABLE_ZIP="${OUTPUT_DIR}/PoorCraftUltra-v${VERSION}-macos-x64-portable.zip"
(cd "${SCRIPT_DIR}" && zip -qry "${PORTABLE_ZIP}" "PoorCraftUltra.app")

log "Preparing DMG contents"
cp -R "${APP_BUNDLE}" "${TEMP_DMG}/"
ln -s /Applications "${TEMP_DMG}/Applications"
[[ -f "${PROJECT_ROOT}/README.md" ]] && cp "${PROJECT_ROOT}/README.md" "${TEMP_DMG}/" || true
[[ -f "${PROJECT_ROOT}/LICENSE" ]] && cp "${PROJECT_ROOT}/LICENSE" "${TEMP_DMG}/" || true

log "Creating DMG"
DMG_PATH="${OUTPUT_DIR}/PoorCraftUltra-v${VERSION}-macos-x64.dmg"
hdiutil create -volname "PoorCraft Ultra" -srcfolder "${TEMP_DMG}" -ov -format UDZO "${DMG_PATH}"

log "Summary"
printf 'Version: %s\n' "${VERSION}"
printf 'App bundle: %s\n' "${APP_BUNDLE}"
printf 'Portable ZIP: %s\n' "${PORTABLE_ZIP}"
printf 'DMG: %s\n' "${DMG_PATH}"
