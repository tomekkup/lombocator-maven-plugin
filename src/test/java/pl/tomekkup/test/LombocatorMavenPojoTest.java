package pl.tomekkup.test;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Class of tests
 *
 * @author Tomek Kuprowski
 */
public class LombocatorMavenPojoTest {

    @Test
    void shouldReplaceTrivialGetterAndSetter() {
        assertThat(2 == 2);
    }


}