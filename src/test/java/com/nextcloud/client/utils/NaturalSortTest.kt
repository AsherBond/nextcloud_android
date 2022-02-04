/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.nextcloud.client.utils

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import third_parties.daveKoeller.AlphanumComparator

/**
 * Adapted from https://github.com/nextcloud/server/blob/caff1023ea72bb2ea94130e18a2a6e2ccf819e5f/tests/lib/NaturalSortTest.php
 */
@RunWith(Parameterized::class)
class NaturalSortTest {

    @Parameterized.Parameter(0)
    lateinit var title: String

    @Parameterized.Parameter(1)
    lateinit var input: Array<String>

    @Parameterized.Parameter(2)
    lateinit var expected: Array<String>

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data(): Iterable<Array<Any>> = listOf(
            arrayOf(
                "Different casing",
                arrayOf("aaa", "bbb", "BBB", "AAA"),
                arrayOf("aaa", "AAA", "bbb", "BBB")
            ),
            arrayOf(
                "Numbers",
                arrayOf(
                    "124.txt", "abc1", "123.txt", "abc", "abc2", "def (2).txt", "ghi 10.txt", "abc12", "def.txt",
                    "def (1).txt", "ghi 2.txt", "def (10).txt", "abc10", "def (12).txt", "z", "ghi.txt", "za",
                    "ghi 1.txt", "ghi 12.txt", "zz", "15.txt", "15b.txt"
                ),
                arrayOf(
                    "15.txt", "15b.txt", "123.txt", "124.txt", "abc", "abc1", "abc2", "abc10", "abc12", "def.txt",
                    "def (1).txt", "def (2).txt", "def (10).txt", "def (12).txt", "ghi.txt", "ghi 1.txt", "ghi 2.txt",
                    "ghi 10.txt", "ghi 12.txt", "z", "za", "zz"
                )
            ),
            arrayOf(
                "Chinese characters",
                arrayOf(
                    "十.txt", "一.txt", "二.txt", "十 2.txt", "三.txt", "四.txt", "abc.txt", "五.txt", "七.txt", "八.txt",
                    "九.txt", "六.txt", "十一.txt", "波.txt", "破.txt", "莫.txt", "啊.txt", "123.txt"
                ),
                arrayOf(
                    "123.txt", "abc.txt", "一.txt", "七.txt", "三.txt", "九.txt", "二.txt", "五.txt", "八.txt", "六.txt",
                    "十.txt", "十 2.txt", "十一.txt", "啊.txt", "四.txt", "波.txt", "破.txt", "莫.txt"
                )
            ),
            arrayOf(
                "With umlauts",
                arrayOf(
                    "öh.txt", "Äh.txt", "oh.txt", "Üh 2.txt", "Üh.txt", "ah.txt", "Öh.txt", "uh.txt", "üh.txt",
                    "äh.txt",
                ),
                arrayOf(
                    "ah.txt", "äh.txt", "Äh.txt", "oh.txt", "öh.txt", "Öh.txt", "uh.txt", "üh.txt", "Üh.txt",
                    "Üh 2.txt",
                )
            )
        )
    }

    @Test
    fun test() {
        val sut = AlphanumComparator<String>()
        val sorted = input.sortedWith(sut).toTypedArray()
        Assert.assertArrayEquals("Wrong sort", expected, sorted)
    }
}
