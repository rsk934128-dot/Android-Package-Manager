<p align="center">
  <img src="docs/images/app_icon.jpg" width="128" height="128" alt="AppPulse Icon" style="border-radius: 28px;" />
</p>

<h1 align="center">AppPulse 🛡️⚡</h1>
<h3 align="center">Smart APK Downloader, Package Manager & Security Threat Auditor for Android</h3>

<p align="center">
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android-green.svg?logo=android" alt="Platform" /></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin-blue.svg?logo=kotlin" alt="Language" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?logo=jetpackcompose" alt="Jetpack Compose" /></a>
  <a href="https://m3.material.io/"><img src="https://img.shields.io/badge/Design-Material%203-blueviolet.svg" alt="Material 3" /></a>
  <a href="https://developer.android.com/tools/releases/platforms"><img src="https://img.shields.io/badge/Min%20SDK-24-orange.svg" alt="Min SDK" /></a>
  <a href="#threat--manifest-auditor"><img src="https://img.shields.io/badge/Security-Threat%20Audit%20Engine-red.svg" alt="Threat Engine" /></a>
</p>

---

<p align="center">
  <img src="docs/images/hero_banner.jpg" width="100%" alt="AppPulse Hero Dashboard" style="border-radius: 14px;" />
</p>

**AppPulse** হলো আধুনিক অ্যানড্রয়েড আর্কিটেকচার (**Modern Android Development - MAD**) এবং **Jetpack Compose** দিয়ে তৈরি একটি শক্তিশালী ও নিরাপদ **APK Downloader, Package Inspector এবং Security Threat Audit Utility**। এটি ব্যাকগ্রাউন্ডে যেকোনো APK ডাউনলোড ও নিরাপদ `FileProvider` দিয়ে ইন্সটল করার পাশাপাশি সিস্টেমে থাকা অ্যাপগুলোর ক্ষতিকর পারমিশন এবং ড্রপার/স্পাইওয়্যার আচরণ শনাক্ত করতে পারে।

---

## 📸 ভিজ্যুয়াল প্রিভিউ ও ইন্টারফেস (Visual Previews)

### 📥 ইউনিভার্সাল APK ডাউনলোডার (Rapid APK Downloader Engine)
<p align="center">
  <img src="docs/images/downloader_preview.jpg" width="100%" alt="Downloader Preview" style="border-radius: 12px;" />
</p>

* **রিয়েল-টাইম স্পিড মিটার:** লাইভ ডাউনলোড গতি (MB/s বা KB/s), স্থানান্তরিত বাইটস এবং শতাংশের সঠিক হিসেব।
* **কাস্টম ও প্রিসেট ইউআরএল:** যেকোনো সরাসরি `.apk` লিঙ্ক ইনপুট করা অথবা এক ক্লিকে F-Droid, Termux ও পরীক্ষামূলক আপডেট ডাউনলোড শুরু করার সুবিধা।
* **ক্যাশ মেমোরি ম্যানেজমেন্ট:** ডাউনলোড করা প্যাকেজগুলোর বিস্তারিত সাইজ দেখা এবং ১-ট্যাপে ইন্সটল বা মুছে ফেলার অপশন।

---

### 🛡️ থ্রেট ও ম্যানিফেস্ট অডিটর (Malware & Dropper Threat Scanner)
<p align="center">
  <img src="docs/images/threat_scanner_preview.jpg" width="100%" alt="Threat Scanner Preview" style="border-radius: 12px;" />
</p>

* **ড্রপার শনাক্তকরণ (Dropper Detection):** যেসকল অ্যাপ ইন্টারনেটের মাধ্যমে অনুমতি ছাড়া অন্য অ্যাপ ডাউনলোড বা ডিলিট করার ক্ষমতা রাখে (`REQUEST_INSTALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`) তাদের চিহ্নিত করে।
* **স্পাইওয়্যার ও স্টকারওয়্যার অ্যানালাইসিস:** ব্যাকগ্রাউন্ড নজরদারি এবং মনিটরিং ট্যাগ (`monitor.child`, `parental` ইত্যাদি) সনাক্ত করে।
* **ঝুঁকি মূল্যায়ন ও স্কোর:** প্রতিটি অ্যাপ বা ম্যানিফেস্ট কোড স্ক্যান করে ০ থেকে ১০০ স্কেলে নিরাপত্তা স্কোর এবং কারিগরি পরামর্শ (JADX-GUI রিভার্স ইঞ্জিনিয়ারিং ও স্যান্ডবক্সিং) প্রদান করে।

---

## 🌟 মূল বৈশিষ্ট্যসমূহ (Key Features)

