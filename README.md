# Walk Tracker – Aplicație Android

Aplicație Android dezvoltată în cadrul practicii la Textron, pentru monitorizarea traseelor de plimbare și alergare.

## Funcționalități
- Înregistrarea traseului în timp real pe Google Maps (GPS)
- Calculul distanței, duratei și ritmului (min/km)
- Autentificare cu email/parolă sau cont Google (Firebase Auth)
- Istoricul antrenamentelor salvat în Firebase Realtime Database
- Vremea locală prin API-ul OpenWeatherMap
- Mod multiplayer Leader / Follower: două telefoane își transmit locația prin UDP

## Tehnologii
Java, Android SDK, Google Maps API, Firebase (Auth, Realtime Database), Volley, Gson

## Rulare
1. Clonează proiectul și deschide-l în Android Studio.
2. Adaugă în `local.properties`:
   WEATHER_API_KEY=cheia_ta_openweather
   MAPS_API_KEY=cheia_ta_google_maps
3. Rulează aplicația pe un telefon sau emulator.
