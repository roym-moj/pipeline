package demo.config

import org.junit.Test

class ConfigTest {

    @Test
    public void getPropertyValue_gets_requested_value() {
        def result = Config.getPropertyValue("testProperty", this)
        assert result == "thevalue"
    }

}