### ১. নিরাপদ প্যাকেজ ইন্সটলার (Safe APK Installer via FileProvider)
* কোনো প্রাইভেট স্টোরেজ এক্সপোজ না করে আধুনিক `FileProvider` (`${applicationId}.provider`) দিয়ে নিরাপদ ইনস্টলেশন প্রক্রিয়া সম্পন্ন করে।
* অ্যান্ড্রয়েড ৮.০+ (API 26+) এর জন্য `canRequestPackageInstalls()` চেক এবং সিস্টেম সেটিংস শর্টকাট।

### ২. ইনস্টল করা অ্যাপসের পারমিশন অডিট (Package Inspector)
* ডিভাইসে থাকা সব অ্যাপসের তালিকা (ইউজার ও সিস্টেম অ্যাপ ফিল্টারিং)।
* টার্গেট SDK, ভার্সন কোড এবং বিপজ্জনক পারমিশন (`DUMP`, `SYSTEM_ALERT_WINDOW`, `RECORD_AUDIO`, `ACCESS_BACKGROUND_LOCATION`) বিশ্লেষণ।
* কুইক অ্যাকশন: অ্যাপ লঞ্চ করা, সিস্টেম সেটিংসে যাওয়া কিংবা সরাসরি আনইন্সটল করা।

### ৩. ডিভাইস ও নেটওয়ার্ক ডায়াগনস্টিকস (Device Diagnostics)
* ওয়াই-ফাই বা মোবাইল ডেটা কানেকশন স্ট্যাটাস ও লোকাল আইপি অ্যাড্রেস।
* ইন্টারনাল স্টোরেজ (Storage) ও র‍্যামের (RAM) রিয়েল-টাইম গ্রাফিক্যাল ব্যবহার।
* অ্যান্ড্রয়েড ভার্সন, API লেভেল ও সিকিউরিটি প্যাচ হিস্ট্রি।

---

## 📱 স্ক্রিন পরিচিতি (App Navigation Guide)

| ট্যাব | স্ক্রিন নাম | প্রধান কাজ |
| :---: | :--- | :--- |
| 📥 | **Downloader** | যেকোনো APK ডাউনলোড, প্রোগ্রেস ট্র্যাকিং ও `FileProvider` দিয়ে ইন্সটল। |
| 📦 | **Inspector** | ফোনে থাকা সকল অ্যাপের তালিকা, প্যাকেজ নেম ও পারমিশন অডিট। |
| 🛡️ | **Threat Audit** | ড্রপার, স্পাইওয়্যার ও ডিকম্পাইল্ড ম্যানিফেস্ট কোড স্ক্যান। |
| 📊 | **Diagnostics** | মেমোরি, নেটওয়ার্ক এবং অজানা উৎস থেকে অ্যাপ ইন্সটল পারমিশন চেক। |

---

## 🛠️ প্রযুক্তিগত স্ট্যাক (Tech Stack)

```
├── Framework: Jetpack Compose (Declarative Android UI)
├── Language: Kotlin 2.0+
├── Design System: Material Design 3 (M3)
├── Architecture: MVVM (Model-View-ViewModel) + StateFlow
├── Asynchronous: Kotlin Coroutines (Dispatchers.IO)
├── File Security: AndroidX FileProvider
└── Verification: JUnit 4 & Robolectric Test Suite
```

---

## 📂 ফাইল স্ট্রাকচার (Project Directory Structure)

```
app/
 ├── src/
 │    ├── main/
 │    │    ├── AndroidManifest.xml          # পারমিশন, FileProvider এবং অ্যাক্টিভিটি ঘোষণা
 │    │    ├── java/com/example/
 │    │    │    ├── MainActivity.kt         # মেইন এন্ট্রি পয়েন্ট এবং M3 বটম নেভিগেশন
 │    │    │    ├── manager/
 │    │    │    │    ├── ApkInstallerManager.kt      # FileProvider এবং ইনস্টলার ইঞ্জিন
 │    │    │    │    ├── DownloaderManager.kt        # ব্যাকগ্রাউন্ড স্ট্রিমিং ও ফাইল কন্ট্রোলার
 │    │    │    │    ├── SecurityAuditEngine.kt      # থ্রেট ডিটেকশন ও পারমিশন স্ক্যানার
 │    │    │    │    └── SystemDiagnosticsManager.kt # মেমোরি ও নেটওয়ার্ক টেলিমেট্রি
 │    │    │    ├── model/
 │    │    │    │    └── Models.kt                   # ডেটা ক্লাস ও এনোটেশনস
 │    │    │    ├── ui/
 │    │    │    │    ├── components/
 │    │    │    │    │    └── AppDetailSheet.kt     # অ্যাপের বিস্তারিত বটম শিট
 │    │    │    │    ├── screens/
 │    │    │    │    │    ├── DownloaderScreen.kt   # ডাউনলোড স্ক্রিন ও প্রোগ্রেস বার
 │    │    │    │    │    ├── PackageInspectorScreen.kt # অ্যাপস ও পারমিশন তালিকা
 │    │    │    │    │    ├── ThreatAuditScreen.kt  # ম্যানিফেস্ট অডিটর স্ক্রিন
 │    │    │    │    │    └── DiagnosticsScreen.kt  # সিস্টেম টেলিমেট্রি স্ক্রিন
 │    │    │    │    └── theme/                     # থিম, কালার ও টাইপোগ্রাফি
 │    │    │    └── viewmodel/
 │    │    │         └── MainViewModel.kt           # অ্যাপের স্টেট ও বিজনেস লজিক
 │    │    └── res/
 │    │         ├── drawable/                      # আইকন ও হিরো ব্যানার আর্টওয়ার্ক
 │    │         ├── values/strings.xml             # লোকালাইজড স্ট্রিং রিসোর্স
 │    │         └── xml/file_paths.xml             # FileProvider ফাইল পাথ কনফিগারেশন
 │    └── test/                                    # রোবোলেক্ট্রিক এবং ইউনিট টেস্ট ফাইল
docs/
 └── images/                                       # README-এর জন্য হাই-রেজ্যুলুশন প্রিভিউ ইমেজ
```

