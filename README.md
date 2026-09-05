# O.R.I.O.N. — Android Tablet MVP (kaynak kod iskeleti)

**Bu bir web sitesi değildir.** Native Kotlin + Jetpack Compose ile yazılmış,
Android Studio'da doğrudan açılıp derlenebilecek gerçek bir Android proje
iskeletidir.

## ÖNEMLİ — bu ortamın sınırı

Bu sohbet ortamında Android SDK, emülatör veya Gradle bağımlılık indirme ağı
yok. Yani burada `.apk` derleyip çalıştıramadım. Kod, gerçek Android
API'lerine karşı yazıldı (AudioRecord, BatteryManager, StatFs, ActivityManager,
ConnectivityManager, SpeechRecognizer) — placeholder/sahte mantık değil — ama
siz Android Studio'da açıp derlemeden önce derleme hatalarını (küçük import
eksikleri, sürüm uyumsuzlukları) gidermeniz gerekebilir.

## Nasıl açılır

1. Android Studio (Koala veya üzeri) → **Open** → bu klasörü seç.
2. Gradle sync'i bekleyin (ilk seferde bağımlılıkları indirir).
3. Bir tablet/emülatörde (landscape, API 26+) çalıştırın.
4. `app/build.gradle.kts` içindeki `DEFAULT_BACKEND_URL`'i kendi
   `orion-backend` adresinizle değiştirin, ya da uygulama içinden
   (ileride) Settings ekranından girin.

## Bu MVP'de gerçekten çalışan şeyler

| Özellik | Durum |
|---|---|
| 3 panelli landscape tablet arayüzü (Nav / AI Core / HUD) | ✅ Tamamen Compose, WebView yok |
| AI Core — gerçek Orion takımyıldızı, çoklu halka, parçacıklar, 6 durumlu animasyon (IDLE/LISTENING/THINKING/SEARCHING/EXECUTING/COMPLETE) | ✅ Canvas tabanlı, durum-güdümlü |
| Çift alkış aktivasyonu | ✅ Gerçek `AudioRecord` + RMS tepe algılama |
| Sesli komut | ✅ Gerçek `SpeechRecognizer` |
| RAM, Storage, Battery, Network | ✅ Gerçek Android API'leri; okunamayan değer asla uydurulmaz |
| CPU | ⚠️ Android 8+'da çoğu cihazda SELinux tarafından engellenir → dürüstçe "DATA UNAVAILABLE" |
| GPU | ❌ Unrooted Android'de hiçbir public API yok → her zaman "DATA UNAVAILABLE" (istediğiniz gibi) |
| Gerçek AI chat | ✅ Ama sizin barındıracağınız ayrı bir backend'e muhtaç — API anahtarı APK içinde YOK |
| Açılış animasyonu | ✅ ORION CONSTELLATION → AI CORE ACTIVATION → ONLINE |

## Henüz yapılmayanlar (bilinçli olarak — talep ettiğiniz sıralamaya göre)

Memory, Web Research, Vision, File Analysis, Coding, Education, Data Analysis,
Translation, Creative Studio, Agent System, Tool System, Security modülleri
henüz eklenmedi. Nav çubuğunda görünüyorlar ama şu an pasif — önce temel
sistemin (bu MVP) sağlam çalışması hedeflendi, siz onayladıktan sonra
modülleri tek tek gerçek işlevle ekleyebiliriz.

## Bu turda eklenen 4 özellik — ne gerçek, ne ek kurulum gerektiriyor

**1) Çift alkış / "Hey Orion" ile uyanma + sesli brifing**
Gerçek çalışıyor: `ClapDetector` (AudioRecord) veya `HotwordListener`
(SpeechRecognizer restart döngüsü) uyandırıyor, `BriefingBuilder` gerçek
saat + gerçek konum bazlı hava durumu (Open-Meteo) + gerçek pil yüzdesi +
gerçek hatırlatıcı sayısını tek cümlede birleştirip Android'in yerleşik
TTS'i ile söylüyor.
⚠️ Bilinen sınır: `SpeechRecognizer` "her an dinleyen" bir motor değildir,
birkaç saniyede bir oturumu yeniler — bu hem pil maliyeti hem küçük kaçırma
riski demektir. Gerçek "her an uyanık" bir "Hey Orion" için Picovoice
Porcupine gibi özel bir wake-word motoruna geçmeniz gerekir (ücretsiz
hesap + kendi "Hey Orion" `.ppn` dosyanızı üretmeniz lazım —
`HotwordListener.kt` içinde yükseltme yolu yorum olarak anlatıldı).
⚠️ Ayrıca: ClapDetector (ham AudioRecord) ile HotwordListener
(SpeechRecognizer) aynı anda mikrofonu paylaşmaya çalışıyor — birçok
cihaz aynı anda tek bir ses girişi istemcisine izin verir. Pratikte
ikisinden birini birincil uyandırma yöntemi seçmeniz daha güvenilir olur.

