/* Qilin - a Java Pointer Analysis Framework
 * Copyright (C) 2021-2030 Qilin developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3.0 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <https://www.gnu.org/licenses/lgpl-3.0.en.html>.
 */

package qilin.test.context;

import driver.Main;
import org.junit.Test;
import qilin.test.util.JunitTests;

public class HybTests extends JunitTests {
    @Test
    public void testHyb0() {
        String[] args = generateArguments("qilin.microben.context.hyb.Hyb0", "1h");
        checkAssertions(Main.run(args));
    }

    @Test
    public void testHyb1() {
        String[] args = generateArguments("qilin.microben.context.hyb.Hyb1", "1h");
        checkAssertions(Main.run(args));
    }

    @Test
    public void testHyb2() {
        String[] args = generateArguments("qilin.microben.context.hyb.Hyb2", "2h");
        checkAssertions(Main.run(args));
    }
}