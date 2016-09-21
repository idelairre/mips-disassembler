import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.math.BigInteger;

abstract class Procedure {
  protected static int address = 0x7A05C;

  public Procedure() {
    address = address + 4;
  }

  public static String getRegister(int reg) {
    return new String('$' + Integer.toString(reg));
  }

  public int getAddress() {
    return address;
  }

  abstract public void print();

  abstract public String toString();

}

class RFormat extends Procedure {
  private String rs;
  private String rt;
  private String rd;
  private String funct;

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

  public RFormat(int hex) {
    super();
    rs = setRs(hex);
    rt = setRt(hex);
    rd = setRd(hex);
    funct = setFunct(hex);
  }

  private static String setRs(int hex) {
    int reg = (hex & 0x3E00000) >>> 21;
    return getRegister(reg);
  }

  private static String setRt(int hex) {
    int reg = (hex & 0x1F0000) >>> 16;
    return getRegister(reg);
  }

  private static String setRd(int hex) {
    int reg = (hex & 0xF800) >>> 11;
    return getRegister(reg);
  }

  private static String setFunct(int hex) {
    int functCode = (hex & 0x3F);
    return functCodes.get(functCode);
  }

  public String toString() {
    String currentAddress = Integer.toHexString(address);
    String procedure = funct + " " + rd + ", " + rs + ", " + rt;
    return currentAddress + " " + procedure;
  }

  public void print() {
    System.out.println(toString());
  }
}

class IFormat extends Procedure {
  // I-Format procedures are called [opcode], [rt], [rs], [offset], they are REPRESENTED as [opcode], [rs], [rt], [offset]
  private String opcode;
  private String rs;
  private String rd;
  private String offset;
  private String branch;

  private static final Map<Integer, String> opCodes;

  static {
    Map<Integer, String> tempMap = new HashMap<Integer, String>();
    tempMap.put(0x23, "lw");
    tempMap.put(0x2B, "sw");
    tempMap.put(0x04, "beq");
    tempMap.put(0x05, "bne");
    opCodes = Collections.unmodifiableMap(tempMap);
  }

  public IFormat(int hex) {
    super();
    opcode = setOpcode(hex);
    rs = setRs(hex);
    rd = setRd(hex);
    offset = setOffset(hex);

  }

  private static String setRd(int hex) {
    int reg = (hex & 0x1F0000) >>> 16;
    return getRegister(reg);
  }

  private static String setRs(int hex) {
    int reg = (hex & 0x3E00000) >>> 21;
    return getRegister(reg);
  }

  public int getBranch(short offset) {
    return address + offset;
  }

  private String setOffset(int hex) {
    short offset = (short) (hex & 0xFFFF);
    if (opcode.equals("beq") || opcode.equals("bne")) {
      int branch = getBranch(offset);
      return new String(Integer.toHexString(branch));
    }
    return String.valueOf(offset);
  }

  static String setOpcode(int hex) {
    int opcode = (hex & 0xFC000000) >>> 26;
    return opCodes.get(opcode);
  }

  public String toString() {
    String procedure;
    String currentAddress = Integer.toHexString(address);
    if (opcode.equals("beq") || opcode.equals("bne")) {
      procedure = opcode + " " + rs + ", " + rd + ", address " + offset;
    } else {
      procedure = opcode + " " + rd + ", " + offset + '(' + rs + ')';
    }
    return currentAddress + " " + procedure;
  }

  public void print() {
    System.out.println(toString());
  }

}

class Disassembler {
  public static final int[] instructions = { 0x022DA822, 0x8EF30018, 0x12A70004, 0x02689820, 0xAD930018, 0x02697824, 0xAD8FFFF4,
0x018C6020, 0x02A4A825, 0x158FFFF6, 0x8E59FFF0 };

  public static final Map<Integer, String> tests;

  static {
    Map<Integer, String> tempMap = new LinkedHashMap<Integer, String>(); // need a linked hash map so these iterate in order
    // R-Format
    tempMap.put(0x01398820, "7a060 add $17, $9, $25");
    tempMap.put(0x01398824, "7a064 and $17, $9, $25");
    tempMap.put(0x01398822, "7a068 sub $17, $9, $25");
    tempMap.put(0x01398825, "7a06c or $17, $9, $25");
    tempMap.put(0x0139882a, "7a070 slt $17, $9, $25");
    // I-Format
    tempMap.put(0x8d3104d2, "7a074 lw $17, 1234($9)");
    tempMap.put(0x8d31fb2e, "7a078 lw $17, -1234($9)");
    tempMap.put(0xad3104d2, "7a07c sw $17, 1234($9)");
    tempMap.put(0xad31fb2e, "7a080 sw $17, -1234($9)");
    tempMap.put(0x1229ffff, "7a084 beq $17, $9, address 7a083"); // -1
    tempMap.put(0x1229000e, "7a088 beq $17, $9, address 7a096"); // 14
    tempMap.put(0x1629fff1, "7a08c bne $17, $9, address 7a07d"); // -15
    tempMap.put(0x16290000, "7a090 bne $17, $9, address 7a090"); // 0
    tempMap.put(0xAE77FFFC, "7a094 sw $23, -4($19)");
    tests = Collections.unmodifiableMap(tempMap);
  }

  static Boolean isRFormat(int address) {
    return (address & 0xFC000000) == 0;
  }

  static Boolean assertEqual(String test, String target) {
    Boolean passed = test.equals(target);
    if (passed) {
      System.out.println("PASSED");
    } else {
      System.out.println("FAILED: expected '" + test + "' to equal '" + target + "'");
    }
    return passed;
  }

  static void test() {
    Iterator it = tests.entrySet().iterator();
    while(it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      int key = Integer.valueOf((int) entry.getKey());
      Procedure procedure;
      if (isRFormat(key)) {
        procedure = new RFormat(key);
      } else {
        procedure = new IFormat(key);
      }
      assertEqual(procedure.toString(), (String) entry.getValue());
    }
  }

  static void parseAddresses() {
    for (int i = 0; i < instructions.length - 1; i++) {
      int address = instructions[i];
      if (isRFormat(address)) {
        RFormat procedure = new RFormat(address);
        procedure.print();
      } else {
        IFormat procedure = new IFormat(address);
        procedure.print();
      }
    }
  }

  public static void main(String[] args) {
    // parseAddresses();
    test();
  }
}