---

## 📲 সরাসরি মোবাইলে APK ডাউনলোড ও ইন্সটল করার নিয়ম (Download & Install APK)

আপনার অ্যানড্রয়েড স্মার্টফোনে অ্যাপটি সরাসরি ইন্সটল করার ৩টি সহজ উপায় নিচে দেওয়া হলো:

### উপায় ১: GitHub Releases থেকে সরাসরি ডাউনলোড (Automated CI/CD)
এই রিপোজিটরিতে **GitHub Actions** কনফিগার করা আছে (`.github/workflows/build-apk.yml`):
1. প্রজেক্টের **[Releases](https://github.com/)** ট্যাবে যান (অথবা **Actions** ট্যাবে)।
2. রিলিজ লিস্ট থেকে **`app-debug.apk`** ফাইলটি সরাসরি আপনার মোবাইল ফোনে ডাউনলোড করুন।
3. ডাউনলোড হওয়া ফাইলটিতে ট্যাপ করে **Install** বাটনে চাপুন।

### উপায় ২: AI Studio থেকে সরাসরি APK এক্সপোর্ট
1. AI Studio স্ক্রিনের উপরে ডান কোণায় থাকা **Settings / Export** মেনুতে যান।
2. **"Generate APK / Export"** অপশনটিতে ক্লিক করুন।
3. সাথে সাথে আপনার ডিভাইসে রেডিমেড `.apk` ফাইলটি ডাউনলোড হয়ে যাবে।

### ⚠️ মোবাইলে ইন্সটল করার সময় সতর্কতা:
* অ্যান্ড্রয়েডে প্রথমবারের মতো ব্রাউজার বা ফাইল ম্যানেজার দিয়ে সরাসরি কোনো APK ইন্সটল করার সময় **"Install Unknown Apps"** বা **"অজানা উৎস থেকে অ্যাপ ইন্সটলেশন"** অনুমতি চাইতে পারে।
* স্ক্রিনে দেখানো প্রম্পট থেকে কেবল অনুমতিটি (Allow) অন করে দিলেই অ্যাপটি নিরাপদে ইন্সটল হয়ে যাবে।

---

## 🔒 প্রয়োজনীয় পারমিশনসমূহ (Permissions Declared)

* `android.permission.INTERNET`: রিমোট সার্ভার থেকে APK ফাইল ডাউনলোড করার জন্য।
* `android.permission.ACCESS_NETWORK_STATE`: ওয়াই-ফাই বা ডেটা কানেকশনের অবস্থা পর্যবেক্ষণ করার জন্য।
* `android.permission.REQUEST_INSTALL_PACKAGES`: ডাউনলোড করা APK নিরাপদে ডিভাইসের অফিসিয়াল ইন্সটলার দিয়ে ইনস্টল করার জন্য।
* `android.permission.REQUEST_DELETE_PACKAGES`: ক্ষতিকর বা অপ্রয়োজনীয় অ্যাপ ব্যবহারকারীর সম্মতিতে আনইন্সটল করার প্রম্পট দেখানোর জন্য।
* `android.permission.QUERY_ALL_PACKAGES`: ডিভাইসে ইনস্টল থাকা প্যাকেজগুলো স্ক্যান করে সিকিউরিটি অডিট করার জন্য।

---

## 🚀 রান করার নির্দেশিকা (How to Build & Run)

```bash
# ১. ডিবাগ APK তৈরি করতে
gradle assembleDebug

# ২. সংযুক্ত ডিভাইসে ইন্সটল করতে
gradle installDebug

# ৩. ইউনিট ও রোবোলেক্ট্রিক টেস্ট রান করতে
gradle :app:testDebugUnitTest
```

---

<p align="center">
  <sub>Developed with ❤️ using Jetpack Compose, Kotlin & Modern Android Development guidelines.</sub>
</p>
