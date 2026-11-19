# Privacy Policy for NovaTV

**Last Updated:** November 19, 2026

This Privacy Policy applies to every build of **NovaTV** (Package IDs: `app.novatv`, `app.novatv.beta`,
`app.novatv.fdroid`).

## 1. Data Collection and Processing
* **No Backend:** NovaTV is a client-side application. It does not communicate with any server controlled by
  the NovaTV developer.
* **No Telemetry or Analytics:** NovaTV builds made from this repository do not configure any analytics or crash
  reporting service (no Firebase, no Crashlytics). We do not monitor how you use the app.
* **No Update Tracking:** The in-app update checker is disabled. Updates are installed manually by the user.
* **Local Settings:** Your settings (including the NovaTV options: theme, accent color, cards per row, preview,
  clock, animations) are stored only on your device.

## 2. Third-Party Services (YouTube/Google)
* **Authentication:** When you sign in, the application uses the official Google OAuth 2.0 flow, which lets you
  authorize the app without sharing your password with the developer.
* **Local Storage:** Authentication tokens are stored only on your device. They are never transmitted to, or
  stored by, the developer.
* **Data Flow:** Videos, channel information (including the channel avatars shown on cards and in the player) and
  account information are fetched directly from YouTube/Google servers to your device.
* **Channel Page Header:** To show a channel's banner, picture and subscriber count, the app requests the public
  channel page data from YouTube. This request is anonymous: it contains only the channel ID and the device
  language, no account token.

## 3. Community-Driven Services
To provide some features, the application communicates with the following community-driven services:
* **SponsorBlock:** The app sends the ID of the video being viewed to the SponsorBlock API to retrieve
  crowd-sourced skip segments.
* **DeArrow:** The app sends the ID of the video being viewed to the DeArrow API to retrieve crowd-sourced titles.
* **Return YouTube Dislike (RYD):** The app sends the video ID to the RYD API to retrieve estimated dislike counts.
* **Anonymous Requests:** These requests are read-only and do not include personal identifiers, Google account
  tokens or user profile data.

## 4. Automated Decision-Making and Profiling
In accordance with **GDPR Article 15**:
* **No Profiling:** We do not track user behavior to build profiles.
* **No Automated Decisions:** The app does not use algorithms or AI to make decisions about users or to change how
  content is ranked. Content is shown as returned by the YouTube API.

## 5. External Links
The application may contain static links (for example to project pages or donation pages).
* **Manual Action Only:** These links never open automatically. No data is transmitted until you choose to open
  a link or copy an address.
* **Third-Party Policies:** Once you leave the application for an external website or service, your data is
  governed by that service's privacy policy.

## 6. Your Rights (GDPR / CCPA)
NovaTV does not store or transmit personal data to the developer, so we hold no records (names, emails, IP
addresses) linked to your identity, and there is no data to provide or delete upon request.

## 7. Contact
For questions about this policy, please open an issue on the
[NovaTV GitHub repository](https://github.com/Dexmodzz/NovaTV/issues).
