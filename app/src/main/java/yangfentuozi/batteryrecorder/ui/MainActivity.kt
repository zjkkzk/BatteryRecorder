package yangfentuozi.batteryrecorder.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import kotlinx.coroutines.flow.MutableSharedFlow
import yangfentuozi.batteryrecorder.shared.util.LoggerX
import yangfentuozi.batteryrecorder.ui.theme.BatteryRecorderTheme

class MainActivity : BaseActivity() {
    private val openCurrentRecordDetailEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BatteryRecorderTheme {
                BatteryRecorderApp(openCurrentRecordDetailEvent = openCurrentRecordDetailEvent)
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("open_current_record_detail", false)) {
            openCurrentRecordDetailEvent.tryEmit(Unit)
            LoggerX.d("MainActivity", "handleIntent: open_current_record_detail == true")
        }
    }
}
