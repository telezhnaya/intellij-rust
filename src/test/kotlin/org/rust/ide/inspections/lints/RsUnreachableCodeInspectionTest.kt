/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints

import org.rust.ProjectDescriptor
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.ide.inspections.RsInspectionsTestBase

class RsUnreachableCodeInspectionTest : RsInspectionsTestBase(RsUnreachableCodeInspection::class) {
    fun `test straightline with return`() = checkByText("""
        fn main() {
            let x = 1;
            return;
            <warning descr="Unreachable statement">let y = 2;</warning>
        }
    """)

    fun `test if else after return`() = checkByText("""
        fn main() {
            let x = 1;
            return;
            <warning descr="Unreachable statement">let y = 2;</warning>
            <warning descr="Unreachable statement">if foo {
                1;
            }</warning>
            <warning descr="Unreachable statement">2;</warning>
            <warning descr="Unreachable expression">if bar {
                2;
            }</warning>
        }
    """)

    fun `test if else returns in both branches`() = checkByText("""
        fn main() {
            if a {
                1;
                return;
                <warning descr="Unreachable statement">2;</warning>
            } else {
                3;
                return;
                <warning descr="Unreachable statement">4;</warning>
            }
            <warning descr="Unreachable statement">5;</warning>
        }
    """)

    fun `test code after loop`() = checkByText("""
        fn main() {
            loop {
                1;
            }
            <warning descr="Unreachable statement">2;</warning>
        }
    """)

    fun `test loop with break`() = checkByText("""
        fn main() {
            loop {
                1;
                if flag {
                    break;
                }
                2;
            }
            3;
        }
    """)

    fun `test loop with match`() = checkByText("""
        fn main() {
            loop {
                1;
                match x {
                    Foo => {
                        2;
                        break;
                    }
                    Bar => {
                        3;
                    }
                }
                4;
            }
            5;
        }
    """)

    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test unreachable in closure call`() = checkByText("""
        fn foo(f: fn(i32)) {}
        fn main() {
            foo(|x| {
                if x > 0 {
                    panic!();
                    <warning descr="Unreachable statement">1;</warning>
                }
            });
            2;
        }
    """)

    fun `test closure call with loop`() = checkByText("""
        fn foo(f: fn(i32)) {}
        fn main() {
            foo(|x| {
                1;
                loop {
                    2;
                }
                <warning descr="Unreachable statement">3;</warning>
            });
            4;
        }
    """)

    fun `test only return`() = checkByText("""
        fn main() {
            return
        }
    """)
}
