# 🌐 LingoSphere — Real-Time AI Voice Calling & Language Tutor

**Package ID:** `com.aistudio.lingosphere.kxpqzm`  
**AdMob App ID:** `ca-app-pub-8471293855339658~7525338937`  
**AdMob Publisher ID:** `pub-8471293855339658`  
**GitHub Account:** `gadgetexplorerpro-crypto`

LingoSphere is an Android language learning application built with Kotlin, Jetpack Compose, Material 3, Firebase Authentication, Cloud Firestore, Google Play Billing 8.0.0, Google AdMob, and Gemini AI real-time bidirectional voice tutoring.

---

## ✅ Exact URLs for Google Play Console & AdMob `app-ads.txt`

Google AdMob **only crawls `app-ads.txt` at the root of your Developer Website** (`https://gadgetexplorerpro-crypto.github.io/app-ads.txt`). A project repository URL like `https://gadgetexplorerpro-crypto.github.io/LingoSphere-app` will **not** verify `app-ads.txt` unless the root repo `gadgetexplorerpro-crypto.github.io` exists.

### Step 1 — Create Your Free GitHub Pages Root Repository
1. On your **`gadgetexplorerpro-crypto`** GitHub account, create a **Public** repository named **exact match**:  
   `gadgetexplorerpro-crypto.github.io`
2. Upload these **6 files** directly to the root of `gadgetexplorerpro-crypto.github.io` (all ready in this project root and in `/docs/`):
   - `app-ads.txt` (contains `google.com, pub-8471293855339658, DIRECT, f08c47fec0942fa0`)
   - `.nojekyll`
   - `index.html`
   - `privacy-policy.html`
   - `terms.html`
   - `delete-account.html`
3. In `gadgetexplorerpro-crypto.github.io`, go to **Settings → Pages → Source: Deploy from a branch → Branch: `main` / folder: `/ (root)` → Save**.
4. Wait 1–2 minutes and open `https://gadgetexplorerpro-crypto.github.io/app-ads.txt` in your browser to confirm it displays:
   ```text
   google.com, pub-8471293855339658, DIRECT, f08c47fec0942fa0
   ```

### Step 2 — Paste These Exact URLs in Google Play Console
In **Google Play Console → LingoSphere**:
- **Developer Website (Store settings → Contact details → Website):**  
  `https://gadgetexplorerpro-crypto.github.io`
- **Privacy Policy (App content → Privacy policy):**  
  `https://gadgetexplorerpro-crypto.github.io/privacy-policy.html`
- **Delete Account & Data URL (App content → Data safety → Delete account URL):**  
  `https://gadgetexplorerpro-crypto.github.io/delete-account.html`
- **Terms of Service (also linked inside the app):**  
  `https://gadgetexplorerpro-crypto.github.io/terms.html`
- **AdMob `app-ads.txt` (crawled automatically by Google AdMob):**  
  `https://gadgetexplorerpro-crypto.github.io/app-ads.txt`

### Step 3 — Publish a Release Build to Send Live AdMob Requests
- `AdConfig.USE_TEST_ADS` automatically follows `BuildConfig.DEBUG`:
  - **Debug builds** use Google Test Ad IDs (`true`).
  - **Signed Release AAB/APK builds** uploaded to Google Play automatically use your production AdMob IDs (`pub-8471293855339658`).
- All in-app Privacy Policy, Terms of Service, and Delete Account links already point to `https://gadgetexplorerpro-crypto.github.io/...`.
- Once your live release build sends real ad requests, AdMob verifies `app-ads.txt` within **24 hours to 7 days**.

---

## 📄 Included Legal & AdMob Files in Repository

| File Path | Purpose |
| :--- | :--- |
| [`/app-ads.txt`](./app-ads.txt) | Root IAB Tech Lab `app-ads.txt` for AdMob (`pub-8471293855339658`) |
| [`/index.html`](./index.html) | Official Developer Website landing page |
| [`/privacy-policy.html`](./privacy-policy.html) | Play Store & AdMob Privacy Policy |
| [`/terms.html`](./terms.html) | Terms of Service |
| [`/delete-account.html`](./delete-account.html) | Account & Data Deletion Web Portal |
| [`/.nojekyll`](./.nojekyll) | Ensures GitHub Pages serves static files directly |
| `/docs/*` & `/public/*` | Synchronized copies of the root HTML & `app-ads.txt` files |
