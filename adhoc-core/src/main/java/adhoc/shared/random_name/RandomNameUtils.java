/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package adhoc.shared.random_name;

import lombok.experimental.UtilityClass;
import net.datafaker.Faker;
import net.datafaker.internal.helper.WordUtils;

@UtilityClass
public class RandomNameUtils {

    public String randomName() {
        Faker faker = new Faker();

        return onlyAlpha(WordUtils.capitalize(faker.word().adjective()) +
                WordUtils.capitalize(faker.word().adjective()) +
                WordUtils.capitalize(faker.word().verb()));
    }

    private String onlyAlpha(String str) {
        return str.replaceAll("[^A-Za-z]", "");
    }

    public static void main() {
        for (int i = 0; i < 20; i++) {
            System.out.println(randomName());
        }
    }
}
