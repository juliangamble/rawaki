package org.rawaki;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RawakiServerTest {

    @Test
    void mainDoesNotThrow() {
        assertDoesNotThrow(() -> RawakiServer.main(new String[]{}));
    }
}
