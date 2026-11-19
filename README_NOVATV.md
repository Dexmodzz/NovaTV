# NovaTV

NovaTV è un fork di [SmartTube](https://github.com/yuliskov/SmartTube) con una nuova interfaccia scura e moderna
per Android TV. **Cambia solo la grafica**: API, riproduzione, blocco pubblicità e SponsorBlock sono quelli di
SmartTube, e i sottomoduli `MediaServiceCore` e `SharedModules` **non sono stati toccati**.

- Pacchetto: `app.novatv` (beta `app.novatv.beta`, fdroid `app.novatv.fdroid`): si installa accanto a SmartTube.
- Tutte le impostazioni di SmartTube e i loro valori salvati restano validi.
- Nessun logo, nome, icona o font di YouTube/Google: il font è Inter (licenza SIL OFL, `assets/fonts/INTER_LICENSE.txt`).
- Tutto il lavoro è sul branch `novatv`; `master` segue upstream.

---

## Indice

1. [Compilare e installare](#1-compilare-e-installare)
2. [Cosa è cambiato, fase per fase](#2-cosa-è-cambiato-fase-per-fase)
3. [File per file](#3-file-per-file)
4. [Riallineare il fork con upstream](#4-riallineare-il-fork-con-upstream)

---

## 1. Compilare e installare

**JDK:** OpenJDK **11–14** (AGP 7.4.2 richiede almeno 11; con JDK più nuovi l'app può crashare).

```bash
git clone --recursive <url-del-fork> && cd SmartTube && git checkout novatv
# se il clone era già fatto senza --recursive:
git submodule update --init --recursive

JAVA_HOME=/percorso/jdk-11 ./gradlew assembleStstableDebug     # build di controllo
JAVA_HOME=/percorso/jdk-11 ./gradlew assembleStstableRelease   # APK da installare
```

APK: `smarttubetv/build/outputs/apk/ststable/release/NovaTV_stable_<versione>_<abi>.apk`
(per Android TV a 64 bit `arm64-v8a`, altrimenti `armeabi-v7a` o `universal`).

```bash
adb connect <ip-tv>:5555
adb install -r smarttubetv/build/outputs/apk/ststable/release/NovaTV_stable_*_arm64-v8a.apk
```

> **Installa sempre la build release.** La build debug attiva in `SharedModules` (OkHttpCommons, `BuildConfig.DEBUG`)
> il profiler di rete e il logger HTTP completo: ogni pezzo di video passa dal log, l'avvio di un video
> sale a 6–11 s, la memoria si riempie e può comparire "errore sorgente". La release parte in circa 0,6 s.
>
> **Firma:** se esiste `keystore.properties` la release usa quella chiave; altrimenti viene firmata con la chiave
> debug standard di Android. Così la release si compila sempre e si installa sopra una debug senza perdere i dati.
> Per pubblicare l'app usa una tua chiave (`keystore.properties`), e ricorda che cambiare chiave obbliga a
> disinstallare.

---

## 2. Cosa è cambiato, fase per fase

| Fase | Contenuto |
|---|---|
| **0 – Identità** | Pacchetto `app.novatv*`, nome "NovaTV", APK `NovaTV_*`, icona e banner nuovi, authority di ricerca vocale aggiornate, aggiornamento automatico disattivato (il fork non scarica mai l'APK di SmartTube), Firebase applicato solo se `google-services.json` contiene il pacchetto NovaTV. |
| **1 – Design token** | Palette (sfondo `#0F0F10`, superfici `#1C1C1F`/`#26262A`, testo `#F1F1F1`/`#AAAAAA`, accenti arancio/blu/viola/verde), raggi, spaziature, scala dei testi; temi `App.Theme.Nova.Dark/Oled`; font Inter su tutte le schermate; gli 8 schemi colore di SmartTube mantengono nome e posizione (l'indice salvato resta valido) ma puntano a Nova Scuro o Nova OLED. |
| **2 – Header e menu laterale** | Header: logo, barra di ricerca a tutta riga (con microfono), orologio + data, avatar. Menu laterale da 112dp con icona + etichetta. Schermata di ricerca con la stessa barra. Le *chip* delle categorie sono state poi tolte su richiesta: tutte le sezioni sono di nuovo nel menu laterale. |
| **3 – Schede video e Home** | Schede arrotondate 14dp su superficie scura, anello bianco + zoom 1,05 sul focus, avatar reale del canale accanto al titolo, "canale" e "visualizzazioni · data" su righe separate, barra di avanzamento color accento. Navigazione interrompibile (destra poi giù non si blocca durante lo scorrimento). Riavvio automatico dopo il cambio di scala. |
| **4 – Player** | Pulsanti tondi (etichetta solo sul focus), Play/Pausa bianco con anello accento, barra più spessa con tempi ai lati, layout centrato, gradienti; header in alto con titolo, avatar, canale · iscritti e orologio; chip "SponsorBlock: N salti"; i pulsanti si fermano ai bordi della riga invece di saltare sulla barra. |
| **5 – Impostazioni** | Nuova voce **NovaTV** nella griglia delle impostazioni: tema Scuro/OLED, colore d'accento, schede per riga (3–6), anteprima sulla scheda (off/muta/audio) e ritardo, orologio e formato 24h, animazioni di focus, posizione del titolo nel player (in alto / sopra le informazioni). Riquadri impostazioni con anello di focus. |
| **Dopo** | Logo con "a" leggibile (Inter SemiBold), banner del launcher = logo dell'header, barra di ricerca a tutta riga su ogni schermo, risultati di ricerca subito sotto la barra (che si nasconde solo quando i risultati la coprono). |

---

## 3. File per file

Regola generale: **il codice nuovo sta in file nuovi** (`novatv_*`, `Nova*`); nei file di upstream ci sono solo
piccoli agganci, sempre marcati con un commento `NovaTV`. Per trovarli tutti:

```bash
git grep -n "NovaTV" -- ':!README_NOVATV.md'
```

### 3.1 File nuovi

**Modulo `common`**

| File | Cosa fa |
|---|---|
| `common/.../prefs/NovaTVData.java` | Preferenze aggiunte da NovaTV in un file proprio (`novatv_prefs`): accento, ritardo anteprima, animazioni di focus, posizione titolo player. Applica l'overlay del colore d'accento. |
| `common/.../presenters/settings/NovaTVSettingsPresenter.java` | Schermata "NovaTV" nelle impostazioni. Dove esiste già un'impostazione di SmartTube (schema colore, scala griglia, anteprima, orologio, 12/24h) scrive quella stessa impostazione. |
| `common/.../misc/NovaFonts.java` | Applica Inter ai `TextView` creati da layout (factory del `LayoutInflater`), su tutte le versioni di Android. |
| `common/src/main/res/values*/novatv_strings.xml` | Testi della schermata NovaTV (inglese e italiano). |
| `common/src/main/res/drawable-nodpi/settings_novatv.png` | Icona (tavolozza) della voce NovaTV; generata da `tools/novatv/gen_settings_icon.py`. |

**Modulo `smarttubetv` – Java**

| File | Cosa fa |
|---|---|
| `tv/ui/browse/NovaSections.java` | Divisione sezioni menu/chip. Chip disattivate (`CHIPS_ENABLED = false`): tutte le sezioni stanno nel menu laterale. |
| `tv/ui/playback/other/NovaPlayerHeader.java` | Header in alto nel player (titolo/canale e orologio), con dissolvenza interrompibile insieme ai controlli. |
| `tv/ui/playback/other/NovaPlayerTitle.java` | Riempie titolo + avatar + "canale · iscritti" nell'header in alto o nel blocco sopra le informazioni, secondo l'impostazione. |
| `tv/util/NovaAvatars.java` | Avatar reali dei canali sulle schede e nel player: cache per canale, al massimo 2 richieste alla volta, solo per le schede visibili. |
| `tv/util/NovaInitialDrawable.java` | Cerchio colorato con l'iniziale del canale, mostrato finché l'avatar non è caricato. |

**Modulo `smarttubetv` – risorse**

| File | Cosa fa |
|---|---|
| `res/values/novatv_colors.xml` | Palette e override dei colori `lb_*` di Leanback. |
| `res/values/novatv_dimens.xml` | Raggi, spaziature, testi, misure di header, menu, player, barra di ricerca; override di alcune dimensioni Leanback (`lb_*`). |
| `res/values/novatv_themes.xml` | Temi `App.Theme.Nova.*`, attributi `novaAccent`/`novaChipBg`/`novaPlayBg`, overlay `NovaAccent.Orange/Blue/Violet/Green`. |
| `res/values*/novatv_strings.xml` | Testi dell'interfaccia (es. "SponsorBlock: %d salti"). |
| `res/drawable/novatv_*.xml` | Sfondi: scheda, anello di focus, pill, barra di ricerca, pulsanti del player, chip, menu laterale, barre di avanzamento per ogni accento, banner del launcher (`novatv_banner.xml`). |
| `res/color/novatv_*.xml` | Colori del testo di pill e chip per stato. |
| `res/layout/lb_control_button_secondary.xml` | Pulsante del player a forma di pill con etichetta (sovrascrive quello di Leanback). |
| `res/layout/novatv_player_title_block.xml` | Blocco titolo/canale sopra la riga delle informazioni. |
| `res/mipmap-nodpi/novatv_logo.png` | Logo dell'header, usato anche come banner e `android:logo`. |
| `assets/fonts/inter_*.ttf` + `INTER_LICENSE.txt` | Font Inter (OFL). |

**Strumenti** (`tools/novatv/`, servono solo per rigenerare le immagini)

| File | Cosa fa |
|---|---|
| `gen_icons.py` | Icona dell'app (quadrato arrotondato + triangolo play). |
| `gen_logo.py` | Logo dell'header con il testo in Inter SemiBold (richiede `pycairo`). |
| `gen_settings_icon.py` | Icona della voce NovaTV nelle impostazioni. |

### 3.2 File di upstream modificati

**Build e identità**

| File | Modifica |
|---|---|
| `smarttubetv/build.gradle` | `applicationId` → `app.novatv*`, nome APK `NovaTV`, Firebase solo se configurato per NovaTV, release firmata con la chiave debug se manca `keystore.properties`. |
| `smarttubetv/src/main/AndroidManifest.xml` | `android:banner` → `@drawable/novatv_banner`, `android:logo` → `@mipmap/novatv_logo`. |
| `smarttubetv/src/*/res/values/strings.xml` | Nome dell'app. |
| `leanbackassistant/src/*/res/values/strings.xml`, `.../xml/searchable.xml` | Authority della ricerca vocale con i nuovi pacchetti. |
| `smarttubetv/src/*/res/mipmap-*/app_icon*.png`, `app_banner.png` | Icona e banner NovaTV. |
| `common/.../dialogs/AppUpdatePresenter.java` | L'aggiornamento automatico non parte se il pacchetto non è di SmartTube. |

**Temi e impostazioni**

| File | Modifica |
|---|---|
| `common/.../prefs/MainUIData.java` | Gli 8 schemi colore puntano ai temi Nova (nomi e ordine invariati). |
| `common/.../misc/MotherActivity.java` | Applica l'overlay del colore d'accento dopo il tema. |
| `common/.../misc/AppDataSourceManager.java` | Aggiunge la voce NovaTV alla griglia delle impostazioni. |
| `common/.../settings/MainUISettingsPresenter.java`, `UIScaleSettingsPresenter.java` | Riavvio automatico dopo il cambio di scala dell'interfaccia o della griglia (senza, la UI restava tagliata oltre 1.3x). |
| `smarttubetv/.../ui/common/LeanbackActivity.java` | Zoom di focus da `NovaTVData` (animazioni on/off). |
| `smarttubetv/.../util/ViewUtil.java` | `FOCUS_ZOOM_FACTOR` non più `final`, per poterlo configurare. |
| `smarttubetv/.../presenter/SettingsCardPresenter.java` | Riquadri delle impostazioni con anello di focus. |
| `smarttubetv/src/main/res/values/colors.xml`, `dimens.xml` | Pochi valori allineati alla palette e al menu stretto (`lb_browse_rows_margin_start`). |

**Header, menu, Home, ricerca**

| File | Modifica |
|---|---|
| `res/layout/lb_title_view.xml` | Header riscritto: logo, barra di ricerca a tutta riga con microfono, orologio/data, avatar. **Tutti gli id originali sono mantenuti.** |
| `res/layout-ldrtl/lb_title_view.xml` | **Eliminato**: il nuovo layout si specchia da solo per le lingue da destra a sinistra. |
| `tv/ui/widgets/browse/NavigateTitleView.java` | Barra di ricerca, microfono, navigazione con il telecomando nell'header, pulsante "torna al video" che non occupa spazio quando non serve. |
| `tv/ui/browse/BrowseFragment.java` | Selezione delle sezioni, header nascosto quando il menu si apre sopra righe scorse. |
| `tv/presenter/IconHeaderItemPresenter.java`, `res/layout/icon_header_item.xml` | Voci del menu laterale (icona sopra, etichetta sotto, selezione arrotondata). |
| `tv/presenter/VideoCardPresenter.java`, `ShortsCardPresenter.java` | Schede arrotondate, avatar del canale. |
| `tv/ui/widgets/complexcardview/ComplexImageCardView.java` | Riga avatar + canale, sottotitolo su due righe. |
| `tv/ui/widgets/complexcardview/ComplexImageView.java` | Ritardo dell'anteprima da `NovaTVData`. |
| `res/layout/lb_search_bar.xml`, `res/drawable/rounded_search_bg.xml` | Barra della schermata di ricerca uguale a quella della Home (id originali mantenuti). |
| `tv/ui/mod/leanback/search/SearchSupportFragment.java` | Risultati subito sotto la barra; barra nascosta solo quando i risultati la coprono. |
| `tv/presenter/ChannelHeaderPresenter.java` | Nella pagina canale la barra di ricerca non mostra il logo. |

**Player**

| File | Modifica |
|---|---|
| `res/layout/lb_playback_fragment.xml` | Header in alto (titolo, avatar, canale · iscritti, orologio). |
| `res/layout/lb_playback_transport_controls_row.xml` | Layout centrato, tempi ai lati della barra, tempo rimanente in alto a destra, chip SponsorBlock. |
| `res/drawable/player_background_controls.xml`, `player_background_suggestions.xml` | Gradienti. |
| `tv/ui/playback/PlaybackFragment.java` | Mostra/nasconde l'header in alto insieme ai controlli; riga informazioni più corta (canale e iscritti stanno nel titolo). |
| `tv/ui/mod/leanback/playerglue/tweaks/MaxControlsVideoPlayerGlue.java` | Aggancio di `NovaPlayerTitle`, testi centrati. |
| `.../tweaks/PlaybackTransportRowPresenter.java` | Chip SponsorBlock (conta i segmenti, esclusi i capitoli). |
| `.../tweaks/ControlBar.java` | Spazio minimo tra i pulsanti, sinistra/destra un pulsante alla volta con stop ai bordi, rimossa la `LayoutTransition` che faceva perdere il focus. |
| `.../tooltips/ControlButtonPresenterSelector.java` | Etichetta del pulsante visibile solo sul focus, Play/Pausa bianco più grande. |
| `tv/ui/mod/leanback/misc/SeekBar.java` | Colori della barra (traccia e buffer). |

**Libreria Leanback inclusa nel repo**

| File | Modifica |
|---|---|
| `leanback-1.0.0/.../widget/GridLayoutManager.java` | Un'unica modifica (due punti marcati `MOD: NovaTV`): in `onInterceptFocusSearch` i tasti che cambiano riga non vengono più scartati mentre la riga scorre (navigazione interrompibile). |

**Non toccati:** `MediaServiceCore/`, `SharedModules/` (sottomoduli: API, blocco pubblicità, SponsorBlock).

---

## 4. Riallineare il fork con upstream

Il modello è semplice: `master` = copia esatta di upstream, `novatv` = master + le modifiche NovaTV.
Si aggiorna sempre prima `master`, poi si fa il merge di `master` in `novatv`.

### 4.1 Una volta sola

```bash
# Se "origin" è il tuo fork su GitHub, aggiungi il repo originale come "upstream"
git remote add upstream https://github.com/yuliskov/SmartTube.git
git remote -v

# Git ricorda come hai risolto un conflitto e la volta dopo lo risolve da solo
git config rerere.enabled true

# Non toccare i fine riga: molti file Java di SmartTube sono CRLF
git config core.autocrlf false
```

(Se `origin` punta già a `yuliskov/SmartTube`, nei comandi qui sotto usa `origin` al posto di `upstream`.)

### 4.2 Ogni aggiornamento

```bash
git fetch upstream

# 1) master = upstream (solo fast-forward: master non contiene modifiche nostre)
git checkout master
git merge --ff-only upstream/master

# 2) porta le novità in novatv
git checkout novatv
git merge master

# 3) sottomoduli alla versione indicata da upstream (non vanno modificati)
git submodule update --init --recursive

# 4) controllo
JAVA_HOME=/percorso/jdk-11 ./gradlew assembleStstableDebug assembleStstableRelease
```

### 4.3 Perché i conflitti sono rari e dove possono capitare

- La maggior parte del lavoro è in **file nuovi** (`novatv_*`, `Nova*`): upstream non li conosce, quindi non possono andare in conflitto.
- I file di upstream modificati hanno quasi sempre **poche righe**, marcate `NovaTV`. Un conflitto lì si risolve tenendo il codice nuovo di upstream e rimettendo il blocco `NovaTV`.
- Le risorse di Leanback sovrascritte (`lb_*.xml`) sono file dell'app con lo stesso nome di quelli della libreria: un aggiornamento della libreria non le tocca.

File in cui un conflitto è possibile, e come risolverlo:

| File | Cosa fare |
|---|---|
| `res/layout/lb_title_view.xml`, `lb_search_bar.xml`, `lb_playback_fragment.xml`, `lb_playback_transport_controls_row.xml` | Sono riscritti. Tieni la versione NovaTV (`git checkout --ours <file>`) e aggiungi a mano gli eventuali **nuovi id** introdotti da upstream (il codice Java li cerca per id). |
| `res/layout-ldrtl/lb_title_view.xml` | Se upstream lo modifica avrai un conflitto "modificato/eliminato": lascialo eliminato (`git rm <file>`). |
| `NavigateTitleView.java`, `BrowseFragment.java`, `PlaybackFragment.java`, `ControlBar.java`, `SearchSupportFragment.java` | Tieni le modifiche di upstream e reinserisci i blocchi `NovaTV`. |
| `MainUIData.java` (schemi colore) | Se upstream aggiunge uno schema, aggiungilo in fondo puntandolo a `App.Theme.Nova.Dark` o `.Oled`, senza cambiare l'ordine degli altri (l'indice salvato deve restare valido). |
| `build.gradle` | Tieni le novità di upstream (versione, dipendenze) e le modifiche NovaTV: applicationId `app.novatv*`, nome APK `NovaTV`, condizione Firebase, firma di riserva (`signingConfigs.debug`). |
| Icone/banner `.png`, `strings.xml` (`app_name`) | Tieni la versione NovaTV: `git checkout --ours <file>`. |
| `leanback-1.0.0/.../GridLayoutManager.java` | Cerca `MOD: NovaTV` (due punti in `onInterceptFocusSearch`) e riapplica la modifica. |

Durante un merge:

```bash
git status                          # file in conflitto
git checkout --ours  <file>         # tieni NovaTV
git checkout --theirs <file>        # tieni upstream
git add <file> && git commit        # chiudi il merge
git merge --abort                   # oppure annulla tutto e torna a prima del merge
```

> **File CRLF:** molti file Java (es. `MotherActivity.java`, `AppDataSourceManager.java`, `ViewUtil.java`,
> `NavigateTitleView.java`, `MaxControlsVideoPlayerGlue.java`) usano fine riga Windows. Modificali con un editor
> che conserva i fine riga, altrimenti il diff diventa l'intero file e ogni merge successivo va in conflitto.
> Per controllare: `file <percorso>` deve dire "with CRLF line terminators".

### 4.4 Dopo l'aggiornamento

1. Compila debug e release (vedi sopra).
2. Installa la release sulla TV e controlla velocemente:
   - Home: header, menu laterale, schede, avatar;
   - ricerca: barra e risultati;
   - player: header in alto, pulsanti, SponsorBlock;
   - Impostazioni → NovaTV.
3. Se upstream aggiunge una nuova sezione al menu, controlla che abbia l'icona nel menu laterale (`IconHeaderItemPresenter`).
