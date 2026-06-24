import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.dormrepair.app',
  appName: '宿舍报修',
  webDir: 'dist',
  server: {
    // In production APK, the app loads from local files (file://)
    // API calls go directly to the backend server
    androidScheme: 'http'
  },
  plugins: {
    StatusBar: {
      style: 'LIGHT',
      backgroundColor: '#F3F0EE'
    },
    Keyboard: {
      resize: 'body',
      resizeOnFullScreen: true
    }
  }
}

export default config
