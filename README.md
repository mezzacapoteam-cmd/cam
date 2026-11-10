# MiaFotocamera - Progetto Android Completo per Tablet

Questo archivio contiene l'intero progetto Android pronto per essere compilato in un file **.apk** installabile su qualsiasi tablet Android (Samsung, Lenovo, Xiaomi, Huawei, Amazon Fire con Play Services o Android standard).

## Cosa contiene questo archivio:
1. `build.gradle`, `gradle.properties`, `settings.gradle` (i file originali del progetto MiaFotocamera)
2. `app/build.gradle` (configurazione del modulo app con CameraX per scatto ad alta definizione)
3. `app/src/main/AndroidManifest.xml` (permessi fotocamera e configurazione ottimizzata per schermi tablet xlarge)
4. `app/src/main/java/com/miafotocamera/app/MainActivity.kt` (codice sorgente completo in Kotlin)
5. `app/src/main/res/layout/activity_main.xml` (interfaccia utente con mirino a schermo intero e pulsanti grandi per tablet)
6. `.github/workflows/build-apk.yml` (compilazione cloud gratuita su GitHub Actions in 2 minuti)

---

## 3 Metodi per Creare il file .APK:

### METODO 1: Senza installare nulla (Gratuito con GitHub Actions)
1. Crea un nuovo repository privato o pubblico su [GitHub.com](https://github.com)
2. Carica tutti i file di questa cartella nel repository.
3. Vai nella scheda **Actions** in alto su GitHub.
4. Clicca su **Compila APK Android (MiaFotocamera)** -> **Run workflow**.
5. Dopo circa 2 minuti, il lavoro terminerà con successo verde!
6. Clicca sul lavoro e troverai l'artefatto **MiaFotocamera-Tablet-Debug-APK** da scaricare direttamente sul tuo tablet!

### METODO 2: Con Android Studio (Sul tuo computer)
1. Scarica e apri **Android Studio**.
2. Clicca su **Open...** e seleziona la cartella di questo progetto.
3. Attendi la sincronizzazione Gradle (Android Studio scaricherà automaticamente le dipendenze).
4. Nel menu in alto, clicca su: **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
5. Quando appare la notifica di completamento in basso a destra, clicca su **locate** per aprire la cartella con il file `app-debug.apk`.

### METODO 3: Da Riga di Comando (Terminale)
Apri il terminale nella cartella del progetto ed esegui:
- Su Windows: `gradlew assembleDebug`
- Su Mac / Linux: `./gradlew assembleDebug`

Troverai il file APK pronto in: `app/build/outputs/apk/debug/app-debug.apk`

---

## Come installare l'APK sul tuo Tablet Android:
1. Invia il file `app-debug.apk` al tablet (tramite cavo USB, Google Drive, WhatsApp o Telegram).
2. Sul tablet, apri il file scaricato.
3. Se appare il messaggio "Installazione da fonti sconosciute bloccata", tocca **Impostazioni** e attiva la spunta **Consenti da questa fonte**.
4. Tocca **Installa** e poi **Apri**.
5. Concedi l'autorizzazione all'uso della Fotocamera al primo avvio. Fatto!
