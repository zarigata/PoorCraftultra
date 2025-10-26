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
PUBLISH_DIR="${PROJECT_ROOT}/publish/linux"
OUTPUT_DIR="${SCRIPT_DIR}/output"
DEB_STAGING="${SCRIPT_DIR}/deb-staging"
APPIMAGE_STAGING="${SCRIPT_DIR}/appimage-staging"
TOOLS_DIR="${SCRIPT_DIR}/tools"
APPRUN_FILE="${SCRIPT_DIR}/AppRun"
DESKTOP_FILE="${SCRIPT_DIR}/poorcraftultra.desktop"
ICON_DIR="${PROJECT_ROOT}/build/icons"

command -v dotnet >/dev/null 2>&1 || die "dotnet CLI not found. Install .NET 8 SDK to continue."
command -v dpkg-deb >/dev/null 2>&1 || die "dpkg-deb is required (install with: sudo apt-get install dpkg-dev)."
command -v curl >/dev/null 2>&1 || die "curl is required to download appimagetool."

if [[ ! -f "${PROJECT_FILE}" ]]; then
  die "Unable to locate project file at ${PROJECT_FILE}"
fi

VERSION=$(grep -oP '(?<=<Version>).*?(?=</Version>)' "${PROJECT_FILE}" | head -n1)
[[ -n "${VERSION:-}" ]] || die "Unable to extract <Version> from PoorCraftUltra.csproj"

log "Cleaning previous outputs"
rm -rf "${PUBLISH_DIR}" "${OUTPUT_DIR}" "${DEB_STAGING}" "${APPIMAGE_STAGING}"
mkdir -p "${PUBLISH_DIR}" "${OUTPUT_DIR}" "${DEB_STAGING}" "${APPIMAGE_STAGING}" "${TOOLS_DIR}"

log "Publishing self-contained linux-x64 build"
dotnet publish "${PROJECT_FILE}" \
  -c Release \
  -r linux-x64 \
  --self-contained true \
  -p:PublishSingleFile=false \
  -p:PublishTrimmed=false \
  -o "${PUBLISH_DIR}"

chmod +x "${PUBLISH_DIR}/PoorCraftUltra"

log "Copying documentation"
[[ -f "${PROJECT_ROOT}/README.md" ]] && cp "${PROJECT_ROOT}/README.md" "${PUBLISH_DIR}/"
[[ -f "${PROJECT_ROOT}/LICENSE" ]] && cp "${PROJECT_ROOT}/LICENSE" "${PUBLISH_DIR}/"
mkdir -p "${PUBLISH_DIR}/logs"

log "Preparing Debian package structure"
mkdir -p \
  "${DEB_STAGING}/DEBIAN" \
  "${DEB_STAGING}/usr/lib/poorcraftultra" \
  "${DEB_STAGING}/usr/bin" \
  "${DEB_STAGING}/usr/share/applications" \
  "${DEB_STAGING}/usr/share/icons/hicolor" \
  "${DEB_STAGING}/usr/share/doc/poorcraftultra"

cp -r "${PUBLISH_DIR}/"* "${DEB_STAGING}/usr/lib/poorcraftultra/"
ln -sf ../lib/poorcraftultra/PoorCraftUltra "${DEB_STAGING}/usr/bin/poorcraftultra"
cp "${DESKTOP_FILE}" "${DEB_STAGING}/usr/share/applications/"

if compgen -G "${ICON_DIR}/poorcraftultra-*.png" > /dev/null; then
  while IFS= read -r icon_path; do
    size=$(basename "${icon_path}" | sed -E 's/.*-([0-9]+)\.png/\1/')
    target_dir="${DEB_STAGING}/usr/share/icons/hicolor/${size}x${size}/apps"
    mkdir -p "${target_dir}"
    cp "${icon_path}" "${target_dir}/poorcraftultra.png"
  done < <(find "${ICON_DIR}" -maxdepth 1 -name 'poorcraftultra-*.png')
fi

cp "${PROJECT_ROOT}/README.md" "${DEB_STAGING}/usr/share/doc/poorcraftultra/" 2>/dev/null || true
cp "${PROJECT_ROOT}/LICENSE" "${DEB_STAGING}/usr/share/doc/poorcraftultra/" 2>/dev/null || true

cat > "${DEB_STAGING}/DEBIAN/control" <<EOF
Package: poorcraftultra
Version: ${VERSION}
Section: games
Priority: optional
Architecture: amd64
Maintainer: PoorCraft Ultra Team
Description: Open-source voxel game engine
EOF

log "Building Debian package"
DEB_OUTPUT="${OUTPUT_DIR}/poorcraftultra_${VERSION}_amd64.deb"
dpkg-deb --build "${DEB_STAGING}" "${DEB_OUTPUT}"

log "Preparing AppImage staging"
mkdir -p \
  "${APPIMAGE_STAGING}/usr/bin" \
  "${APPIMAGE_STAGING}/usr/lib/poorcraftultra" \
  "${APPIMAGE_STAGING}/usr/share/applications" \
  "${APPIMAGE_STAGING}/usr/share/icons/hicolor/256x256/apps"

cp -r "${PUBLISH_DIR}/"* "${APPIMAGE_STAGING}/usr/lib/poorcraftultra/"
ln -sf ../lib/poorcraftultra/PoorCraftUltra "${APPIMAGE_STAGING}/usr/bin/PoorCraftUltra"
cp "${DESKTOP_FILE}" "${APPIMAGE_STAGING}/usr/share/applications/poorcraftultra.desktop"
cp "${DESKTOP_FILE}" "${APPIMAGE_STAGING}/poorcraftultra.desktop"
cp "${ICON_DIR}/poorcraftultra-256.png" "${APPIMAGE_STAGING}/usr/share/icons/hicolor/256x256/apps/poorcraftultra.png"
cp "${ICON_DIR}/poorcraftultra-512.png" "${APPIMAGE_STAGING}/poorcraftultra.png"
cp "${APPRUN_FILE}" "${APPIMAGE_STAGING}/AppRun"
chmod +x "${APPIMAGE_STAGING}/AppRun"

APPIMAGE_TOOL="${TOOLS_DIR}/appimagetool"
if [[ ! -x "${APPIMAGE_TOOL}" ]]; then
  log "Downloading appimagetool"
  APPIMAGE_URL="https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
  curl -L "${APPIMAGE_URL}" -o "${APPIMAGE_TOOL}"
  chmod +x "${APPIMAGE_TOOL}"
fi

log "Building AppImage"
APPIMAGE_OUTPUT="${OUTPUT_DIR}/PoorCraftUltra-v${VERSION}-linux-x86_64.AppImage"
"${APPIMAGE_TOOL}" "${APPIMAGE_STAGING}" "${APPIMAGE_OUTPUT}"

log "Creating portable tarball"
PORTABLE_TARBALL="${OUTPUT_DIR}/PoorCraftUltra-v${VERSION}-linux-x64-portable.tar.gz"
tar -czf "${PORTABLE_TARBALL}" -C "${PUBLISH_DIR}" .

log "Summary"
printf 'Version: %s\n' "${VERSION}"
printf 'Debian package: %s\n' "${DEB_OUTPUT}"
printf 'AppImage: %s\n' "${APPIMAGE_OUTPUT}"
printf 'Portable tarball: %s\n' "${PORTABLE_TARBALL}"
