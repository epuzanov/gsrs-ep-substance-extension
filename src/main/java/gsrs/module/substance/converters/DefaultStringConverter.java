package gsrs.module.substance.converters;

/**
 * Created by Egor Puzanov on 06/12/23.
 */

public class DefaultStringConverter extends AbstractStringConverter {
    public String toFormat(String fmt, String value) {
        return value;
    }
}
