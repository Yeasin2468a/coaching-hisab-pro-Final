# Coaching Hisab Pro v20 — Android APK via GitHub

এই project-এ V20 web app-কে একটি native Android WebView app হিসেবে package করা হয়েছে।

## GitHub-এ আপলোড
1. GitHub-এ নতুন repository তৈরি করুন।
2. এই ZIP-এর **ভেতরের সব file/folder** repository root-এ upload করুন।
3. নিশ্চিত করুন `.github/workflows/build-apk.yml` ঠিক ওই path-এ আছে।
4. Commit করুন।

## APK তৈরি
GitHub repository → **Actions** → **Build Coaching Hisab Pro APK** → run শেষ হলে workflow খুলুন → **Artifacts** → `Coaching-Hisab-Pro-v20` download করুন।

Artifact ZIP-এর ভিতরে `Coaching-Hisab-Pro-v20.apk` থাকবে। Android ফোনে APK install করুন।

## গুরুত্বপূর্ণ
- এটি installable **Debug APK**; GitHub Actions-এর debug signing ব্যবহার করে।
- একই debug key না থাকায় পরের build-কে পুরোনো build-এর উপর update হিসেবে install না-ও করা যেতে পারে; প্রয়োজন হলে পুরোনো app uninstall করে নতুনটি install করুন।
- App-এর হিসাব `WebView localStorage`-এ থাকে। নিয়মিত Full Backup রাখুন।
- Full Backup/CSV Android app mode-এ Downloads/Coaching Hisab Pro folder-এ save হবে।
- Restore-এর জন্য Android file picker ব্যবহার করা হয়েছে।
- PWA service worker APK mode-এর জন্য প্রয়োজনীয় নয়; app-এর HTML সরাসরি local asset হিসেবে চলে, তাই offline ব্যবহার সম্ভব।
