# Kelime Sayacı

Android için son derece hafif ve hızlı bir **kelime sayacı** uygulaması.
PDF, Word (DOCX) ve düz metin (TXT) belgelerindeki kelime sayısını hesaplar.

> 🔒 **Gizlilik:** Tüm belge işleme tamamen cihaz üzerinde yapılır. Hiçbir
> dosya veya veri sunucuya gönderilmez; internet bağlantısı gerekmez.

## Özellikler

- 📄 **PDF, DOCX ve TXT** desteği
- 🔢 Word'e benzeyen kelime sayımı (satır sonu heceleme tireleri birleştirilir)
- 🕘 Son analizlerin saklandığı **geçmiş** (cihazda yerel olarak)
- 📤 Sonucu **paylaşma** ve kelime sayısını **kopyalama**
- 📥 Dosya yöneticisinden "Aç" veya başka uygulamalardan "Paylaş" ile dosya alma
- 🌙 Material 3 tabanlı, açık/koyu temaya uyumlu arayüz

## Teknolojiler

- Kotlin + Jetpack Compose (Material 3)
- MVVM: `AndroidViewModel` + `StateFlow`
- [PDFBox-Android](https://github.com/TomRoush/PdfBox-Android) (PDF metin çıkarımı)
- DOCX için yerleşik `XmlPullParser` (ek bağımlılık yok)

## Telefona kurmak için APK indir (en kolayı)

Bilgisayara hiçbir şey kurmadan, hazır bir **debug APK** indirebilirsiniz:

1. **Actions** sekmesine gidin → **Build APK** workflow'unu açın.
2. En son başarılı çalışmayı seçin → sayfanın altındaki **Artifacts** bölümünden
   **`kelime-sayaci-debug-apk`**'yı indirin (bir `.zip` iner).
3. Zip'i açın → içindeki `app-debug.apk` dosyasını telefona kopyalayıp kurun
   ("bilinmeyen kaynaklardan kuruluma izin ver" demeniz gerekebilir).

> APK, `main`'e her push'ta otomatik üretilir. Workflow'u elle tetiklemek için
> **Actions → Build APK → Run workflow** kullanabilirsiniz.

## Kaynaktan derleme

**Gereksinimler:** [Android Studio](https://developer.android.com/studio)

1. Android Studio'yu açın.
2. **Open** ile bu projeyi seçin ve Gradle senkronizasyonunun tamamlanmasını bekleyin.
3. Bir emülatör veya fiziksel cihaz seçip **Run** ile çalıştırın.

> `debug` derlemesi için `debug.keystore` depoya dahil değildir (`.gitignore`).
> Android Studio yerel derlemelerde bunu otomatik üretir; CI ortamında ise
> [`build-apk.yml`](.github/workflows/build-apk.yml) workflow'u `keytool` ile
> standart debug kimlik bilgileriyle oluşturur.

### Release derlemesi

Release derlemesi R8/ProGuard ile küçültme (`isMinifyEnabled = true`) ve kaynak
küçültme açık şekilde gelir. İmzalama için aşağıdaki ortam değişkenlerini tanımlayın:

- `KEYSTORE_PATH` (varsayılan: `<proje>/my-upload-key.jks`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`

(Anahtar alyası `upload` olarak beklenir.)
