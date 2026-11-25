# Gamer's Vault (Kotlin + XML) — Termux & GitHub Builder

Hii ni **100% Native Android App** ya **Gamer's Vault** iliyoandikwa kwa **Kotlin** na **Android XML**, ikitengeneza `.apk` moja kwa moja kupitia **GitHub Actions**.

## Hatua za Kutumia Kwenye Termux

1. **Safisha `.git` ya zamani na u-extract ZIP:**
   ```bash
   rm -rf ~/.git ~/projects/.git
   mkdir -p ~/projects
   unzip -q ~/storage/downloads/GamersVault.zip -d ~/projects/
   cd ~/projects/GamersVault
   ```

2. **Push kwenda GitHub (`mickster19`):**
   ```bash
   git init
   git add .
   git commit -m "Initial Gamer's Vault native Android setup"
   git branch -M main
   git remote add origin https://github.com/mickster19/JINA_LA_REPO.git
   git push -u origin main
   ```

3. **Pakua APK:**
   - Fungua repository yako GitHub -> **Actions** -> **Build Gamer's Vault APK**.
   - Ikimaliza, shuka sehemu ya **Artifacts** na pakua **GamersVault-Debug-APK**.
