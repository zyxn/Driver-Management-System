# Fix: Token Persistence - Auto Login

## 🐛 Problem
User harus login ulang setiap kali app di-close/swipe dari recent apps.

## 🔍 Root Cause
`MainActivity.kt` tidak mengecek token yang tersimpan di DataStore saat app dibuka. Navigation logic hanya menggunakan state `isLoggedIn` yang di-reset setiap kali app restart.

## ✅ Solution

### 1. Check Token on App Start
Tambahkan `LaunchedEffect(Unit)` di `AppNavigation` untuk mengecek token saat app dibuka:

```kotlin
LaunchedEffect(Unit) {
    val token = preferencesManager.token.first()
    val savedUsername = preferencesManager.fullName.first() 
        ?: preferencesManager.username.first()
    
    if (!token.isNullOrEmpty()) {
        // User already logged in
        isLoggedIn = true
        username = savedUsername ?: ""
    } else {
        // No token, show login
        isLoggedIn = false
    }
}
```

### 2. Three-State Logic
Gunakan nullable Boolean untuk handle loading state:
- `null` = Checking token (show loading)
- `true` = Logged in (show HomeScreen)
- `false` = Not logged in (show LoginScreen)

```kotlin
var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }

when (isLoggedIn) {
    true -> HomeScreen(...)
    false -> LoginScreen(...)
    null -> LoadingScreen()
}
```

### 3. Clear Token on Logout
Pastikan token dihapus saat logout:

```kotlin
onLogout = {
    kotlinx.coroutines.MainScope().launch {
        preferencesManager.clearAll()
    }
    isLoggedIn = false
    username = ""
    viewModel.resetState()
}
```

## 📊 Flow Diagram

### Before (Bug)
```
App Start
    ↓
isLoggedIn = false (hardcoded)
    ↓
Show LoginScreen
    ↓
User must login again ❌
```

### After (Fixed)
```
App Start
    ↓
isLoggedIn = null (checking)
    ↓
Show Loading
    ↓
Check DataStore for token
    ↓
    ├─ Token exists?
    │   ├─ YES → isLoggedIn = true → Show HomeScreen ✅
    │   └─ NO  → isLoggedIn = false → Show LoginScreen
    ↓
User stays logged in! ✅
```

## 🔐 Token Storage

Token disimpan di **DataStore** (encrypted by Android):
- Location: `/data/data/com.example.driver_management_system/files/datastore/user_preferences.preferences_pb`
- Persistent: ✅ Survive app restart
- Secure: ✅ Encrypted by Android OS
- Cleared on: App uninstall or manual logout

## 🧪 Testing

### Test 1: Login Persistence
1. Login ke app
2. Close app (swipe dari recent apps)
3. Open app lagi
4. **Expected**: Langsung masuk ke HomeScreen (tidak perlu login)

### Test 2: Logout
1. Login ke app
2. Tap logout
3. **Expected**: Kembali ke LoginScreen
4. Close dan open app
5. **Expected**: Tetap di LoginScreen (token sudah dihapus)

### Test 3: Fresh Install
1. Install app pertama kali
2. Open app
3. **Expected**: Show LoginScreen (no token)

## 🔧 Code Changes

### File: `MainActivity.kt`

#### Added Imports
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import com.example.driver_management_system.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
```

#### Updated `onCreate()`
```kotlin
val preferencesManager = PreferencesManager(applicationContext)

AppNavigation(
    viewModel = viewModel,
    preferencesManager = preferencesManager, // ← Added
    onLoginSuccess = { requestLocationPermissionAndStartTracking() }
)
```

#### Updated `AppNavigation()`
```kotlin
@Composable
fun AppNavigation(
    viewModel: LoginViewModel,
    preferencesManager: PreferencesManager, // ← Added parameter
    onLoginSuccess: () -> Unit
) {
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) } // ← Nullable
    
    // ← Added: Check token on start
    LaunchedEffect(Unit) {
        val token = preferencesManager.token.first()
        val savedUsername = preferencesManager.fullName.first() 
            ?: preferencesManager.username.first()
        
        if (!token.isNullOrEmpty()) {
            isLoggedIn = true
            username = savedUsername ?: ""
        } else {
            isLoggedIn = false
        }
    }
    
    // ← Changed: Three-state logic
    when (isLoggedIn) {
        true -> HomeScreen(...)
        false -> LoginScreen(...)
        null -> LoadingScreen()
    }
}
```

## 🎯 Benefits

✅ **Better UX**: User tidak perlu login berulang kali
✅ **Secure**: Token tetap encrypted di DataStore
✅ **Standard Practice**: Sesuai dengan best practice mobile app
✅ **Smooth**: Loading state saat check token

## 📝 Notes

### DataStore vs SharedPreferences
- **DataStore**: Async, type-safe, modern (digunakan untuk token)
- **SharedPreferences**: Sync, legacy (digunakan untuk userId di Service)

Kita menggunakan keduanya karena:
- DataStore untuk UI layer (async-friendly)
- SharedPreferences untuk Service layer (butuh sync access)

### Token Expiration
Backend menggunakan JWT token. Jika token expired, backend akan return 401 Unauthorized. App sudah handle ini dengan:
1. ✅ Catch 401 error di RetrofitClient interceptor
2. ✅ Clear token dari DataStore
3. ✅ Emit AuthEvent.TokenExpired via AuthEventBus
4. ✅ MainActivity listen event dan redirect ke LoginScreen

**Implementation**: 
- `RetrofitClient.kt` - Interceptor untuk detect 401
- `AuthEventBus.kt` - Event bus untuk broadcast token expiration
- `MainActivity.kt` - Listen event dan handle logout

## 🚀 Next Steps

1. ✅ Token persistence (DONE)
2. ✅ Token expiration handling (DONE)
3. ⏳ Refresh token mechanism
4. ⏳ Biometric authentication
5. ⏳ Remember me checkbox

## 🐛 Troubleshooting

### Token masih hilang setelah fix
1. Check DataStore file exists:
   ```bash
   adb shell ls /data/data/com.example.driver_management_system/files/datastore/
   ```

2. Check token tersimpan:
   ```kotlin
   lifecycleScope.launch {
       val token = preferencesManager.token.first()
       Log.d("Token", "Saved token: $token")
   }
   ```

3. Clear app data dan test ulang:
   ```bash
   adb shell pm clear com.example.driver_management_system
   ```

### App stuck di loading
- Check LaunchedEffect tidak error
- Check DataStore accessible
- Add timeout fallback

## ✨ Summary

Masalah token tidak persisten sudah **FIXED**! User sekarang bisa:
- Login sekali
- Close app
- Open app lagi → Langsung masuk (auto login)
- Token tersimpan aman di DataStore
