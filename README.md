# 🌐 LingoSphere — Real-Time AI Voice Calling & Language Tutor

**Package ID:** `com.aistudio.lingosphere.kxpqzm`  
**AdMob App ID:** `ca-app-pub-8471293855339658~7525338937`  
**AdMob Publisher ID:** `pub-8471293855339658`  
**GitHub Account:** `gadgetexplorerpro-crypto`

LingoSphere is an Android language learning application built with Kotlin, Jetpack Compose, Material 3, Firebase Authentication, Cloud Firestore, Google Play Billing 8.0.0, Google AdMob, and Gemini AI real-time bidirectional voice tutoring.

---

## ✅ Multi-App Setup: One Shared `app-ads.txt` + Separate Repo Per App

Because all of your apps share the **same Google AdMob account (`pub-8471293855339658`)**, you only ever need **one** root `app-ads.txt` file for **all of your apps**, while each app hosts its own Privacy Policy and Delete Account pages in its own GitHub repository.

### Quick Copy-Paste Table (LingoSphere vs. Your Other Apps)

| Google Play Console Field | LingoSphere | Any Other App (`<OtherAppRepo>`) |
| :--- | :--- | :--- |
| **Developer Website** *(Store settings → Contact details → Website)* | `https://gadgetexplorerpro-crypto.github.io` *(SAME)* | `https://gadgetexplorerpro-crypto.github.io` *(SAME)* |
| **AdMob `app-ads.txt`** *(crawled automatically by AdMob)* | `https://gadgetexplorerpro-crypto.github.io/app-ads.txt` *(SAME)* | `https://gadgetexplorerpro-crypto.github.io/app-ads.txt` *(SAME)* |
| **Privacy Policy URL** *(App content → Privacy policy)* | `https://gadgetexplorerpro-crypto.github.io/LingoSphere-app/privacy-policy.html` | `https://gadgetexplorerpro-crypto.github.io/<OtherAppRepo>/privacy-policy.html` |
| **Delete Account URL** *(Data safety → Delete account URL)* | `https://gadgetexplorerpro-crypto.github.io/LingoSphere-app/delete-account.html` | `https://gadgetexplorerpro-crypto.github.io/<OtherAppRepo>/delete-account.html` |
| **Terms of Service URL** | `https://gadgetexplorerpro-crypto.github.io/LingoSphere-app/terms.html` | `https://gadgetexplorerpro-crypto.github.io/<OtherAppRepo>/terms.html` |

---

### Step 1 — One-Time Root Repo (Covers ALL Your Apps Forever)
1. On your **`gadgetexplorerpro-crypto`** GitHub account, create **one** Public repository named **exact match**:  
   `gadgetexplorerpro-crypto.github.io`
2. Upload just these **2 required files** to its root (plus an optional simple `index.html` listing your apps):
   - `app-ads.txt` containing:
     ```text
     google.com, pub-8471293855339658, DIRECT, f08c47fec0942fa0
     ```
   - `.nojekyll` (empty file)
3. Enable **Settings → Pages → Source: Deploy from a branch → Branch: `main` / folder: `/ (root)` → Save**.
4. Open `https://gadgetexplorerpro-crypto.github.io/app-ads.txt` in your browser to verify the line appears. You never have to touch this repo again when adding new apps on the same AdMob account.

### Step 2 — LingoSphere Repo Pages (`LingoSphere-app`)
1. Push your changes from AI Studio to `gadgetexplorerpro-crypto/LingoSphere-app`.
2. In `gadgetexplorerpro-crypto/LingoSphere-app`, enable **Settings → Pages → Source: Deploy from a branch → Branch: `main` / folder: `/docs` → Save**.
3. LingoSphere's Privacy Policy, Terms of Service, and Delete Account pages are automatically live under `https://gadgetexplorerpro-crypto.github.io/LingoSphere-app/...` — matching all in-app legal links inside LingoSphere.

### Step 3 — Publish a Release Build for Live AdMob Requests
- `AdConfig.USE_TEST_ADS` automatically follows `BuildConfig.DEBUG`:
  - **Debug builds** use Google Test Ad IDs (`true`).
  - **Signed Release AAB/APK builds** uploaded to Google Play automatically use your production AdMob IDs (`pub-8471293855339658`).
- Once your live Play Store release sends real ad requests and every app's Developer Website is set to `https://gadgetexplorerpro-crypto.github.io`, AdMob verifies `app-ads.txt` within **24 hours to 7 days**.

---

## 📄 Included Legal & AdMob Files in Repository

| File Path | Purpose |
| :--- | :--- |
| [`/app-ads.txt`](./app-ads.txt) | Copy this single file to your root `gadgetexplorerpro-crypto.github.io` repo |
| [`/.nojekyll`](./.nojekyll) | Copy to root repo so GitHub Pages serves static files directly |
| [`/docs/index.html`](./docs/index.html) | LingoSphere GitHub Pages Landing Page (`/LingoSphere-app/`) |
| [`/docs/privacy-policy.html`](./docs/privacy-policy.html) | LingoSphere Play Store & AdMob Privacy Policy |
| [`/docs/terms.html`](./docs/terms.html) | LingoSphere Terms of Service |
| [`/docs/delete-account.html`](./docs/delete-account.html) | LingoSphere Account & Data Deletion Web Portal |
