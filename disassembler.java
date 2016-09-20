import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.math.BigInteger;

class Procedure {
  private static final String[] registers = {
    "$zero", "$at", "$v0", "$v1", "$a0", "$a1", "$a2", "$a3", "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7", "$t8", "$t9", "$k0", "$k1", "$gp", "$sp", "$fp", "$ra"
  };

  public static String getRegister(int reg) {
    return registers[reg];
  }
}

class RFormat extends Procedure {
  private String rs;
  private String rt;
  private String rd;
  private String funct;
  private String address;

  private static final Map<Integer, String> functCodes;

  static {
    Map<Integer, String> tempMap = new HashMap<Integer, String>();
    tempMap.put(0x20, "add");
    tempMap.put(0x22, "sub");
    tempMap.put(0x24, "and");
    tempMap.put(0x25, "or");
    tempMap.put(0x2A, "slt");
    functCodes = Collections.unmodifiableMap(tempMap);
  }

  public RFormat(int addr) {
    rs = setRs(addr);
    rt = setRt(addr);
    rd = setRd(addr);
    address = Integer.toHexString(addr);
    funct = setFunct(addr);
  }

  private static String setRs(int address) {
    int reg = (address & 0x3E00000) >>> 21;
    return getRegister(reg);
  }

  private static String setRt(int address) {
    int reg = (address & 0x1F0000) >>> 16;
    return getRegister(reg);
  }

  private static String setRd(int address) {
    int reg = (address & 0xF800) >>> 11;
    return getRegister(reg);
  }

  private static String setFunct(int address) {
    int functCode = (address & 0x3F);
    return functCodes.get(functCode);
  }

  public String getAddress() {
    return address;
  }

  public String toString() {
    return new String(funct + ", " + rd + ", " + rs + ", " + rt);
  }

  public void print() {
    System.out.print(getAddress() + ": " + toString());
  }
}

class IFormat extends Procedure {
  public int opcode;
  public int rs;
  public int rd;
  public short offset;

  private static final Map<Integer, String> opCodes;
  static {
    Map<Integer, String> tempMap = new HashMap<Integer, String>();
    tempMap.put(0x23, "lw");
    tempMap.put(0x2B, "sw");
    tempMap.put(0x04, "beq");
    tempMap.put(0x05, "bne");
    opCodes = Collections.unmodifiableMap(tempMap);
  }

  static int getOpcode(int address) {
    return address & 0xFC000000;
  }

}

class Disassembler {
  public static final int[] instructions = { 0x022DA822, 0x8EF30018, 0x12A70004, 0x02689820, 0xAD930018, 0x02697824, 0xAD8FFFF4,
0x018C6020, 0x02A4A825, 0x158FFFF6, 0x8E59FFF0 };
  public static final Map<Integer, String> tests;
  static {
    Map<Integer, String> tempMap = new HashMap<Integer, String>();
    tempMap.put(0x01398820, "add, $s1, $t1, $t9");
    tempMap.put(0x01398824, "and, $s1, $t1, $t9");
    tempMap.put(0x01398822, "sub, $s1, $t1, $t9");
    tempMap.put(0x01398825, "or, $s1, $t1, $t9");
    tempMap.put(0x0139882a, "slt, $s1, $t1, $t9");
    tests = Collections.unmodifiableMap(tempMap);
  }

  static Boolean isRFormat(int address) {
    return (address & 0xFC000000) == 0;
  }

  static void test() {
    Iterator it = tests.entrySet().iterator();
    while(it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      int key = Integer.valueOf((int) entry.getKey());
      if (isRFormat(key)) {
        RFormat procedure = new RFormat(key);
        if (procedure.toString().equals(entry.getValue())) {
          System.out.println("PASSED: " + entry.getKey() + " " + entry.getValue());
        } else {
          System.out.println("FAILED: " + entry.getKey() + " " + entry.getValue());
        }
      }
    }
  }

  static void parseAddresses() {
    for (int i = 0; i < instructions.length - 1; i++) {
      int address = instructions[i];
      if (isRFormat(address)) {
        RFormat procedure = new RFormat(address);
        procedure.toString();
      }
    }
  }

  public static void main(String[] args) {
    // parseAddresses();
    test();
  }
}
