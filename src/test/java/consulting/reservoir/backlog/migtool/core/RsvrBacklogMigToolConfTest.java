package consulting.reservoir.backlog.migtool.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RsvrBacklogMigToolConfTest {

    @Test
    void test() {
        RsvrBacklogMigToolConf conf = new RsvrBacklogMigToolConf();
        conf.setApiInterval(123);
        assertEquals(123, conf.getApiInterval());

        conf.setDirDb("c:/ABC");
        assertEquals("c:/ABC/backlogDb", conf.getDirDbPath());
    }

}
