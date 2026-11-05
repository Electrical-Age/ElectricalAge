package mods.eln.robotics.computer.cpu;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The official riscv-tests (rv32ui/um/ua/uf/ud/uc/mi, "p" environment) on the CPU. Each image is loaded at RAM_BASE
 * and run until it writes tohost: 1 is a pass, (n << 1) | 1 a failure of its test case n. The images are built by
 * tools/rv32/build-riscv-tests.sh.
 */
class RiscvTestsTest {
    /** Tests this CPU doesn't run, with why. */
    static final java.util.Map<String, String> SKIP = java.util.Map.of(
            "rv32mi-p-breakpoint", "no debug triggers (tselect/tdata are optional)",
            "rv32mi-p-pmpaddr", "no PMP (optional; there is nothing below machine mode to protect)");

    @TestFactory
    List<DynamicTest> riscvTests() throws IOException {
        List<DynamicTest> tests = new ArrayList<>();
        try (InputStream in = RiscvTestsTest.class.getResourceAsStream("/riscv-tests/index.txt")) {
            assertNotNull(in, "riscv-tests images missing");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
            for (String line; (line = r.readLine()) != null; ) {
                String[] p = line.trim().split(" ");
                if (p.length != 2 || SKIP.containsKey(p[0])) continue;
                String name = p[0];
                int tohost = Integer.parseInt(p[1]);
                tests.add(DynamicTest.dynamicTest(name, () -> run(name, tohost)));
            }
        }
        return tests;
    }

    static void run(String name, int tohost) throws IOException {
        byte[] image;
        try (InputStream in = RiscvTestsTest.class.getResourceAsStream("/riscv-tests/" + name + ".bin")) {
            image = in.readAllBytes();
        }
        Memory mem = new Memory(null, 1 << 20);
        mem.loadRam(Memory.RAM_BASE, image);
        Cpu cpu = new Cpu(mem);
        cpu.reset(Memory.RAM_BASE);
        int host = Memory.RAM_BASE + tohost;
        for (int i = 0; i < 2000; i++) {
            cpu.run(1000);
            int v = mem.load32(host);
            if (v != 0) {
                assertEquals(1, v, name + " failed test case " + (v >>> 1) + " (pc " + Integer.toHexString(cpu.pc)
                        + ", mcause " + cpu.mcause + ")");
                return;
            }
        }
        fail(name + " never finished (pc " + Integer.toHexString(cpu.pc) + ")");
    }
}
