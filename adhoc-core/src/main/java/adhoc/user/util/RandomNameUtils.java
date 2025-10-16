package adhoc.user.util;

import lombok.experimental.UtilityClass;
import net.datafaker.Faker;
import net.datafaker.internal.helper.WordUtils;

@UtilityClass
public class RandomNameUtils {

    public String randomName() {
        Faker faker = new Faker();

        return WordUtils.capitalize(faker.word().adjective()) +
                WordUtils.capitalize(faker.word().adjective()) +
                WordUtils.capitalize(faker.word().noun());
    }

    public static void main() {
        for (int i = 0; i < 20; i++) {
            System.out.println(randomName());
        }
    }
}
