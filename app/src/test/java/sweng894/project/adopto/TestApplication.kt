package sweng894.project.adopto

import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

class TestApplication : App(), TestLifecycleApplication {

    override fun beforeTest(method: Method?) {
        // No-op, can be used for pre-test setup
    }

    override fun prepareTest(test: Any?) {
        // No-op, can be used for test-specific setup
    }

    override fun afterTest(method: Method?) {
        // Reset state after the test
        _instance = null
    }

    companion object {
        fun initialize() {
            _instance = TestApplication()
        }
    }
}
