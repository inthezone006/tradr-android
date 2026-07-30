# 📈 tradr

💸 Welcome to **tradr**, a high-performance and modern Android application built with **Jetpack Compose** and **Firebase**. Master the art of trading in a risk-free environment designed with a sleek aesthetic!

## ✨ Features

### 🔐 Advanced Security & Authentication
- **Multi-Method Login**: Sign up with an **Email/Password** or use **Google One Tap Sign-in**.
- **Secure Onboarding**: Mandatory password setup for all users (including those with Google) and a strict 5-point password validation system.
- **Identity Protection**: Re-authentication required for sensitive actions like account deletion.

### 🎮 Gamified Experience
- **Difficulty Levels**: Choose your starting capital from **Level 1 ($100,000)** all the way to **Level 7 ($100)**.
- **Global Leaderboard**: Compete for the top spot! Both Global and Level specific rankings show the top traders globally.
- **Personal Recognition**: Your name gets a **Royal Crown (👑)** and highlight when you make the leaderboard.

### 💼 Portfolio & Trading
- **Live Market Data**: Powered by the **Finnhub API** for live NASDAQ quotes and statistics.
- **Fintech Dashboard**: Track your calculated **Total Account Value**, **Cash Balance**, and **Live Equity** in a professional UI.
- **Comprehensive Trade History**: View both your **Active** and **Old Positions** (full trading history) in one place.
- **Atomic Transactions**: High-integrity Buy and Sell operations using Firestore's "Read-Before-Write" patterns.

### 🔍 Market Intelligence
- **Unified Search Bar**: A powerful Material 3 search interface that takes over the full screen for an immersive experience.
- **Personal Watchlist**: Hit the heart (❤️) and save your favorite stocks to a persistent list, synced across all your devices.
- **Deep Insights**: View Open, Prev Close, Day High/Low, and Percent Change for any stock.
- **Gemini AI Analysis 🧠**: Get real-time, AI-powered stock summaries and outlooks using **Google Gemini**. Includes a custom Markdown formatter for clear, readable insights.
- **Economic Calendar**: Stay ahead of the market with a live feed of global economic events (Impact, Actual vs. Forecast).

### ⚙️ Account Management
- **Customizable Profiles**: Upload and change your profile picture via **Firebase Storage**.
- **Smart Notifications 🔔**: Granular Push Notification controls for Large Stock Drops, Low Balances, and New Sign-ins.
- **Immersive Settings**: A clean, dark-themed, and refreshable account management hub to edit your display name, password, or email verification status.

## 🛠️ Tech Stack

- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: Modern MVVM Pattern with Repository logic.
- **Networking**: [Retrofit](https://square.github.io/retrofit/) & [Gson](https://github.com/google/gson) for real-time API calls.
- **Backend**: [Firebase](https://firebase.google.com/) 🔥
  - **Authentication**: Real-time authentication and account linking.
  - **Firestore**: Scalable NoSQL database for portfolios, watchlists, and leaderboards.
  - **Storage**: Secure image hosting for user profiles.
- **Images**: [Coil](https://coil-kt.github.io/coil/) for fast, asynchronous image loading.
- **Refresh**: Material 3 **Pull-to-Refresh** integrated on all data-heavy screens.

## 🚀 Getting Started

### Prerequisites

- Android Studio Jellyfish or newer.
- A Firebase project (Free **Spark** Tier compatible).
- A [Finnhub.io](https://finnhub.io/) API Key.

### Setup

1. **Clone the repo**:
   ```bash
   git clone https://github.com/yourusername/tradrgit
   ```

2. **Firebase Configuration**:
   - Download your `google-services.json` from the console and place it in the `app/` folder.
   - Enable **Authentication** (Email & Google), **Firestore**, and **Storage**.
   - Set up a Firestore Composite Index for the `users` collection on fields `level` (Ascending) and `balance` (Descending) to enable leaderboard filtering.

3. **API Integration**:
   - **Finnhub**: Replace the API key in `MarketRepository.kt` with your own [Finnhub.io](https://finnhub.io/) key.
   - **Google Gemini**: Add your `GEMINI_API_KEY` to `gradle.properties` (Get it from [Google AI Studio](https://aistudio.google.com/)).
   - **Firebase**: Update `WEB_CLIENT_ID` in `Navigation.kt` with your Firebase Web Client ID.

4. **Build & Run**: 🏁 Launch the app and start building your empire!

## 🗒️ Logs

This app also logs various different user data.

- **Financial & Market Events**
  - `purchase`: Logged whenever a user buys a stock and includes the **Ticker Symbol** and the **Total USD Value** of the trade.
  - `add_to_wishlist`: Logged when a user hearts a stock. Tracks which companies are trending among users.
  - `select_difficulty_level`: Logged during onboarding. Tracks the Level (1-7) and the **Starting Balance** chosen.

- **User Behavior & Navigation**
  - `screen_view`: Logged every time a user switches tabs or opens a detail page. Includes the **Screen Name** (e.g., `portfolio_screen`, `leaderboard_screen`).
  - `login` & `sign_up`: Tracks how users join (Google vs. Email).
  - `logout` & `delete_account`: Tracks user churn and session length.
  - `first_open`: Logged the first time a user launches the app after downloading it.
  - `session_start`: Tracks when user re-opens app after 30 minutes.
  - `user_engagement`: Standard event that measures how long the app is in the foreground and is being used.
  - `app_exception`: Tracks major errors or crashes.
  - `app_remove`: Triggered when app is uninstalled on device.
  - `app_update`: Triggered when user updates app and then opens it.

- **Account Management**
  - `update_display_name`: Logs the new name chosen by the user.
  - `update_profile_picture`: Tracks engagement with profile customization.
  - `update_password`: Logs when a user strengthens their account security.
  - `send_email_verification`: Tracks users attempting to verify their accounts.
  - `update_notification_settings`: Tracks how users customize their push alerts.

**For High-Level Stats**: Go to **Analytics > Dashboard**.
**For Specific Actions**: Go to **Analytics > Events**. And **For Technical Logs**: Go to **Release & Monitor > Crashlytics** to click on any non-fatal log to see the **Logs** tab, which contains the full **API_LOG** (OkHttp Request/Response).

**IMPORTANT**: Add GEMINI_API_KEY to `local.properties` before building.

## 🔨 Updating

For each update to the app for production, make sure to update the `versionCode` and `versionName` in `build.gradle.kts`.

## 🤝 Contributing

This project is a labor of love for fintech and mobile development. Contributions and suggestions are always welcome!

## 📝 License

This project is licensed under the MIT License. 📄
