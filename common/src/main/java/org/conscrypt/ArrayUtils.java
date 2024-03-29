/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.conscrypt;

/**
 * Compatibility utility for Arrays.
 */
final class ArrayUtils {
    private ArrayUtils() {}

    /**
     * Checks that the range described by {@code offset} and {@code count}
     * doesn't exceed {@code arrayLength}.
     */
    static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw new ArrayIndexOutOfBoundsException("length=" + arrayLength + "; regionStart="
                    + offset + "; regionLength=" + count);
        }
    }

    static String[] concatValues(String[] a1, String... values) {
        return concat (a1, values);
    }

    static String[] concat(String[] a1, String[] a2) {
        String[] result = new String[a1.length + a2.length];
        int offset = 0;
        for (int i = 0; i < a1.length; i++, offset++) {
            result[offset] = a1[i];
        }
        for (int i = 0; i < a2.length; i++, offset++) {
            result[offset] = a2[i];
        }
        return result;
    }
}
