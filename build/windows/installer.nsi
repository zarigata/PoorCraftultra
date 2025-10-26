!include "MUI2.nsh"

!ifndef VERSION
  !define VERSION "0.1.0"
!endif

!define PRODUCT_NAME "PoorCraft Ultra"
!define COMPANY_NAME "PoorCraft Ultra Team"
!define PRODUCT_PUBLISHER "PoorCraft Ultra Team"
!define PRODUCT_WEB "https://github.com/yourusername/PoorCraftUltra"
!define INSTALLER_ICON "${__FILEDIR__}\..\icons\poorcraftultra.ico"
!define STAGING_DIR "${__FILEDIR__}\installer-staging"
!define OUTPUT_DIR "${__FILEDIR__}\output"

Unicode true
RequestExecutionLevel admin

Name "${PRODUCT_NAME}"
OutFile "${OUTPUT_DIR}\PoorCraftUltra-v${VERSION}-windows-x64-installer.exe"
InstallDir "$PROGRAMFILES64\PoorCraftUltra"
InstallDirRegKey HKLM "Software\PoorCraftUltra" "InstallLocation"
ShowInstDetails show
ShowUninstDetails show
Icon "${INSTALLER_ICON}"
UninstallIcon "${INSTALLER_ICON}"

VIProductVersion "${VERSION}.0"
VIAddVersionKey "ProductName" "${PRODUCT_NAME}"
VIAddVersionKey "CompanyName" "${COMPANY_NAME}"
VIAddVersionKey "FileDescription" "${PRODUCT_NAME} Installer"
VIAddVersionKey "ProductVersion" "${VERSION}"
VIAddVersionKey "LegalCopyright" "Copyright © 2025 PoorCraft Ultra Team"

!define MUI_ABORTWARNING
!define MUI_ICON "${INSTALLER_ICON}"
!define MUI_UNICON "${INSTALLER_ICON}"

Var StartMenuFolder
Var CreateDesktop

!insertmacro MUI_PAGE_WELCOME
!ifexist "${STAGING_DIR}\LICENSE"
  !define MUI_PAGE_CUSTOMFUNCTION_SHOW LicenseShow
  !insertmacro MUI_PAGE_LICENSE "${STAGING_DIR}\LICENSE"
!endif
!insertmacro MUI_PAGE_DIRECTORY
Page Custom DesktopPageCreate DesktopPageLeave
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "English"

Function LicenseShow
  StrCpy $CreateDesktop "1"
FunctionEnd

Function DesktopPageCreate
  nsDialogs::Create 1018
  Pop $0
  ${If} $0 == error
    Abort
  ${EndIf}

  ${NSD_CreateCheckbox} 20u 30u 100% 12u "Create a desktop shortcut"
  Pop $1
  ${NSD_Check} $1
  nsDialogs::Show
FunctionEnd

Function DesktopPageLeave
  ${NSD_GetState} $1 $CreateDesktop
FunctionEnd

Section "Install"
  SetOutPath "$INSTDIR"
  File /r "${STAGING_DIR}\*"

  CreateDirectory "$SMPROGRAMS\PoorCraft Ultra"
  CreateShortCut "$SMPROGRAMS\PoorCraft Ultra\PoorCraft Ultra.lnk" "$INSTDIR\PoorCraftUltra.exe"
  CreateShortCut "$SMPROGRAMS\PoorCraft Ultra\Uninstall PoorCraft Ultra.lnk" "$INSTDIR\uninstall.exe"

  ${If} $CreateDesktop == "1"
    CreateShortCut "$DESKTOP\PoorCraft Ultra.lnk" "$INSTDIR\PoorCraftUltra.exe"
  ${EndIf}

  WriteUninstaller "$INSTDIR\uninstall.exe"
  WriteRegStr HKLM "Software\PoorCraftUltra" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PoorCraftUltra" "DisplayName" "${PRODUCT_NAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PoorCraftUltra" "DisplayVersion" "${VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PoorCraftUltra" "Publisher" "${PRODUCT_PUBLISHER}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PoorCraftUltra" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PoorCraftUltra" "UninstallString" "$INSTDIR\uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PoorCraftUltra" "DisplayIcon" "$INSTDIR\PoorCraftUltra.exe"
SectionEnd

Section "Uninstall"
  Delete "$INSTDIR\uninstall.exe"
  Delete "$INSTDIR\PoorCraftUltra.exe"
  RMDir /r "$INSTDIR"

  Delete "$SMPROGRAMS\PoorCraft Ultra\PoorCraft Ultra.lnk"
  Delete "$SMPROGRAMS\PoorCraft Ultra\Uninstall PoorCraft Ultra.lnk"
  RMDir "$SMPROGRAMS\PoorCraft Ultra"

  Delete "$DESKTOP\PoorCraft Ultra.lnk"

  DeleteRegKey HKLM "Software\PoorCraftUltra"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\PoorCraftUltra"
SectionEnd
