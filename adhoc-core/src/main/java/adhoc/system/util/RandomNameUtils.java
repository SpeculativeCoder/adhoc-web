package adhoc.system.util;

import lombok.experimental.UtilityClass;
import net.datafaker.Faker;
import net.datafaker.internal.helper.WordUtils;

@UtilityClass
public class RandomNameUtils {

    public String randomName() {
        Faker faker = new Faker();

        return WordUtils.capitalize(faker.word().adjective()) +
                WordUtils.capitalize(faker.word().noun()) +
                WordUtils.capitalize(faker.word().verb());
    }

    public static void main() {
        System.out.println(randomName());
    }
}
