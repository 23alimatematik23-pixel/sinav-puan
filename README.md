# Sınav Puan Tarayıcı (Android)

Öğretmenler için sınav puanı tarama uygulaması.

1. Sınıf, şube ve sınıf mevcudu girilir.
2. Sınıf listesi manuel eklenir ya da kameradan/galeriden taranır (ML Kit, cihaz üzerinde OCR) ve düzenlenebilir tabloya aktarılır.
3. Her öğrencinin yanında **1. Sınav** ve **2. Sınav** butonları bulunur. Butona basılıp sınav kağıdı taranır:
   kırmızı kalemle yazılmış puanlar renk filtresiyle ayrılır, okunur ve soru numaralarına eşleştirilir.
   Puanı yazılmamış soru 0 kabul edilir. Kaydetmeden önce puanlar kontrol edilip düzeltilebilir.
4. Sınıfın tamamı tablo halinde **PDF**, **Excel (.xlsx)** veya **Word (.docx)** olarak alınır ve WhatsApp ile paylaşılır.
5. Veli telefonu girilmiş öğrencilerin velilerine puanları WhatsApp mesajı olarak gönderilir.

## Puan eşleştirme
- **Puan tablosu**: Kağıtta basılı `1 2 3 ...` satırı/sütunu varsa altına/yanına yazılan kırmızı puanlar ilgili soruya yazılır.
- **Soru numaraları**: `1)`, `2.`, `Soru 3:` gibi numaralar bulunur; her kırmızı puan aynı sütunda üstündeki en yakın soruya atanır (iki sütunlu kağıtlar desteklenir).
- Hiçbiri bulunamazsa kırmızı puanlar yukarıdan aşağıya sırayla yerleştirilir.
- Diğer puanların toplamına eşit kırmızı sayı (öğretmenin yazdığı toplam) yok sayılır.

## Derleme
```
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest
```
