# MAHI — Android AI Assistant

Ekta complete Android project. UI (`app/src/main/assets/index.html`) offline-e kaj kore; shudhu Gemini chat-er jonno internet lagbe.

## Features
- Female-leaning voice (phone-e offline voice thakle sheta-i use hoy)
- Voice input (mic) ar keyboard input
- Gemini API chat — Settings e key dile
- Phone control: app khola, call, SMS, call receive (permission dile)
- Screen automation (Accessibility): text dhore tap, type, scroll, back, home
- Screen recording (MediaProjection) — Movies/ folder e save hoy
- Camera diye photo
- Background service (notification soho chole, Power button diye bondho)
- 18+ voice mode: age confirmation modal ache; confirm na korle chalu hobe na

## Termux theke APK banano (sob kaj GitHub Actions korbe)
Termux-e Android APK build kora bhari, tai build GitHub-e hoy.

1. Termux e: `pkg install unzip` tarpor zip khul
2. GitHub e notun private repo banao (jemon `MAHI`), URL copy koro
3. `cd MAHI_Project && bash push_to_github.sh https://github.com/USERNAME/MAHI.git`
   (GitHub login/token chaile Termux prompt e dibe)
4. GitHub > repo > **Actions** > "Build MAHI APK" run hole, shesh hole **MAHI-debug-apk** download koro
5. APK install koro (Unknown sources allow korte hobe)

## Phone-e setup
1. MAHI khulo → Settings (⚙) → **Gemini API key** boshao (https://aistudio.google.com/apikey theke free key)
2. Permission prompt e sob allow koro: Mic, Camera, Call, Phone, SMS, Notification
3. Settings → **Accessibility on koro** → "MAHI" on koro (screen automation-er jonno Android ei permission manually dite hoy)
4. Power (⏻) button → background-e chalu hobe
5. 18+ mode chalu korte: Settings → 18+ voice mode → confirm koro

## Important notes
- Android security-r karone accessibility, screen recording, call receive ar SMS sob user-er explicit onumoti lage. App nijeo ei permission bypass korte pare na.
- Kichu app (bank, OTP screen) accessibility read/tap block kore.
- "Hey MAHI" wake word ei version e nei. Mic button ba orb tap kore bolo.
- Gemini API key phone-e local SharedPreferences e thake. Key share koro na.
- Gemini call internet chay; shudhu UI ar screen-e kaj offline-e chole.

## Folder
```
app/src/main/java/com/mahi/assistant/   Kotlin code
app/src/main/assets/index.html          UI (offline)
app/src/main/AndroidManifest.xml        permissions
.github/workflows/build-apk.yml         APK build
```
