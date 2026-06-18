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

## Kurulum ve Çalıştırma

**Gereksinimler:** [Android Studio](https://developer.android.com/studio)

1. Android Studio'yu açın.
2. **Open** ile bu projeyi seçin ve Gradle senkronizasyonunun tamamlanmasını bekleyin.
3. Bir emülatör veya fiziksel cihaz seçip **Run** ile çalıştırın.

> Varsayılan olarak `debug` derlemesi, depodaki `debug.keystore` ile imzalanır;
> ek bir yapılandırma gerekmez.

### Release derlemesi

Release derlemesi R8/ProGuard ile küçültme (`isMinifyEnabled = true`) ve kaynak
küçültme açık şekilde gelir. İmzalama için aşağıdaki ortam değişkenlerini tanımlayın:

- `KEYSTORE_PATH` (varsayılan: `<proje>/my-upload-key.jks`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`

(Anahtar alyası `upload` olarak beklenir.)
