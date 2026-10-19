import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Absichtlich kaputter Test, um zu zeigen, dass die CI rot wird. Wird nicht gemergt. */

public class CiDemoTest {
      @Test
    void schlaegtAbsichtlichFehl() {
        assertEquals(1, 2);
    }
}
