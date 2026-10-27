# DuGO

## About

DuGO is a mobile blood-donation networking app — an Android/Kotlin + Compose product that connects people who need blood with verified donors around them, and lets those donors respond directly in-app.

### What it does

* **Request blood instantly.** Anyone can post a blood request with blood type, hospital, location, urgency, notes and contact info. Requests live in Firestore under `requests/{auto-id}` and are queryable by type/location.
* **Find donors by proximity.** The home screen shows donors on a map, filtered by blood type and distance. The app ships with 70 sample donors across 14 Cebu regions for testing, and merges them with real registered donors from Firestore so the "find nearest donor" flow works anywhere. Sample donors are clearly marked as test data and have no uid — real donors can be messaged.
* **Message directly.** A conversation document per donor-requester pair (`conversations/{uidA_uidB}`) stores participant metadata and a sub-collection of messages. No fake chat history is ever shown — messages are real Firestore data only.
* **Keep donor profiles real.** Profiles in `users/{uid}` store fullName, username, bloodType, location, contactNumber, role, availableToday, totalDonations, joinedDate, email, latitude/longitude. Your own profile and donation history start empty for new accounts — no placeholder data.
* **Local-first today, Firebase coming.** Currently auth is local/in-memory with a debug admin login. Planned 2026-10-19 work adds Firebase Google/Facebook sign-in and FCM push notifications, with the Settings toggles becoming the FCM preference source.

### How it’s built

* **App:** Kotlin + Jetpack Compose, branch `feature/profile-settings`
* **Data:** Firestore collections `users`, `requests`, `conversations/{id}/messages`. Security rules live in `firestore.rules` and deploy with `firebase deploy --only firestore:rules`. All queries are single-field, so no composite indexes are needed.
* **Auth mapping:** Username accounts map to `<username>@users.dugo.example.com` (reserved domain, no mail, so password reset can’t work for them).
* **Distribution:** APKs are uploaded to your Google Drive via rclone `gdrive:` remote.

In short: DuGO turns a blood emergency into a searchable, mappable, chat-enabled donor network — testable with sample data, but real for actual users once they sign up.
