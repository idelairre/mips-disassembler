import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;

abstract class Procedure {
  protected static int address = 0x7A05C;
  protected String rs;
  protected String rt;

  public Procedure() {
    address += 4;
  }

  protected static String getRegister(int reg) {
    return '$' + Integer.toString(reg);
  }

  protected int getAddress() {
    return address;
  }

  protected static String setRt(int hex) {
    int reg = (hex & 0x1F0000) >>> 16;
    return getRegister(reg);
  }

  protected static String setRs(int hex) {
    int reg = (hex & 0x3E00000) >>> 21;
    return getRegister(reg);
  }

  abstract public void print();

  abstract public String toString();

}

class RFormat extends Procedure {
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
    rt = setRt(hex);
    offset = setOffset(hex);
  }

  private int getBranch(short offset) {
    return (address + 4) + (offset << 2); // sign extension evidently occurs automatically in java
  }

  private String setOffset(int hex) {
    short offset = (short) (hex & 0xFFFF);
    if (opcode.equals("beq") || opcode.equals("bne")) {
      int branch = getBranch(offset);
      return Integer.toHexString(branch);
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
      procedure = opcode + " " + rs + ", " + rt + ", address " + offset;
    } else {
      procedure = opcode + " " + rt + ", " + offset + '(' + rs + ')';
    }
    return currentAddress + " " + procedure;
  }

  public void print() {
    System.out.println(toString());
  }

}

class Disassembler {
  public static final Map<Integer, String> instructions;  // need a linked hash map so these iterate in order

  static {
    Map<Integer, String> tempMap = new LinkedHashMap<Integer, String>(); // maybe there is a more idiomatic way of populating this?
    // R-Format
    tempMap.put(0x022DA822, "7a060 sub $21, $17, $13");
    tempMap.put(0x8EF30018, "7a064 lw $19, 24($23)");
    tempMap.put(0x12A70004, "7a068 beq $21, $7, address 7a078");
    tempMap.put(0x02689820, "7a06c add $19, $19, $8");
    tempMap.put(0xAD930018, "7a070 sw $19, 24($12)");
    tempMap.put(0x02697824, "7a074 and $15, $19, $9");
    tempMap.put(0xAD8FFFF4, "7a078 sw $15, -12($12)");
    tempMap.put(0x018C6020, "7a07c add $12, $12, $12");
    tempMap.put(0x02A4A825, "7a080 or $21, $21, $4");
    tempMap.put(0x158FFFF6, "7a084 bne $12, $15, address 7a05c");
    tempMap.put(0x8E59FFF0, "7a088 lw $25, -16($18)");
    instructions = Collections.unmodifiableMap(tempMap);
  }

  public static final Map<Integer, String> tests;

  static {
    Map<Integer, String> tempMap = new LinkedHashMap<Integer, String>();
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
    tempMap.put(0x1229ffff, "7a084 beq $17, $9, address 7a084"); // -1
    tempMap.put(0x1229000e, "7a088 beq $17, $9, address 7a0c4"); // 14
    tempMap.put(0x1629fff1, "7a08c bne $17, $9, address 7a054"); // -15
    tempMap.put(0x16290000, "7a090 bne $17, $9, address 7a094"); // 0
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

  static void parseAddresses(Map<Integer, String> tests, Boolean test) {
    for (Map.Entry<Integer, String> entry : tests.entrySet()) {
      int key = entry.getKey();
      Procedure procedure;
      if (isRFormat(key)) {
        procedure = new RFormat(key);
      } else {
        procedure = new IFormat(key);
      }
      if (test) {
        assertEqual(procedure.toString(), entry.getValue());
      } else {
        System.out.println(procedure.toString());
      }
    }
  }

  public static void main(String[] args) {
    parseAddresses(instructions, false);
  }
}
