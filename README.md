# Abhaya - SOS Emergency App

Abhaya is an Android-based **SOS emergency application** designed for **Smart India Hackathon 2024** under the problem statement **Women's Safety Analytics**. The goal of this project is to **empower women and make them feel safer in society** by providing a quick and efficient emergency alert system. Built using **Java, XML, and Firebase**, the app ensures real-time communication and tracking for enhanced safety.

## 📌 Features

- **Emergency Contacts Management** - Users can add, edit, and delete emergency contacts within the app.
- **SOS Alert System** - Two types of SOS alerts available:
  - **Regular SOS Alert** - Sent manually by the victim, containing the user's name, phone number, gender, and live location details.
  - **Anonymous SOS Alert** - Does not include the user's name, phone number, or gender, only sending location details.
- **Live Location Tracking** - Continuously updates the user's real-time location in **Firestore**, even when the app is running in the background.
- **HTTP POST SOS Data** - Sends an HTTP request with emergency data to a predefined endpoint for external handling.
- **Notifications** - Uses **Firebase Cloud Messaging (FCM)** to send real-time alerts.
- **Firebase Authentication** - Secure user login and registration for personalized safety settings.

## 🛠 Tech Stack
- **Java** & **XML** - Basic tech stack for the UI and functionalities just like traditional Android applications
- **Firebase Realtime Database** - Stores emergency contacts of each user and user details
- **Firestore** - Handles real-time location updates
- **Firebase Cloud Messaging (FCM)** - For notifications
- **OkHttp** - For HTTP requests to send SOS data
- **Navigation Drawer** - User-friendly navigation

## 🚀 Setup & Installation
1. Clone the repository:
   ```sh
   git clone https://github.com/yourusername/abhaya.git
   cd abhaya
   ```
2. Open the project in **Android Studio**.
3. Connect your Firebase project and update `google-services.json`.
4. Run the app on an emulator or physical device.

## 🔮 Future Enhancements
- **SMS & WhatsApp Integration** for faster SOS alerts.
- **WearOS Companion App** for quick access.
- **AI-based Incident Detection** using sensor data.
- **Voice Command Trigger** for hands-free emergency activation.

---
Made with ❤️ by your friendly neighbourhood crazy coder