**2) Şarj animasyonu**
Gerçek `BATTERY_CHANGED` broadcast'i ile anlık şarj durumu okunuyor.
Şarj oluyorken AI Core'un etrafında yeşil, çakan şimşek çizgileri
dönüyor (`AiCoreView`'da `isCharging` parametresi), ve sağdaki pil
göstergesinde pil sembolünün üzerinde siyah bir şimşek ikonu beliriyor.

**3) Kısayollar**
- 3 hızlı alkış → `ClapEvent.TRIPLE_CLAP_DETECTED` → her şeyi durdurup
  (mikrofon, TTS, alkış/knock algılayıcıları) acil kapanış.
- 2 alkış: uyanıkken uykuya, uyurken uyanmaya geçiriyor (aynı jest, iki yönlü).
- Afet uyarısı: `DisasterAlertMonitor`, konuma yakın (varsayılan 150km,
  4.0+ büyüklük) depremleri gerçek bir halka açık deprem API'sinden
  çekiyor, core'u kırmızı yanıp söndürüyor ve mesajı sesli okuyor.
  ⚠️ **ÇOK ÖNEMLİ:** Bu API resmi AFAD/Kandilli kanalı DEĞİL, topluluk
  tarafından işletilen bir aynadır (`DisasterApiService.kt` içindeki
  uyarıyı okuyun). Ağ gecikmesi yüzünden Türkiye'nin resmi Hücresel Yayın
  (Cell Broadcast) deprem erken uyarı sisteminden asla daha hızlı olamaz.
  Bunu tamamlayıcı bir "farkındalık" özelliği olarak sunun, tek güvenlik
  kaynağı olarak asla pazarlamayın.
- Masaya çift vurma → `KnockDetector` (ivmeölçer) → Rahatsız Etme modu.
  ⚠️ Android, "Do Not Disturb access"i normal bir izin diyaloğu olarak
  vermez — kullanıcının Settings > Bildirimler'den bir kerelik elle
  açması gerekir; kod bunu kontrol edip yoksa sessizce hiçbir şey yapmıyor.

**4) Otonom uygulama kontrolü — burada dürüst olmam gerekiyor**
Android'in güvenlik modeli "bir kere izin ver, sonra her uygulamayı sürekli
kontrol et" şeklinde tek bir global mekanizma sunmuyor (iOS Shortcuts'ın
aksine). Üç farklı gerçeklik seviyesi var:

| İstek | Gerçeklik |
|---|---|
| "Yarın 14:00'e X hatırlatıcısı kur" | ✅ Tam otonom — `AlarmClock.ACTION_SET_REMINDER` özel izin istemez, tek adımda çalışır |
| "Spotify'da son şarkıyı çal" | ⚠️ Neredeyse otonom — Spotify'ın resmi App Remote SDK'sı bunu destekler ama SİZİN developer.spotify.com'da bir Client ID almanız ve kullanıcının BİR KEZ Spotify OAuth ekranını onaylaması gerekir (`SpotifyController.kt` içinde tam kod şablonu var, yorum satırında çünkü SDK Maven Central'da değil) |
| "X'e '...' yaz" | ⚠️ Kısmi — Android, sıradan bir üçüncü parti uygulamanın sessizce SMS/WhatsApp göndermesine izin vermez (spam SMS'i engellemek için bilinçli bir OS kısıtlaması). `MessagingHelper.kt` kişiyi bulup mesajı ilgili uygulamada hazır şekilde açıyor — göndermek için son bir dokunuş gerekiyor. Tamamen sıfır-dokunuşlu göndermek istiyorsanız tek yol `OrionAccessibilityService.kt` — ama bu, ekrandaki HER ŞEYİ okuyup sizin adınıza dokunabilen, hassasiyeti yüksek bir izin. Kodda bilerek inert bırakıldı; açmadan önce dosyadaki uzun uyarı yorumunu okuyun. |

## Yeni izinler (bu turda eklendi)
`ACCESS_COARSE/FINE_LOCATION` (hava durumu), `READ_CONTACTS` (mesaj
kısayolu), `ACCESS_NOTIFICATION_POLICY` (Rahatsız Etme). Hiçbiri
otomatik "her şeye izin ver" şeklinde değil — her biri tek bir özelliğe
bağlı, kodda nerede kullanıldığı yorumlarla belirtildi.

## Klasör yapısı


```
orion-android/
  app/src/main/java/com/orion/app/
    MainActivity.kt, OrionApplication.kt
    ui/theme/          — renk paleti, tipografi
    ui/components/      — AiCoreView, NavigationRail, SystemHudPanel, ChatPanel
    ui/screens/          — MainScreen (3 panelin birleştiği yer + boot sequence)
    core/                — CoreState, OrionViewModel (tüm state burada toplanır)
    audio/               — ClapDetector (AudioRecord), VoiceInputManager (SpeechRecognizer)
    network/             — Retrofit servis + ChatRepository (yalnızca KENDİ backend'inize konuşur)
    system/              — SystemStatsProvider (RAM/Storage/Battery/Network/CPU — hepsi gerçek API)
    settings/            — DataStore tabanlı ayarlar (backend URL, mic switch)

orion-backend/           — API anahtarını tutan ayrı FastAPI servisi (APK'nin dışında)
```
